package kr.yp_crowdfunding.service;

import kr.yp_crowdfunding.persistence.dao.ReviewsDAO;

public class ReviewService extends Service<ReviewsDAO>{
    public ReviewService(ReviewsDAO dao) {
        super(dao);
    }
}
