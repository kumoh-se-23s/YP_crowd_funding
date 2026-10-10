package ypfunding.server.service;

import ypfunding.server.persistence.dao.UsersDAO;
import ypfunding.common.dto.UserDTO;

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
