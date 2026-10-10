package ypfunding.server.service;

import ypfunding.common.dto.UserDTO;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.UsersDAO;

import java.util.List;

public class UserService extends Service<UsersDAO>{
    
    public UserService(TransactionManager transactionManager, UsersDAO dao) {
        super(transactionManager, dao);
    }

    public List<UserDTO> getAllUsers() {
        return transactionManager.execute(dao::getAllUsers);
    }
}
