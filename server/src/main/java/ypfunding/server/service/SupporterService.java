package ypfunding.server.service;

import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.UsersDAO;

public class SupporterService extends Service{
    UsersDAO usersDAO;
    public SupporterService(TransactionManager transactionManager, UsersDAO usersDAO) {
        super(transactionManager);
        this.usersDAO = usersDAO;
    }
}
