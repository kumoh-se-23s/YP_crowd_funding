package ypfunding.server.persistence.dao;

import ypfunding.common.dto.UserDTO;
import ypfunding.common.type.UserType;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLUpdate;
import ypfunding.server.persistence.dao.dynamicsql.col.ColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.col.DirectColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.col.SimpleOrderBySelector;
import ypfunding.server.persistence.dao.dynamicsql.col.UpdateSelector;
import ypfunding.server.persistence.dao.dynamicsql.condition.BasicCondition;
import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.token.ColumnValueToken;
import ypfunding.server.persistence.dao.dynamicsql.token.UserValueToken;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UsersDAO extends DAO {


    public enum Columns implements ColumnsEnum {
        USER_ID("user_id"),
        LOGIN_ID("login_id"),
        PASSWORD("password"),
        SALT("salt"),
        NAME("name"),
        ADDRESS("address"),
        USERTYPE("usertype"),
        REGDATE("regdate");

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

    private UserDTO toDTO(ResultSet rs) throws SQLException {
        UserDTO dto = new UserDTO();
        dto.setUserId(rs.getLong(Columns.USER_ID.toString()));
        dto.setLoginId(rs.getString(Columns.LOGIN_ID.toString()));
        dto.setName(rs.getString(Columns.NAME.toString()));
        dto.setAddress(rs.getString(Columns.ADDRESS.toString()));
        dto.setUserType(UserType.valueOf(rs.getString(Columns.USERTYPE.toString())));
        dto.setRegDate(rs.getObject(Columns.REGDATE.toString(), LocalDateTime.class));
        return dto;
    }

    private List<UserDTO> toDTOList(ResultSet rs) throws SQLException {
        List<UserDTO> result = new ArrayList<>();
        while (rs.next()) {
            result.add(toDTO(rs));
        }
        return result;
    }

    // =================================================================
    // 2. READ SECTION
    // =================================================================

    public UserDTO getUserById(Connection conn, long userId) throws SQLException {
        try (ResultSet rs = selectUsers()
                .addCondition(eq(Columns.USER_ID, userId))
                .execute(conn)) {
            return rs.next() ? toDTO(rs) : null;
        }
    }

    public List<UserDTO> getAllAdmins(Connection conn) throws SQLException {
        try (ResultSet rs = selectUsers()
                .addCondition(eq(Columns.USERTYPE, UserType.ADMIN.name()))   //enum은 이름 문자열로
                .orderBy(new SimpleOrderBySelector(Columns.USER_ID, true))
                .execute(conn)) {
            return toDTOList(rs);
        }
    }

    public List<UserDTO> getAllUsers(Connection conn) throws SQLException {
        try (ResultSet rs = selectUsers()
                .orderBy(new SimpleOrderBySelector(Columns.USER_ID, true))
                .execute(conn)) {
            return toDTOList(rs);
        }
    }

    // =================================================================
    // 3. UPDATE SECTION
    // =================================================================

    public int updateProfile(Connection conn, long userId, String name, String address) throws SQLException {
        return new SQLUpdate(Tables.USERS, new UpdateSelector[]{
                new UpdateSelector(Columns.NAME, new UserValueToken(name)),
                new UpdateSelector(Columns.ADDRESS, new UserValueToken(address))
        })
                .addCondition(eq(Columns.USER_ID, userId))
                .execute(conn);
    }

    // 공통=================================================
    private SQLSelect selectUsers() {
        ColumnSelector[] columns = {
                new DirectColumnSelector(Columns.USER_ID),
                new DirectColumnSelector(Columns.LOGIN_ID),
                new DirectColumnSelector(Columns.NAME),
                new DirectColumnSelector(Columns.ADDRESS),
                new DirectColumnSelector(Columns.USERTYPE),
                new DirectColumnSelector(Columns.REGDATE)
        };
        return new SQLSelect(columns, new DirectReferenceTable(Tables.USERS));
    }

    //컬럼 == 값 조건
    private static Condition eq(ColumnsEnum column, Object value) {
        return new BasicCondition(new ColumnValueToken(column), ArithmeticConditionOperator.EQ, new UserValueToken(value));
    }


}
