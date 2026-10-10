package ypfunding.server.service;

import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.FailReasonsDAO;

public class FailReasonService extends Service<FailReasonsDAO>{
    public FailReasonService(TransactionManager transactionManager, FailReasonsDAO dao) {
        super(transactionManager, dao);
    }
}
