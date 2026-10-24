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
        dto.setUserID(rs.getLong(Columns.USER_ID.toString()));
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

            psmt.setLong(1, fundDTO.getUserID());
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
