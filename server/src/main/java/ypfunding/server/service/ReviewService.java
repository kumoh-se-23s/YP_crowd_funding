package ypfunding.server.service;

import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.ReviewsDAO;

public class ReviewService extends Service<ReviewsDAO>{
    public ReviewService(TransactionManager transactionManager, ReviewsDAO dao) {
        super(transactionManager, dao);
    }
}
