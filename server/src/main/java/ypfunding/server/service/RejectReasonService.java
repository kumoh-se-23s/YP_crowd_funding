package ypfunding.server.service;

import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.RejectDAO;

public class RejectReasonService extends Service{
    private final RejectDAO rejectDAO;
    public RejectReasonService(TransactionManager transactionManager, RejectDAO rejectDAO) {
        super(transactionManager);
        this.rejectDAO = rejectDAO;
    }
}
