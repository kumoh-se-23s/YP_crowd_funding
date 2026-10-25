package ypfunding.server.persistence.dao;

import ypfunding.common.dto.FundDTO;
import ypfunding.common.dto.LikeDTO;
import ypfunding.server.exception.DuplicateFundException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class FundsDAO extends DAO{
    public enum Columns implements ColumnsEnum{
        FUND_ID("fund_id"),
        USER_ID("user_id"),
        PROJECT_ID("project_id"),
        REWARD_ID("reward_id"),
        QUANTITY("quantity"),
        DATE("date");

        private final String name;
        Columns(String name){
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public void fillDataFromResultSet(FundDTO dto, ResultSet rs)throws SQLException{
        dto.setFundId(rs.getLong(Columns.FUND_ID.toString()));
        dto.setUserId(rs.getLong(Columns.USER_ID.toString()));
        dto.setProjectId(rs.getLong(Columns.PROJECT_ID.toString()));
        dto.setRewardId(rs.getLong(Columns.REWARD_ID.toString()));
        dto.setQuantity(rs.getLong(Columns.QUANTITY.toString()));
        dto.setDate(rs.getObject(Columns.DATE.toString(), LocalDateTime.class));
    }

    // =================================================================
    // 1. CREATE SECTION
    // =================================================================

    public void insert(Connection conn, FundDTO fundDTO) throws SQLException {
        final String INSERT_SQL =
                "INSERT INTO funds (user_id, project_id, reward_id, quantity, date) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement psmt = conn.prepareStatement(INSERT_SQL)) {

            psmt.setLong(1, fundDTO.getUserId());
            psmt.setLong(2, fundDTO.getProjectId());
            psmt.setLong(3, fundDTO.getRewardId());
            psmt.setLong(4, fundDTO.getQuantity());
            psmt.setObject(5, fundDTO.getDate());

            psmt.executeUpdate();
        }catch (SQLException e){
            if(e.getErrorCode() == 1062) throw new DuplicateFundException(e);
            throw e;
        }
    }

    // =================================================================
    // 2. READ SECTION
    // =================================================================

    //후원 내역 조회 기능
    public FundDTO findById(Connection conn, long fundId) throws SQLException {
        final String FIND_BY_ID_SQL = "SELECT * FROM funds WHERE fund_id = ?";

        try (PreparedStatement psmt = conn.prepareStatement(FIND_BY_ID_SQL)) {
            psmt.setLong(1, fundId);

            try (ResultSet rs = psmt.executeQuery()) {
                if (rs.next()) {
                    FundDTO dto = new FundDTO();
                    fillDataFromResultSet(dto, rs);
                    return dto;
                }
                return null;
            }
        }
    }

    // 리워드의 유효한 후원 수량 합계. 후원이 없으면 0
    // 리워드 행을 먼저 FOR UPDATE로 잠근 뒤 호출해야 최신 값이 읽힌다
    public long sumQuantityByReward(Connection conn, long rewardId) throws SQLException {
        final String SUM_SQL = "SELECT COALESCE(SUM(quantity), 0) FROM funds WHERE reward_id = ?";

        try (PreparedStatement psmt = conn.prepareStatement(SUM_SQL)) {
            psmt.setLong(1, rewardId);

            try (ResultSet rs = psmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    // =================================================================
    // 3. UPDATE SECTION
    // =================================================================


    // =================================================================
    // 4. DELETE SECTION
    // =================================================================

    public int delete(Connection conn, long fundId) throws SQLException {
        final String DELETE_SQL = "DELETE FROM funds WHERE fund_id = ?";

        try ( PreparedStatement psmt = conn.prepareStatement(DELETE_SQL)) {
            psmt.setLong(1, fundId);

            return psmt.executeUpdate();
        }
    }
}
