package ypfunding.server.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ypfunding.common.constant.DomainRules;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.exception.DataAccessException;
import ypfunding.server.exception.DuplicateReviewException;
import ypfunding.server.exception.NotAllowedException;
import ypfunding.server.support.Concurrent;
import ypfunding.server.support.DbTestBase;
import ypfunding.server.support.TestData;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReviewServiceTest extends DbTestBase {
    private static final int DURATION_DAYS = 10;
    private static final int PRICE = 10000;
    private static final int UNLIMITED = 0;

    /** 리뷰 테스트용 상황: 서포터 1명의 후원 1건 + 그 프로젝트 정보 */
    private record Fixture(LoginUser supporter, long projectId, long rewardId, long fundId) {}

    /**
     * 목표 금액을 정해서 프로젝트를 만들고 서포터 1명이 1개 후원한다.
     * goal = PRICE 이면 마감 후 성공, goal > PRICE 이면 마감 후 실패.
     */
    private Fixture setUp(long goal) {
        long makerId = tx.execute(conn -> TestData.insertUser(conn, "maker", UserType.MAKER, clock.now()));
        long supporterId = tx.execute(conn -> TestData.insertUser(conn, "sup", UserType.SUPPORTER, clock.now()));
        long projectId = tx.execute(conn -> TestData.insertApprovedProject(conn, makerId, goal, DURATION_DAYS, clock.now()));
        long rewardId = tx.execute(conn -> TestData.insertReward(conn, projectId, PRICE, UNLIMITED));
        long fundId = tx.execute(conn -> TestData.insertFund(conn, supporterId, projectId, rewardId, 1, clock.now()));
        return new Fixture(new LoginUser(supporterId, UserType.SUPPORTER), projectId, rewardId, fundId);
    }

    /** 목표를 딱 채운 프로젝트를 만들고 마감시킨다 (성공) */
    private Fixture setUpSuccessAndEnd() {
        Fixture f = setUp(PRICE);
        clock.plus(Duration.ofDays(DURATION_DAYS));
        return f;
    }

    private int reviewCount(long fundId) {
        return tx.execute(conn -> TestData.countReviews(conn, fundId));
    }

    private ReviewService reviewService() {
        return context.getReviewService();
    }

    // ===== 정상 =====
    @Test
    @DisplayName("성공한 마감 프로젝트의 본인 후원에는 리뷰를 쓸 수 있다")
    void writeSuccess() {
        Fixture f = setUpSuccessAndEnd();

        reviewService().writeReview(f.supporter(), f.fundId(), 5, "좋아요");

        assertEquals(1, reviewCount(f.fundId()));
    }

    @Test
    @DisplayName("리뷰 내용은 비워도 된다")
    void writeWithoutContent() {
        Fixture f = setUpSuccessAndEnd();

        reviewService().writeReview(f.supporter(), f.fundId(), 4, "   ");

        assertEquals(1, reviewCount(f.fundId()));
    }

    @Test
    @DisplayName("별점 경계값(최소, 최대)은 허용된다")
    void starAtBoundary() {
        Fixture f = setUpSuccessAndEnd();
        long otherId = tx.execute(conn -> TestData.insertUser(conn, "sup2", UserType.SUPPORTER, clock.now()));
        long otherFund = tx.execute(conn -> TestData.insertFund(conn, otherId, f.projectId(), f.rewardId(), 1, clock.now()));

        reviewService().writeReview(f.supporter(), f.fundId(), DomainRules.MIN_STAR, null);
        reviewService().writeReview(new LoginUser(otherId, UserType.SUPPORTER), otherFund, DomainRules.MAX_STAR, null);

        assertEquals(1, reviewCount(f.fundId()));
        assertEquals(1, reviewCount(otherFund));
    }

    // ===== 후원당 1회 =====
    @Test
    @DisplayName("같은 후원에 리뷰를 두 번 쓰면 중복으로 거부된다")
    void duplicateReview() {
        Fixture f = setUpSuccessAndEnd();
        reviewService().writeReview(f.supporter(), f.fundId(), 5, "첫 리뷰");

        assertThrows(DuplicateReviewException.class,
                () -> reviewService().writeReview(f.supporter(), f.fundId(), 3, "두 번째"));
        assertEquals(1, reviewCount(f.fundId()));
    }

    @Test
    @DisplayName("같은 후원에 동시에 5번 리뷰를 쓰면 1건만 저장된다")
    void concurrentDuplicateReview() {
        Fixture f = setUpSuccessAndEnd();

        List<Throwable> results = Concurrent.runAtOnce(5,
                i -> reviewService().writeReview(f.supporter(), f.fundId(), 5, "리뷰" + i));

        assertEquals(1, Concurrent.successCount(results));
        assertEquals(4, Concurrent.countOf(results, DuplicateReviewException.class));
        assertEquals(0, Concurrent.countOf(results, DataAccessException.class));
        assertEquals(1, reviewCount(f.fundId()));
    }

    // ===== 프로젝트 상태 =====
    @Test
    @DisplayName("진행 중인 프로젝트에는 리뷰를 쓸 수 없다")
    void ongoingProject() {
        Fixture f = setUp(PRICE);   // 마감시키지 않음

        assertThrows(BusinessException.class,
                () -> reviewService().writeReview(f.supporter(), f.fundId(), 5, "리뷰"));
        assertEquals(0, reviewCount(f.fundId()));
    }

    @Test
    @DisplayName("펀딩에 실패한 프로젝트에는 리뷰를 쓸 수 없다")
    void failedProject() {
        Fixture f = setUp(PRICE * 10);   // 목표 미달
        clock.plus(Duration.ofDays(DURATION_DAYS));

        assertThrows(BusinessException.class,
                () -> reviewService().writeReview(f.supporter(), f.fundId(), 5, "리뷰"));
        assertEquals(0, reviewCount(f.fundId()));
    }

    // ===== 후원 확인 =====
    @Test
    @DisplayName("남의 후원에는 리뷰를 쓸 수 없다")
    void othersFund() {
        Fixture f = setUpSuccessAndEnd();
        long otherId = tx.execute(conn -> TestData.insertUser(conn, "other", UserType.SUPPORTER, clock.now()));

        assertThrows(NotAllowedException.class,
                () -> reviewService().writeReview(new LoginUser(otherId, UserType.SUPPORTER), f.fundId(), 5, "리뷰"));
        assertEquals(0, reviewCount(f.fundId()));
    }

    @Test
    @DisplayName("취소한 후원에는 리뷰를 쓸 수 없다")
    void canceledFund() {
        Fixture f = setUp(PRICE);
        context.getFundService().cancelFund(f.supporter(), f.fundId(), "취소");   // 진행 중에 취소
        clock.plus(Duration.ofDays(DURATION_DAYS));

        assertThrows(BusinessException.class,
                () -> reviewService().writeReview(f.supporter(), f.fundId(), 5, "리뷰"));
    }

    // ===== 입력·권한 =====
    @Test
    @DisplayName("별점이 범위 밖(" + DomainRules.MIN_STAR + "," + DomainRules.MAX_STAR + ")이면 거부된다")
    void starOutOfRange() {
        Fixture f = setUpSuccessAndEnd();

        assertThrows(BusinessException.class,
                () -> reviewService().writeReview(f.supporter(), f.fundId(), DomainRules.MIN_STAR - 1, "리뷰"));
        assertThrows(BusinessException.class,
                () -> reviewService().writeReview(f.supporter(), f.fundId(), DomainRules.MAX_STAR + 1, "리뷰"));
        assertEquals(0, reviewCount(f.fundId()));
    }

    @Test
    @DisplayName("메이커는 리뷰를 쓸 수 없다")
    void makerIsRejected() {
        Fixture f = setUpSuccessAndEnd();
        long makerId = tx.execute(conn -> TestData.insertUser(conn, "maker2", UserType.MAKER, clock.now()));

        assertThrows(NotAllowedException.class,
                () -> reviewService().writeReview(new LoginUser(makerId, UserType.MAKER), f.fundId(), 5, "리뷰"));
    }
}
