package ypfunding.server.service;

import ypfunding.server.persistence.dao.LikesDAO;

public class LikeService extends Service<LikesDAO>{
    public LikeService(LikesDAO dao) {
        super(dao);
    }
}
