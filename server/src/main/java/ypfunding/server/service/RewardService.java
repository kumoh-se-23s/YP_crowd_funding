package ypfunding.server.service;

import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.RewardsDAO;

public class RewardService extends Service<RewardsDAO> {
    public RewardService(TransactionManager transactionManager, RewardsDAO dao) {
        super(transactionManager, dao);
    }
}
