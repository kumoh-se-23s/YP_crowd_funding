package ypfunding.server.service;

import ypfunding.server.persistence.dao.FailReasonsDAO;

public class FailReasonService extends Service<FailReasonsDAO>{
    public FailReasonService(FailReasonsDAO dao) {
        super(dao);
    }
}
