package ypfunding.server.persistence.dao;

import ypfunding.common.dto.RewardDTO;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLDelete;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLInsert;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLUpdate;
import ypfunding.server.persistence.dao.dynamicsql.col.*;
import ypfunding.server.persistence.dao.dynamicsql.condition.BasicCondition;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.token.ColumnValueToken;
import ypfunding.server.persistence.dao.dynamicsql.token.UserValueToken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RewardsDAO extends DAO {
    public enum Columns implements ColumnsEnum {
        REWARD_ID("reward_id"),
        PROJECT_ID("project_id"),
        NAME("name"),
        PRICE("price"),
        DESCRIPTION("description"),
        STOCK("stock");

        private final String name;

        Columns(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // =================================================================
    // 1. CREATE SECTION
    // =================================================================

    /**
     * 리워드 등룍 기능
     * @param conn 커넥션
     * @param rewardDTO 리워드DTO
     * @throws SQLException DB 오류 발생 시
     */
    public void insert(Connection conn, RewardDTO rewardDTO) throws SQLException {
        SQLInsert sqlInsert = new SQLInsert(Tables.REWARDS)
                .addTarget(Columns.PROJECT_ID, rewardDTO.getProjectId())
                .addTarget(Columns.NAME, rewardDTO.getName())
                .addTarget(Columns.PRICE, rewardDTO.getPrice())
                .addTarget(Columns.DESCRIPTION, rewardDTO.getDescription());

        // SQLLogger가 null 파라미터에서 NPE를 던지므로 stock이 null(무제한)이면 addTarget 생략 -> DB에 NULL로 저장됨
        if (rewardDTO.getStock() != null) {
            sqlInsert.addTarget(Columns.STOCK, rewardDTO.getStock());
        }

        sqlInsert.execute(conn);

        //DB내에서는 rewardId를 AI로 갱신하지만 자바 메모리엔 갱신이 안되어 직접 갱신
        try (PreparedStatement ps = conn.prepareStatement("SELECT LAST_INSERT_ID()");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                rewardDTO.setRewardId(rs.getLong(1));
            }
        }
    }

    // =================================================================
    // 2. READ SECTION
    // =================================================================

    /**
     * 리워드 ID 기반 조회
     * @param conn 커넥션
     * @param rewardId 리워드 id
     * @return 리워드 DTO
     */
    public RewardDTO findById(Connection conn, long rewardId) throws SQLException {
        try (ResultSet rs = SQLSelect.allColumns(new DirectReferenceTable(Tables.REWARDS))
                .addCondition(new BasicCondition(
                        new ColumnValueToken(Columns.REWARD_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(rewardId)))
                .execute(conn)) {
            if (rs.next()) {
                return toDTO(rs);
            }
            return null;
        }
    }

    /**
     * 후원 신청·취소용: 리워드 행을 잠그고 조회한다 (SELECT ... FOR UPDATE)
     * 반드시 트랜잭션의 첫 SQL로 호출할 것.
     * 잠금 전에 다른 SELECT를 하면 이후 합계가 옛날 값으로 읽혀 초과 판매가 생길 수 있다.
     * @param conn     커넥션
     * @param rewardId 리워드 id
     * @return 리워드 DTO, 없으면 null
     */
    public RewardDTO findByIdForUpdate(Connection conn, long rewardId) throws SQLException {
        final String FIND_FOR_UPDATE_SQL = "SELECT * FROM rewards WHERE reward_id = ? FOR UPDATE";

        try (PreparedStatement psmt = conn.prepareStatement(FIND_FOR_UPDATE_SQL)) {
            psmt.setLong(1, rewardId);

            try (ResultSet rs = psmt.executeQuery()) {
                return rs.next() ? toDTO(rs) : null;
            }
        }
    }

    /**
     * 프로젝트 ID 기반 조회 (reward_id 오름차순)
     * @param conn      커넥션
     * @param projectId 프로젝트 ID
     * @return 리워드 DTO 목록
     * @throws SQLException DB 오류 발생 시
     */
    public List<RewardDTO> findByProjectId(Connection conn, long projectId) throws SQLException {
        List<RewardDTO> rewards = new ArrayList<>();

        try (ResultSet rs = SQLSelect.allColumns(new DirectReferenceTable(Tables.REWARDS))
                .addCondition(new BasicCondition(
                        new ColumnValueToken(Columns.PROJECT_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(projectId)))
                .orderBy(new SimpleOrderBySelector(Columns.REWARD_ID, true))
                .execute(conn)) {
            while (rs.next()) {
                rewards.add(toDTO(rs));
            }
        }
        return rewards;
    }

    //ResultSet 묶기
    private RewardDTO toDTO(ResultSet rs) throws SQLException {
        RewardDTO dto = new RewardDTO();
        dto.setRewardId(rs.getLong("reward_id"));
        dto.setProjectId(rs.getLong("project_id"));
        dto.setName(rs.getString("name"));
        dto.setPrice(rs.getLong("price"));
        dto.setDescription(rs.getString("description"));

        // NULL을 0이 아닌 null로 읽기 위해 getObject 사용
        Number stockNum = (Number) rs.getObject(Columns.STOCK.name);
        dto.setStock(stockNum != null ? stockNum.longValue() : null);

        return dto;
    }
    // =================================================================
    // 3. UPDATE SECTION
    // =================================================================

    /**
     * 리워드 수정 기능
     * @param conn      커넥션
     * @param rewardDTO 수정할 리워드 정보 (rewardId 필수)
     * @return 영향받은 행 수
     * @throws SQLException DB 오류 발생 시
     */
    public int update(Connection conn, RewardDTO rewardDTO) throws SQLException {
        return new SQLUpdate(Tables.REWARDS, new UpdateSelector[]{
                new UpdateSelector(Columns.NAME, new UserValueToken(rewardDTO.getName())),
                new UpdateSelector(Columns.PRICE, new UserValueToken(rewardDTO.getPrice())),
                new UpdateSelector(Columns.DESCRIPTION, new UserValueToken(rewardDTO.getDescription())),
                new UpdateSelector(Columns.STOCK, new UserValueToken(rewardDTO.getStock()))
        })
                .addCondition(new BasicCondition(
                        new ColumnValueToken(Columns.REWARD_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(rewardDTO.getRewardId())))
                .execute(conn);
    }

    /**
     * 후원 성공 시 리워드 차감 용
     * @param conn     커넥션
     * @param rewardId 리워드 ID
     * @param quantity 차감할 수량
     * @return 영향받은 행 수 (0이면 재고 부족 또는 존재하지 않는 리워드)
     * @throws SQLException DB 오류 발생 시
     */
    public int decreaseStock(Connection conn, long rewardId, long quantity) throws SQLException {
        final String DECREASE_STOCK_SQL = """
                UPDATE rewards SET stock = stock - ?
                 WHERE reward_id = ? AND (stock IS NULL OR stock >= ?)
                """;
        try (PreparedStatement psmt = conn.prepareStatement(DECREASE_STOCK_SQL)) {
            psmt.setLong(1, quantity);
            psmt.setLong(2, rewardId);
            psmt.setLong(3, quantity);
            return psmt.executeUpdate();
        }
    }

    /**
     * 후원 취소 시 리워드 재고 복구 용
     * @param conn     트랜잭션 커넥션
     * @param rewardId 리워드 ID
     * @param quantity 복구할 수량
     * @return 영향받은 행 수
     * @throws SQLException DB 오류 발생 시
     */
    public int increaseStock(Connection conn, long rewardId, long quantity) throws SQLException {
        final String INCREASE_STOCK_SQL = "UPDATE rewards SET stock = stock + ? WHERE reward_id = ?";
        try (PreparedStatement psmt = conn.prepareStatement(INCREASE_STOCK_SQL)) {
            psmt.setLong(1, quantity);
            psmt.setLong(2, rewardId);
            return psmt.executeUpdate();
        }
    }

    // =================================================================
    // 4. DELETE SECTION
    // =================================================================

    /**
     * 특정 프로젝트의 모든 리워드를 삭제 기능
     * 주의@@: 반려 프로젝트 재신청 시 반드시 이미지 테이블 을 먼저 삭제해야함
     *
     * @param conn      커넥션
     * @param projectId 프로젝트 ID
     * @return 삭제된 행 수
     * @throws SQLException DB 오류 발생 시
     */
    public int deleteByProjectId(Connection conn, long projectId) throws SQLException {
        return new SQLDelete(Tables.REWARDS)
                .addCondition(new BasicCondition(
                        new ColumnValueToken(Columns.PROJECT_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(projectId)))
                .execute(conn);
    }
}
