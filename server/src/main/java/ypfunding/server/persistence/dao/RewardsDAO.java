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
        new SQLInsert(Tables.REWARDS)
                .addTarget(Columns.PROJECT_ID, rewardDTO.getProjectId())
                .addTarget(Columns.NAME, rewardDTO.getName())
                .addTarget(Columns.PRICE, rewardDTO.getPrice())
                .addTarget(Columns.DESCRIPTION, rewardDTO.getDescription())
                .addTarget(Columns.STOCK, rewardDTO.getStock())
                .execute(conn);
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
     * 주의@@: 반려 프로젝트 재신청 시 반드시 이미지 테이블 을 먼저 삭제!!
     *
     * @param conn      커넥션
     * @param projectId 프로젝트 ID
     * @return 삭제된 행 수
     * @throws SQLException DB 오류 발생 시
     */
    public int delete(Connection conn, long projectId) throws SQLException {
        return new SQLDelete(Tables.REWARDS)
                .addCondition(new BasicCondition(
                        new ColumnValueToken(Columns.PROJECT_ID),
                        ArithmeticConditionOperator.EQ,
                        new UserValueToken(projectId)))
                .execute(conn);
    }
}
