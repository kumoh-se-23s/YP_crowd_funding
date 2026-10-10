package ypfunding.server.service;

import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.LikesDAO;


public class LikeService extends Service<LikesDAO>{
    public LikeService(TransactionManager transactionManager, LikesDAO dao) {
        super(transactionManager, dao);
    }
}
