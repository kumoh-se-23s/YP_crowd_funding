package ypfunding.server.persistence.dao;

import ypfunding.common.dto.UserDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsersDAO extends DAO {

    public enum Columns{
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


    private void fillDataFromResultSet(UserDTO dto, ResultSet rs) throws SQLException {
        dto.setUserID(rs.getLong(Columns.USER_ID.name));
        dto.setLoginID(rs.getString(Columns.LOGIN_ID.name));
        dto.setEncryptedPassword(rs.getString(Columns.PASSWORD.name));
        dto.setName(rs.getString(Columns.NAME.name));
        dto.setAddress(rs.getString(Columns.ADDRESS.name));
        dto.setUserType(UserDTO.UserType.valueOf(rs.getString(Columns.USERTYPE.name)));
        dto.setRegDate(rs.getDate(Columns.REGDATE.name));
    }

    //Create(생성) 기능
    public void insert(Connection conn, UserDTO userDTO) throws SQLException {
        final String INSERT_SQL =
                "INSERT INTO users (address, name, type, regdate, login_id, password) VALUES (?, ?, ?, ?, ?, ?)";

        try(PreparedStatement psmt = conn.prepareStatement(INSERT_SQL)){

            psmt.setString(1, userDTO.getAddress());
            psmt.setString(2, userDTO.getName());
            psmt.setString(3, userDTO.getUserType().name());
            psmt.setTimestamp(4, new java.sql.Timestamp(userDTO.getRegDate().getTime()));
            psmt.setString(5, userDTO.getLoginID());
            psmt.setString(6, userDTO.getEncryptedPassword());

            psmt.executeUpdate();
        }
    }

    //Read(조회)기능
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

    //Update(수정) 기능
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
