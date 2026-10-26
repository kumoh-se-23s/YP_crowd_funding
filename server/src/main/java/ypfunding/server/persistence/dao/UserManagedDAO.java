package ypfunding.server.persistence.dao;

import ypfunding.common.dto.UserDTO;
import ypfunding.common.type.UserType;
import ypfunding.server.persistence.dao.UsersDAO.Columns;
import ypfunding.server.exception.DuplicateLoginIdException;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLInsert;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLUpdate;
import ypfunding.server.persistence.dao.dynamicsql.col.ColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.col.DirectColumnSelector;
import ypfunding.server.persistence.dao.dynamicsql.col.UpdateSelector;
import ypfunding.server.persistence.dao.dynamicsql.condition.BasicCondition;
import ypfunding.server.persistence.dao.dynamicsql.condition.Condition;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.token.ColumnValueToken;
import ypfunding.server.persistence.dao.dynamicsql.token.UserValueToken;
import ypfunding.server.persistence.dto.UserLoginDTO;

import java.sql.*;

public class UserManagedDAO extends DAO {

    //MySQL 중복 키 에러 코드
    private static final int ER_DUP_ENTRY = 1062;

    // =================================================================
    // 1. CREATE SECTION
    // =================================================================
    private UserLoginDTO toLoginDTO(ResultSet rs) throws SQLException {
        UserLoginDTO dto = new UserLoginDTO();
        dto.setUserId(rs.getLong(UsersDAO.Columns.USER_ID.toString()));
        dto.setUserType(UserType.valueOf(rs.getString(UsersDAO.Columns.USERTYPE.toString())));
        dto.setLoginId(rs.getString(UsersDAO.Columns.LOGIN_ID.toString()));
        dto.setPasswordHash(rs.getString(UsersDAO.Columns.PASSWORD.toString()));
        dto.setSalt(rs.getString(UsersDAO.Columns.SALT.toString()));
        return dto;
    }

    public long insert(Connection conn, UserDTO user, String passwordHash, String salt) throws SQLException {
        try {
            new SQLInsert(Tables.USERS)
                    .addTarget(Columns.LOGIN_ID, user.getLoginId())
                    .addTarget(Columns.PASSWORD, passwordHash)
                    .addTarget(Columns.SALT, salt)
                    .addTarget(Columns.NAME, user.getName())
                    .addTarget(Columns.ADDRESS, user.getAddress())
                    .addTarget(Columns.USERTYPE, user.getUserType().name())   //enum은 이름 문자열로
                    .addTarget(Columns.REGDATE, user.getRegDate())
                    .execute(conn);
        } catch (SQLException e) {
            //id중복
            if (e.getErrorCode() == ER_DUP_ENTRY) {
                throw new DuplicateLoginIdException(e);
            }
            throw e;
        }

        return getByLoginId(conn, user.getLoginId()).getUserId();
    }

    // =================================================================
    // 2. READ SECTION
    // =================================================================

    public UserLoginDTO getByLoginId(Connection conn, String loginId) throws SQLException {
        try (ResultSet rs = selectLoginInfo()
                .addCondition(eq(Columns.LOGIN_ID, loginId))
                .execute(conn)) {
            return rs.next() ? toLoginDTO(rs) : null;
        }
    }

    public UserLoginDTO getById(Connection conn, long userId) throws SQLException {
        try (ResultSet rs = selectLoginInfo()
                .addCondition(eq(Columns.USER_ID, userId))
                .execute(conn)) {
            return rs.next() ? toLoginDTO(rs) : null;
        }
    }

    // =================================================================
    // 3. UPDATE
    // =================================================================
    public int updatePassword(Connection conn, long userId, String oldHash, String newHash, String newSalt)
            throws SQLException {
        return new SQLUpdate(Tables.USERS, new UpdateSelector[]{
                new UpdateSelector(Columns.PASSWORD, new UserValueToken(newHash)),
                new UpdateSelector(Columns.SALT, new UserValueToken(newSalt))
        }).addCondition(Condition.and(eq(Columns.USER_ID, userId), eq(Columns.PASSWORD, oldHash))).execute(conn);
    }


    //공통========================================================
    private SQLSelect selectLoginInfo() {
        ColumnSelector[] columns = {
                new DirectColumnSelector(Columns.USER_ID),
                new DirectColumnSelector(Columns.USERTYPE),
                new DirectColumnSelector(Columns.LOGIN_ID),
                new DirectColumnSelector(Columns.PASSWORD),
                new DirectColumnSelector(Columns.SALT)
        };
        return new SQLSelect(columns, new DirectReferenceTable(Tables.USERS));
    }

    //컬럼 = 값 조건
    private static Condition eq(ColumnsEnum column, Object value) {
        return new BasicCondition(new ColumnValueToken(column), ArithmeticConditionOperator.EQ, new UserValueToken(value));
    }
}
