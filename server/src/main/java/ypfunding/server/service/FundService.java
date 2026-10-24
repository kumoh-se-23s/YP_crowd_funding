package ypfunding.server.service;

import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.FundsDAO;

public class FundService extends Service{
    private final FundsDAO fundsDAO;
    public FundService(TransactionManager transactionManager, FundsDAO fundsDAO) {
        super(transactionManager);
        this.fundsDAO = fundsDAO;
    }
}
