package ypfunding.server.service;

import ypfunding.common.constant.DomainRules;
import ypfunding.common.dto.FundDTO;
import ypfunding.common.dto.ReviewDTO;
import ypfunding.common.type.ProjectFundingStatus;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.exception.NotAllowedException;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.FundQueryDAO;
import ypfunding.server.persistence.dao.FundsDAO;
import ypfunding.server.persistence.dao.ReviewsDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;

public class ReviewService extends Service{
    private final ReviewsDAO reviewsDAO;
    private final FundsDAO fundsDAO;
    private final FundQueryDAO fundQueryDAO;
    private final Clock clock;

    public ReviewService(TransactionManager transactionManager, ReviewsDAO reviewsDAO, FundsDAO fundsDAO,
                         FundQueryDAO fundQueryDAO, Clock clock) {
        super(transactionManager);
        this.reviewsDAO = reviewsDAO;
        this.fundsDAO = fundsDAO;
        this.fundQueryDAO = fundQueryDAO;
        this.clock = clock;
    }

    // =================================================================
    // 리뷰 작성: 본인이 후원, 프로젝트 마감, 펀딩 성공일 때만, 후원당 1회
    // =================================================================

    public void writeReview(LoginUser user, long fundId, int star, String content) {
        requireSupporter(user, "서포터만 리뷰를 작성할 수 있습니다.");
        requireValidStar(star);
        LocalDateTime now = LocalDateTime.now(clock);

        transactionManager.execute(conn -> {
            FundDTO fund = findOwnFund(conn, user, fundId);
            requireSuccessfulProject(conn, fund.getProjectId(), now);
            reviewsDAO.insert(conn, newReview(fundId, star, content, now));
            return null;
        });
    }

    // ----- 트랜잭션 밖 검사 -----
    private static void requireSupporter(LoginUser user, String message) {
        if (user.userType() != UserType.SUPPORTER) {
            throw new NotAllowedException(message);
        }
    }

    private static void requireValidStar(int star) {
        if (star < DomainRules.MIN_STAR || star > DomainRules.MAX_STAR) {
            throw new BusinessException("별점은 " + DomainRules.MIN_STAR + "점 이상 "
                    + DomainRules.MAX_STAR + "점 이하로 선택해 주세요.");
        }
    }

    // ----- 트랜잭션 안 검사 -----
    /** 본인의 유효한 후원을 찾는다. 취소한 후원은 funds에서 지워져 있어 "존재하지 않음"으로 걸린다 */
    private FundDTO findOwnFund(Connection conn, LoginUser user, long fundId) throws SQLException {
        FundDTO fund = fundsDAO.findById(conn, fundId);
        if (fund == null) {
            throw new BusinessException("존재하지 않는 후원입니다.");
        }
        if (fund.getUserId() != user.userId()) {
            throw new NotAllowedException("본인의 후원에만 리뷰를 작성할 수 있습니다.");
        }
        return fund;
    }

    /** 마감되었고 펀딩에 성공한 프로젝트인지. 진행 중·실패 모두 거부 */
    private void requireSuccessfulProject(Connection conn, long projectId, LocalDateTime now) throws SQLException {
        ProjectFundingStatus status = fundQueryDAO.findProjectStatus(conn, projectId, now);
        if (status != ProjectFundingStatus.SUCCESS) {
            throw new BusinessException("펀딩에 성공한 마감 프로젝트에만 리뷰를 작성할 수 있습니다.");
        }
    }

    // ----- DTO 만들기 -----
    private static ReviewDTO newReview(long fundId, int star, String content, LocalDateTime now) {
        ReviewDTO review = new ReviewDTO();
        review.setFundId(fundId);
        review.setStar(star);
        review.setContent(content == null || content.isBlank() ? null : content);   // 내용은 선택
        review.setDate(now);
        return review;
    }
}
