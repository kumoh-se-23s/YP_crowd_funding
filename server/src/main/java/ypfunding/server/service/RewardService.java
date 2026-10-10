package ypfunding.server.service;

import ypfunding.server.persistence.dao.RewardsDAO;

public class RewardService extends Service<RewardsDAO>{
    public RewardService(RewardsDAO dao) {
        super(dao);
    }
}
