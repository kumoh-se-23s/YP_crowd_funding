package kr.yp_crowdfunding.service;

import kr.yp_crowdfunding.persistence.dao.UsersDAO;
import kr.yp_crowdfunding.persistence.dto.UserDTO;

import java.sql.SQLException;
import java.util.List;

public class UserService extends Service<UsersDAO>{

    public UserService(UsersDAO usersDAO) { //의존성 주입
        super(usersDAO);
    }

    public List<UserDTO> getAllUsers() throws SQLException {
        return dao.getAllUsers();
    }
}
