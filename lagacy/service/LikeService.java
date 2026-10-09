package kr.yp_crowdfunding.service;

import kr.yp_crowdfunding.persistence.dao.LikesDAO;

public class LikeService extends Service<LikesDAO>{
    public LikeService(LikesDAO dao) {
        super(dao);
    }
}
