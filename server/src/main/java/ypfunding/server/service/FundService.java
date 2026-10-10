package ypfunding.server.service;

import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.FundsDAO;

public class FundService extends Service<FundsDAO>{
    public FundService(TransactionManager transactionManager, FundsDAO dao) {
        super(transactionManager, dao);
    }
}
