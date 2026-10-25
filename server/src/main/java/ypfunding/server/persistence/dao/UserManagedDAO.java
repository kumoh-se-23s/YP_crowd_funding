package ypfunding.server.persistence.dao;

import ypfunding.common.type.UserType;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.condition.BasicCondition;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.token.ColumnValueToken;
import ypfunding.server.persistence.dao.dynamicsql.token.UserValueToken;
import ypfunding.server.persistence.dto.UserLoginDTO;

import java.sql.*;

public class UserManagedDAO extends DAO {



    // =================================================================
    // 1. CREATE SECTION
    // =================================================================
    private UserLoginDTO toLoginDTO(ResultSet rs) throws SQLException {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUserId(rs.getLong(UsersDAO.Columns.USER_ID.toString()));
        dto.setUserType(UserType.valueOf(rs.getString(UsersDAO.Columns.USERTYPE.toString())));
        dto.setLoginId(rs.getString(UsersDAO.Columns.LOGIN_ID.toString()));
        dto.setEncryptedPassword(rs.getString(UsersDAO.Columns.PASSWORD.toString()));
        dto.setSalt(rs.getString(UsersDAO.Columns.SALT.toString()));
        return dto;
    }

    // =================================================================
    // 2. READ SECTION
    // =================================================================

    public UserLoginDTO getUserByLoginId(Connection conn, String loginId) throws SQLException {
        try (ResultSet rs = SQLSelect.allColumns(Tables.USERS)
                .addCondition(new BasicCondition(new ColumnValueToken(UsersDAO.Columns.LOGIN_ID), ArithmeticConditionOperator.EQ, new UserValueToken(loginId)))
                .execute(conn)) {
            if (rs.next()) {
                return toLoginDTO(rs);
            }
        }
        return null;
    }
}
