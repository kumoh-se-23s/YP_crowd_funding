package ypfunding.server.service;

import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.LikesDAO;


public class LikeService extends Service{
    private final LikesDAO likesDAO;
    public LikeService(TransactionManager transactionManager, LikesDAO likesDAO) {
        super(transactionManager);
        this.likesDAO = likesDAO;
    }
}
