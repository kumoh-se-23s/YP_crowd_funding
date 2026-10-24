package ypfunding.server.persistence.dao;

import ypfunding.common.dto.CanceledFundDTO;
import ypfunding.common.dto.FundDTO;
import ypfunding.server.exception.DuplicateFundException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class CanceledFundsDAO extends DAO{
    public enum Columns implements ColumnsEnum{
        CANCELED_FUND_ID("canceled_funds_id"),
        USER_ID("user_id"),
        PROJECT_ID("project_id"),
        REWARD_ID("reward_id"),
        QUANTITY("quantity"),
        CONTENT("content"),
        CANCELED_DATE("cancel_date");

        private final String name;
        Columns(String name){
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public void fillDataFromResultSet(CanceledFundDTO dto, ResultSet rs) throws SQLException {
        dto.setCanceledFundId(rs.getLong(Columns.CANCELED_FUND_ID.toString()));
        dto.setUserId(rs.getLong(Columns.USER_ID.toString()));
        dto.setProjectId(rs.getLong(Columns.PROJECT_ID.toString()));
        dto.setRewardId(rs.getLong(Columns.REWARD_ID.toString()));
        dto.setQuantity(rs.getLong(Columns.QUANTITY.toString()));
        dto.setContent(rs.getString(Columns.CONTENT.toString()));
        dto.setCancelDate(rs.getObject(Columns.CANCELED_DATE.toString(), LocalDateTime.class));
    }

    // =================================================================
    // 1. CREATE SECTION
    // =================================================================

    public void insert(Connection conn, CanceledFundDTO canceledFundDTO) throws SQLException {
        final String INSERT_SQL =
                "INSERT INTO canceled_funds (user_id, project_id, reward_id, quantity, content, cancel_date) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement psmt = conn.prepareStatement(INSERT_SQL)) {

            psmt.setLong(1, canceledFundDTO.getUserId());
            psmt.setLong(2, canceledFundDTO.getProjectId());
            psmt.setLong(3, canceledFundDTO.getRewardId());
            psmt.setLong(4, canceledFundDTO.getQuantity());
            psmt.setString(5, canceledFundDTO.getContent());
            psmt.setObject(6, canceledFundDTO.getCancelDate());

            psmt.executeUpdate();
        }
    }

    // =================================================================
    // 2. READ SECTION
    // =================================================================

    // =================================================================
    // 3. UPDATE SECTION
    // =================================================================


    // =================================================================
    // 4. DELETE SECTION
    // =================================================================

}
