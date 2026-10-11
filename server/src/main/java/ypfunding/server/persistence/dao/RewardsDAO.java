package ypfunding.server.persistence.dao;

import ypfunding.common.dto.RewardDTO;
import ypfunding.server.util.DynamicSQLUtil;

import java.sql.*;
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


    public void fillDataFromResultSet(RewardDTO dto, PreparedStatement psmt) throws SQLException {

    }

    // =================================================================
    // 1. CREATE SECTION
    // =================================================================

    public void insert(Connection conn, RewardDTO rewardDTO) throws SQLException {
        final String INSERT_SQL = DynamicSQLUtil.generate("INSERT INTO rewards ({}) VALUES ({})",
                Columns.PROJECT_ID, Columns.NAME, Columns.PRICE, Columns.DESCRIPTION, Columns.STOCK);

        try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
            setInsertParameters(ps, rewardDTO);
            ps.executeUpdate();
        }
    }

    private void setInsertParameters(PreparedStatement ps, RewardDTO rewardDTO) throws SQLException {
        ps.setLong(1, rewardDTO.getProjectId());
        ps.setString(2, rewardDTO.getName());
        ps.setLong(3, rewardDTO.getPrice());
        ps.setString(4, rewardDTO.getDescription());
        // NULL 가능 컬럼(무제한 재고) 처리
        if (rewardDTO.getStock() != null) {
            ps.setLong(5, rewardDTO.getStock());
        } else {
            ps.setNull(5, Types.INTEGER); // NULL은 무제한을 의미
        }
    }

    // =================================================================
    // 2. READ SECTION
    // =================================================================

    //프로젝트 ID 기반 조회
    public List<RewardDTO> findByProjectId(Connection conn, Long projectId) throws SQLException {
        final String FIND_BY_PROJECT_ID = "SELECT * FROM rewards WHERE project_id = ?";

        List<RewardDTO> rewards = new ArrayList<>();

        try (PreparedStatement psmt = conn.prepareStatement(FIND_BY_PROJECT_ID)) {
            psmt.setLong(1, projectId);
            try (ResultSet rs = psmt.executeQuery()) {
                while (rs.next()) {
                    rewards.add(toDTO(rs));
                }
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
        dto.setStock(Long.valueOf(rs.getObject("stock", Integer.class)));

        return dto;
    }
    // =================================================================
    // 3. UPDATE SECTION
    // =================================================================

    // 후원 성공 시 리워드 차감 용
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

    // 후원 취소 시 리워드 재고 복구 용
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

    public int delete(Connection conn, long projectId) throws SQLException {
        final String DELETE_SQL = "DELETE FROM rewards WHERE project_id = ?";

        try (PreparedStatement psmt = conn.prepareStatement(DELETE_SQL)) {
            psmt.setLong(1, projectId);
            return psmt.executeUpdate();
        }
    }
}
