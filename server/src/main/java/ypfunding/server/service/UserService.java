package ypfunding.server.service;

import lombok.Getter;
import ypfunding.common.dto.UserDTO;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.UsersDAO;

import java.util.List;

public class UserService extends Service{
    @Getter
    private final UsersDAO usersDAO;
    public UserService(TransactionManager transactionManager, UsersDAO usersDAO) {
        super(transactionManager);
        this.usersDAO = usersDAO;
    }

    public List<UserDTO> getAllUsers() {
        return transactionManager.execute(usersDAO::getAllUsers);
    }


    public UserDTO getUserById(LoginUser user) {
        return transactionManager.execute(conn -> usersDAO.getUserById(conn, user.userId()));
    }
}
