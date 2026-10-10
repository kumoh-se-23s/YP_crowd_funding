package ypfunding.server.persistence.dao;

import ypfunding.common.dto.UserDTO;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsersDAO extends DAO {

    public enum Columns{
        ID("id"),
        ADDRESS("address"),
        NAME("name"),
        TYPE("type"),
        PASSWORD("password"),
        REGDATE("regDate"),
        LOGIN_ID("login_id");

        private final String name;
        Columns(String name){
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public UsersDAO(DataSource dataSource) {
        super(dataSource);
    }

    //Create(생성) 기능
    public void insert(UserDTO userDTO) throws SQLException {
        final String INSERT_SQL =
                "INSERT INTO users (address, name, type, regdate, login_id, password) VALUES (?, ?, ?, ?, ?, ?)";

        try(Connection conn = dataSource.getConnection();
            PreparedStatement psmt = conn.prepareStatement(INSERT_SQL)){

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
    public UserDTO getUserById(long id) throws SQLException {
        final String READ_SQL = "SELECT * FROM users WHERE id = ?";
        UserDTO dto = null;

        try(Connection conn = dataSource.getConnection();
        PreparedStatement psmt = conn.prepareStatement(READ_SQL)){

            psmt.setLong(1, id);

            try (ResultSet rs = psmt.executeQuery()){
                if(rs.next()){
                    dto = new UserDTO();

                    dto.setUserID(rs.getLong(Columns.ID.name));
                    dto.setAddress(rs.getString(Columns.ADDRESS.name));
                    dto.setName(rs.getString(Columns.NAME.name));
                    dto.setUserType(UserDTO.UserType.valueOf(rs.getString(Columns.TYPE.name)));
                    dto.setEncryptedPassword(rs.getString(Columns.PASSWORD.name));
                    dto.setRegDate(rs.getDate(Columns.REGDATE.name));
                    dto.setLoginID(rs.getString(Columns.LOGIN_ID.name));

                }
            }
        }
        return dto;
    }

    public List<UserDTO> getAllUsers() throws SQLException {
        final String sql = "select * from users";
        List<UserDTO> result = new ArrayList<>();

        try(Statement statement = dataSource.getConnection().createStatement();
            ResultSet rs = statement.executeQuery(sql)){

            while(rs.next()){
                UserDTO dto = new UserDTO();

                dto.setUserID(rs.getLong(Columns.ID.name));
                dto.setAddress(rs.getString(Columns.ADDRESS.name));
                dto.setName(rs.getString(Columns.NAME.name));
                dto.setUserType(UserDTO.UserType.valueOf(rs.getString(Columns.TYPE.name)));
                dto.setEncryptedPassword(rs.getString(Columns.PASSWORD.name));
                dto.setRegDate(rs.getDate(Columns.REGDATE.name));
                dto.setLoginID(rs.getString(Columns.LOGIN_ID.name));

                result.add(dto);
            }
        }
        return result;
    }

    //Update(수정) 기능
    public void update(UserDTO userDTO) throws SQLException {
        //id(pk), regdate는 수정 불가 type은 수정 허용하긴 해야하려나?
        final String UPDATE_SQL = "UPDATE users SET address = ?, name = ?, type = ?, login_id = ?, password = ? WHERE id = ?";

        try(Connection conn = dataSource.getConnection();
            PreparedStatement psmt = conn.prepareStatement(UPDATE_SQL)){

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
    public void delete(long id) throws SQLException {
        final String DELETE_SQL = "DELETE FROM users WHERE id = ?";

        try(Connection conn = dataSource.getConnection();
        PreparedStatement psmt = conn.prepareStatement(DELETE_SQL)){

            psmt.setLong(1, id);
            psmt.executeUpdate();
        }
    }
}
