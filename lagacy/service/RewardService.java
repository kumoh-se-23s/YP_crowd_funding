package kr.yp_crowdfunding.service;

import kr.yp_crowdfunding.persistence.dao.RewardsDAO;

public class RewardService extends Service<RewardsDAO>{
    public RewardService(RewardsDAO dao) {
        super(dao);
    }
}
