package kr.yp_crowdfunding.service;

import kr.yp_crowdfunding.persistence.dao.FailReasonsDAO;

public class FailReasonService extends Service<FailReasonsDAO>{
    public FailReasonService(FailReasonsDAO dao) {
        super(dao);
    }
}
