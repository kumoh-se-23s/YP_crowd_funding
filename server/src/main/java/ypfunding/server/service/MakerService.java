package ypfunding.server.service;

import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.UsersDAO;

public class MakerService extends UserService{
    public MakerService(TransactionManager transactionManager, UsersDAO usersDAO) {
        super(transactionManager, usersDAO);
    }
}
