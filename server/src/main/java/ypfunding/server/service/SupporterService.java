package ypfunding.server.service;

import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.UsersDAO;

public class SupporterService extends UserService{
    public SupporterService(TransactionManager transactionManager, UsersDAO usersDAO) {
        super(transactionManager, usersDAO);
    }
}
