package ypfunding.server.persistence.dao;

import ypfunding.common.dto.UserDTO;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLInsert;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UsersDAO extends DAO {

    public enum Columns implements ColumnsEnum{
        USER_ID("user_id"),
        LOGIN_ID("login_id"),
        PASSWORD("password"),
        SALT("salt"),
        NAME("name"),
        ADDRESS("address"),
        USERTYPE("usertype"),
        REGDATE("regdate");

        private final String name;
        Columns(String name){
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


    private void fillDataFromResultSet(UserDTO dto, ResultSet rs) throws SQLException {
        dto.setUserID(rs.getLong(Columns.USER_ID.name));
        dto.setLoginID(rs.getString(Columns.LOGIN_ID.name));
        dto.setEncryptedPassword(rs.getString(Columns.PASSWORD.name));
        dto.setName(rs.getString(Columns.NAME.name));
        dto.setAddress(rs.getString(Columns.ADDRESS.name));
        dto.setUserType(UserDTO.UserType.valueOf(rs.getString(Columns.USERTYPE.name)));
        dto.setRegDate(rs.getObject(Columns.REGDATE.name, LocalDateTime.class));
    }

    //TODO salt casdasjido nasichj oiauslhiaounsdyhaioudsjnsaoiludhjmoail
    public void insert(Connection conn, UserDTO userDTO, String salt) throws SQLException {
        new SQLInsert(Tables.USERS)
                .addTarget(Columns.LOGIN_ID, userDTO.getLoginID())
                .addTarget(Columns.PASSWORD, userDTO.getEncryptedPassword())
                .addTarget(Columns.SALT, salt)
                .addTarget(Columns.NAME, userDTO.getName())
                .addTarget(Columns.ADDRESS, userDTO.getAddress())
                .addTarget(Columns.USERTYPE, userDTO.getUserType())
                .execute(conn);

    }

    // =================================================================
    // 2. READ SECTION
    // =================================================================

    public UserDTO getUserById(Connection conn, long id) throws SQLException {
        final String READ_SQL = "SELECT * FROM users WHERE id = ?";
        UserDTO dto = null;

        try(PreparedStatement psmt = conn.prepareStatement(READ_SQL)){

            psmt.setLong(1, id);

            try (ResultSet rs = psmt.executeQuery()){
                if(rs.next()){
                    dto = new UserDTO();
                    fillDataFromResultSet(dto, rs);
                }
            }
        }
        return dto;
    }


    public List<UserDTO> getAllUsers(Connection conn) throws SQLException {
        final String sql = "select * from users";
        List<UserDTO> result = new ArrayList<>();

        try(Statement statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sql)){

            while(rs.next()){
                UserDTO dto = new UserDTO();

                fillDataFromResultSet(dto, rs);

                result.add(dto);
            }
        }
        return result;
    }

    // =================================================================
    // 3. UPDATE SECTION
    // =================================================================

    public void update(Connection conn, UserDTO userDTO) throws SQLException {
        //id(pk), regdate는 수정 불가 type은 수정 허용하긴 해야하려나?
        final String UPDATE_SQL = "UPDATE users SET address = ?, name = ?, type = ?, login_id = ?, password = ? WHERE id = ?";

        try(PreparedStatement psmt = conn.prepareStatement(UPDATE_SQL)){

            psmt.setString(1, userDTO.getAddress());
            psmt.setString(2, userDTO.getName());
            psmt.setString(3, userDTO.getUserType().name());
            psmt.setString(4, userDTO.getLoginID());
            psmt.setString(5, userDTO.getEncryptedPassword());
            psmt.setLong(6, userDTO.getUserID());

            psmt.executeUpdate();
        }
    }

    // =================================================================
    // 4. DELETE SECTION
    // =================================================================

    //Delete(삭제) 기능
    //아마 update로 삭제된 유저 덮을거 같긴한데 혹시 모를 삭제 대비로 만들긴 함
    public void delete(Connection conn, long id) throws SQLException {
        final String DELETE_SQL = "DELETE FROM users WHERE id = ?";

        try(PreparedStatement psmt = conn.prepareStatement(DELETE_SQL)){

            psmt.setLong(1, id);
            psmt.executeUpdate();
        }
    }
}
