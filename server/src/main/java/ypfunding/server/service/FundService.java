package ypfunding.server.service;

import ypfunding.server.persistence.dao.FundsDAO;

public class FundService extends Service<FundsDAO>{
    public FundService(FundsDAO dao) {
        super(dao);
    }
}
