package kr.yp_crowdfunding.service;

import kr.yp_crowdfunding.persistence.dao.FundsDAO;

public class FundService extends Service<FundsDAO>{
    public FundService(FundsDAO dao) {
        super(dao);
    }
}
