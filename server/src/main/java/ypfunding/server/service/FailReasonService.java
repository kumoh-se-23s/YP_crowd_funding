package ypfunding.server.service;

import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.RejectDAO;

public class FailReasonService extends Service<RejectDAO>{
    public FailReasonService(TransactionManager transactionManager, RejectDAO dao) {
        super(transactionManager, dao);
    }
}
