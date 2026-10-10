package ypfunding.server.service;

import ypfunding.server.persistence.dao.ReviewsDAO;

public class ReviewService extends Service<ReviewsDAO>{
    public ReviewService(ReviewsDAO dao) {
        super(dao);
    }
}
