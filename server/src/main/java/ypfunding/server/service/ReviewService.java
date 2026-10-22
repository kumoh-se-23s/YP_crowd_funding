package ypfunding.server.service;

import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.ReviewsDAO;

public class ReviewService extends Service{
    private final ReviewsDAO reviewsDAO;
    public ReviewService(TransactionManager transactionManager, ReviewsDAO reviewsDAO) {
        super(transactionManager);
        this.reviewsDAO = reviewsDAO;
    }
}
