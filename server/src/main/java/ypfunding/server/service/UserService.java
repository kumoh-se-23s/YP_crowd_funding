package ypfunding.server.service;

import ypfunding.common.dto.UserDTO;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.UsersDAO;

import java.util.List;

public class UserService extends Service{
    private final UsersDAO usersDAO;
    public UserService(TransactionManager transactionManager, UsersDAO usersDAO) {
        super(transactionManager);
        this.usersDAO = usersDAO;
    }

    public List<UserDTO> getAllUsers() {
        return transactionManager.execute(usersDAO::getAllUsers);
    }
}
