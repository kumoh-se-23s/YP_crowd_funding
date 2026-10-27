package ypfunding.server.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ypfunding.common.constant.DomainRules;
import ypfunding.common.dto.FundQueryDTO;
import ypfunding.common.type.ProjectFundingStatus;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.*;
import ypfunding.server.support.Concurrent;
import ypfunding.server.support.DbTestBase;
import ypfunding.server.support.TestData;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FundServiceTest extends DbTestBase {
    private static final int DURATION_DAYS = 30;
    private static final int UNLIMITED = 0;

    private long insertProject() {
        long makerId = tx.execute(conn -> TestData.insertUser(conn, "maker", UserType.MAKER, clock.now()));
        return tx.execute(conn -> TestData.insertApprovedProject(conn, makerId, 100000, DURATION_DAYS, clock.now()));
    }

    private long insertReward(long projectId, int stock) {
        return tx.execute(conn -> TestData.insertReward(conn, projectId, 10000, stock));
    }

    private LoginUser supporter(String loginId) {
        long id = tx.execute(conn -> TestData.insertUser(conn, loginId, UserType.SUPPORTER, clock.now()));
        return new LoginUser(id, UserType.SUPPORTER);
    }

    private long soldOf(long rewardId) {
        return tx.execute(conn -> TestData.sumFundQuantity(conn, rewardId));
    }

    private long stockOf(long rewardId) {
        return tx.execute(conn -> TestData.getStock(conn, rewardId));
    }

    private FundService fundService() {
        return context.getFundService();
    }

    @Test
    @DisplayName("후원하면 팔린 수량이 늘고, stock(제공 수량)은 그대로다")
    void applySuccess() {
        long rewardId = insertReward(insertProject(), 10);

        fundService().applyFund(supporter("sup"), rewardId, 3);

        assertEquals(3, soldOf(rewardId));
        assertEquals(10, stockOf(rewardId));
    }

    @Test
    @DisplayName("무제한(stock 0) 리워드는 최대 수량을 사도 성공한다")
    void unlimitedReward() {
        long rewardId = insertReward(insertProject(), UNLIMITED);

        fundService().applyFund(supporter("sup1"), rewardId, 10);
        fundService().applyFund(supporter("sup2"), rewardId, 10);

        assertEquals(20, soldOf(rewardId));
    }

    @Test
    @DisplayName("남은 수량을 딱 맞게 사면 성공한다 (경계)")
    void exactRemaining() {
        long rewardId = insertReward(insertProject(), 5);

        fundService().applyFund(supporter("sup1"), rewardId, 2);
        fundService().applyFund(supporter("sup2"), rewardId, 3);

        assertEquals(5, soldOf(rewardId));
    }

    @Test
    @DisplayName("남은 수량보다 많이 신청하면 품절로 실패하고 저장되지 않는다")
    void notEnoughRemaining() {
        long rewardId = insertReward(insertProject(), 5);
        fundService().applyFund(supporter("sup1"), rewardId, 3);

        assertThrows(SoldOutException.class,
                () -> fundService().applyFund(supporter("sup2"), rewardId, 3));
        assertEquals(3, soldOf(rewardId));
    }

    @Test
    @DisplayName("같은 프로젝트의 다른 리워드라도 두 번 후원하면 중복으로 실패한다")
    void duplicateFund() {
        long projectId = insertProject();
        long rewardA = insertReward(projectId, 10);
        long rewardB = insertReward(projectId, 10);
        LoginUser sup = supporter("sup");

        fundService().applyFund(sup, rewardA, 1);

        assertThrows(DuplicateFundException.class, () -> fundService().applyFund(sup, rewardB, 1));
        assertEquals(1, soldOf(rewardA));
        assertEquals(0, soldOf(rewardB));
    }

    @Test
    @DisplayName("수량이 범위 밖("+ (DomainRules.MIN_FUND_QUANTITY - 1) + "," +
            (DomainRules.MAX_FUND_QUANTITY + 1) + ")이면 거부된다")
    void quantityOutOfRange() {
        long rewardId = insertReward(insertProject(), UNLIMITED);
        LoginUser sup = supporter("sup");

        assertThrows(BusinessException.class, () -> fundService().applyFund(sup, rewardId, DomainRules.MIN_FUND_QUANTITY - 1));
        assertThrows(BusinessException.class, () -> fundService().applyFund(sup, rewardId, DomainRules.MAX_FUND_QUANTITY + 1));
        assertEquals(0, soldOf(rewardId));
    }

    @Test
    @DisplayName("메이커는 후원할 수 없다")
    void makerIsRejected() {
        long rewardId = insertReward(insertProject(), 10);
        long makerId = tx.execute(conn -> TestData.insertUser(conn, "maker2", UserType.MAKER, clock.now()));

        assertThrows(PermissionDeniedException.class,
                () -> fundService().applyFund(new LoginUser(makerId, UserType.MAKER), rewardId, 1));
    }

    @Test
    @DisplayName("존재하지 않는 리워드에는 후원할 수 없다")
    void unknownReward() {
        assertThrows(BusinessException.class,
                () -> fundService().applyFund(supporter("sup"), 999999L, 1));
    }

    @Test
    @DisplayName("승인 대기 중인 프로젝트에는 후원할 수 없다")
    void pendingProject() {
        long makerId = tx.execute(conn -> TestData.insertUser(conn, "maker", UserType.MAKER, clock.now()));
        long projectId = tx.execute(conn -> TestData.insertPendingProject(conn, makerId, 100000, DURATION_DAYS));
        long rewardId = insertReward(projectId, 10);

        assertThrows(BusinessException.class, () -> fundService().applyFund(supporter("sup"), rewardId, 1));
    }

    @Test
    @DisplayName("종료 1초 전에는 후원할 수 있다")
    void justBeforeEnd() {
        long rewardId = insertReward(insertProject(), 10);
        LoginUser sup = supporter("sup");

        clock.plus(Duration.ofDays(DURATION_DAYS).minusSeconds(1));

        fundService().applyFund(sup, rewardId, 1);
        assertEquals(1, soldOf(rewardId));
    }

    @Test
    @DisplayName("종료 시각이 되면 후원할 수 없다")
    void atEnd() {
        long rewardId = insertReward(insertProject(), 10);
        LoginUser sup = supporter("sup");

        clock.plus(Duration.ofDays(DURATION_DAYS));

        assertThrows(BusinessException.class, () -> fundService().applyFund(sup, rewardId, 1));
        assertEquals(0, soldOf(rewardId));
    }

    // ===== 동시성 (평가표 19) =====
    @Test
    @DisplayName("제공 수량 1개에 20명이 동시에 신청하면 1명만 성공하고 19명은 품절")
    void concurrentSoldOut() {
        long rewardId = insertReward(insertProject(), 1);
        List<LoginUser> users = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            users.add(supporter("sup" + i));
        }

        List<Throwable> results = Concurrent.runAtOnce(20,
                i -> fundService().applyFund(users.get(i), rewardId, 1));

        assertEquals(1, Concurrent.successCount(results));
        assertEquals(19, Concurrent.countOf(results, SoldOutException.class));
        assertEquals(0, Concurrent.countOf(results, DataAccessException.class));
        assertEquals(1, soldOf(rewardId));
    }

    @Test
    @DisplayName("같은 사용자가 같은 리워드에 동시에 10번 신청하면 1건만 성공")
    void concurrentDuplicateSameReward() {
        long rewardId = insertReward(insertProject(), UNLIMITED);
        LoginUser sup = supporter("sup");

        List<Throwable> results = Concurrent.runAtOnce(10,
                i -> fundService().applyFund(sup, rewardId, 1));

        assertEquals(1, Concurrent.successCount(results));
        assertEquals(9, Concurrent.countOf(results, DuplicateFundException.class));
        assertEquals(0, Concurrent.countOf(results, DataAccessException.class));
        assertEquals(1, soldOf(rewardId));
    }

    @Test
    @DisplayName("같은 사용자가 한 프로젝트의 서로 다른 리워드에 동시에 신청하면 1건만 성공")
    void concurrentDuplicateDifferentRewards() {
        long projectId = insertProject();
        List<Long> rewards = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            rewards.add(insertReward(projectId, UNLIMITED));
        }
        LoginUser sup = supporter("sup");

        List<Throwable> results = Concurrent.runAtOnce(5,
                i -> fundService().applyFund(sup, rewards.get(i), 1));

        assertEquals(1, Concurrent.successCount(results));
        assertEquals(4, Concurrent.countOf(results, DuplicateFundException.class));
        assertEquals(0, Concurrent.countOf(results, DataAccessException.class));
    }

    @Test
    @DisplayName("서로 다른 리워드에 여러 명이 동시에 신청해도 데드락 없이 전부 성공")
    void concurrentDifferentRewardsNoDeadlock() {
        long projectId = insertProject();
        List<Long> rewards = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            rewards.add(insertReward(projectId, 10));
        }
        List<LoginUser> users = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            users.add(supporter("sup" + i));
        }

        // 20명이 리워드 5개에 4명씩 나눠서 동시에 신청
        List<Throwable> results = Concurrent.runAtOnce(20,
                i -> fundService().applyFund(users.get(i), rewards.get(i % 5), 1));

        assertEquals(20, Concurrent.successCount(results));
        assertEquals(0, Concurrent.countOf(results, DataAccessException.class));
        for (long rewardId : rewards) {
            assertEquals(4, soldOf(rewardId));
        }
    }

    // ===== 후원 취소 =====
    private long insertFund(LoginUser user, long projectId, long rewardId, int quantity) {
        return tx.execute(conn -> TestData.insertFund(conn, user.userId(), projectId, rewardId, quantity, clock.now()));
    }

    private int canceledCountOf(LoginUser user) {
        return tx.execute(conn -> TestData.countCanceledFunds(conn, user.userId()));
    }

    @Test
    @DisplayName("취소하면 후원이 사라지고 취소 기록이 남는다")
    void cancelSuccess() {
        long projectId = insertProject();
        long rewardId = insertReward(projectId, 10);
        LoginUser sup = supporter("sup");
        long fundId = insertFund(sup, projectId, rewardId, 3);

        fundService().cancelFund(sup, fundId, "단순 변심");

        assertEquals(0, soldOf(rewardId));
        assertEquals(1, canceledCountOf(sup));
    }

    @Test
    @DisplayName("취소하면 남은 수량이 돌아와서 다른 사람이 살 수 있다")
    void cancelRestoresRemaining() {
        long projectId = insertProject();
        long rewardId = insertReward(projectId, 1);
        LoginUser sup1 = supporter("sup1");
        // 매진 상태
        long fundId = insertFund(sup1, projectId, rewardId, 1);

        fundService().cancelFund(sup1, fundId, "취소");
        fundService().applyFund(supporter("sup2"), rewardId, 1);

        assertEquals(1, soldOf(rewardId));
    }

    @Test
    @DisplayName("취소한 뒤 같은 프로젝트에 다시 후원할 수 있다")
    void reapplyAfterCancel() {
        long projectId = insertProject();
        long rewardId = insertReward(projectId, 10);
        LoginUser sup = supporter("sup");
        long fundId = insertFund(sup, projectId, rewardId, 1);

        fundService().cancelFund(sup, fundId, "취소");
        fundService().applyFund(sup, rewardId, 2);

        assertEquals(2, soldOf(rewardId));
    }

    @Test
    @DisplayName("남의 후원은 취소할 수 없다")
    void cancelOthersFund() {
        long projectId = insertProject();
        long rewardId = insertReward(projectId, 10);
        long fundId = insertFund(supporter("owner"), projectId, rewardId, 1);
        LoginUser other = supporter("other");

        assertThrows(PermissionDeniedException.class, () -> fundService().cancelFund(other, fundId, "취소"));
        assertEquals(1, soldOf(rewardId));
    }

    @Test
    @DisplayName("취소 사유가 비어 있으면 거부된다")
    void cancelWithoutReason() {
        long projectId = insertProject();
        long rewardId = insertReward(projectId, 10);
        LoginUser sup = supporter("sup");
        long fundId = insertFund(sup, projectId, rewardId, 1);

        assertThrows(BusinessException.class, () -> fundService().cancelFund(sup, fundId, " "));
        assertEquals(1, soldOf(rewardId));
    }

    @Test
    @DisplayName("마감된 프로젝트의 후원은 취소할 수 없다")
    void cancelAfterEnd() {
        long projectId = insertProject();
        long rewardId = insertReward(projectId, 10);
        LoginUser sup = supporter("sup");
        long fundId = insertFund(sup, projectId, rewardId, 1);

        clock.plus(Duration.ofDays(DURATION_DAYS));

        assertThrows(BusinessException.class, () -> fundService().cancelFund(sup, fundId, "취소"));
        assertEquals(1, soldOf(rewardId));
        assertEquals(0, canceledCountOf(sup));
    }

    @Test
    @DisplayName("존재하지 않는 후원은 취소할 수 없다")
    void cancelUnknownFund() {
        assertThrows(BusinessException.class,
                () -> fundService().cancelFund(supporter("sup"), 999999L, "취소"));
    }

    @Test
    @DisplayName("같은 후원을 동시에 5번 취소하면 1번만 성공하고 취소 기록도 1건")
    void concurrentCancel() {
        long projectId = insertProject();
        long rewardId = insertReward(projectId, 10);
        LoginUser sup = supporter("sup");
        long fundId = insertFund(sup, projectId, rewardId, 1);

        List<Throwable> results = Concurrent.runAtOnce(5,
                i -> fundService().cancelFund(sup, fundId, "취소"));

        assertEquals(1, Concurrent.successCount(results));
        assertEquals(0, Concurrent.countOf(results, DataAccessException.class));
        assertEquals(1, canceledCountOf(sup));
        assertEquals(0, soldOf(rewardId));
    }

    @Test
    @DisplayName("신청과 취소가 같은 리워드에 동시에 들어와도 데드락 없이 처리된다")
    void concurrentApplyAndCancel() {
        long projectId = insertProject();
        long rewardId = insertReward(projectId, 100);
        List<LoginUser> cancelers = new ArrayList<>();
        List<Long> fundIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            LoginUser u = supporter("cancel" + i);
            cancelers.add(u);
            fundIds.add(insertFund(u, projectId, rewardId, 1));
        }
        List<LoginUser> appliers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            appliers.add(supporter("apply" + i));
        }

        // 짝수 스레드는 취소, 홀수 스레드는 신청
        List<Throwable> results = Concurrent.runAtOnce(20, i -> {
            if (i % 2 == 0) {
                fundService().cancelFund(cancelers.get(i / 2), fundIds.get(i / 2), "취소");
            } else {
                fundService().applyFund(appliers.get(i / 2), rewardId, 1);
            }
        });

        assertEquals(20, Concurrent.successCount(results));
        assertEquals(0, Concurrent.countOf(results, DataAccessException.class));
        assertEquals(10, soldOf(rewardId));   // 10건 취소 + 10건 신청
    }

    // ===== 후원 내역 =====
    /**
    * 후원 내역 테스트용 공통 상황
    *  P1 진행 중: 목표 100,000 / 기간 30일 / 나 2개 + 다른2 1개
    *  P2 성공   : 목표  50,000 / 기간 10일 / 나 2개(20,000) + 다른1 3개(30,000) = 정확히 목표
    *  P3 실패   : 목표 100,000 / 기간 10일 / 나 1개(10,000)
    *  P4 취소   : 목표 100,000 / 기간 30일 / 나 1개 취소 (리워드 20,000원)
    *  P2의 내 후원에만 리뷰 있음
    */

    private record HistoryFixture(LoginUser me, long p1, long p2, long p3, long p4) {}

    private HistoryFixture setUpHistory() {
        long makerId = tx.execute(conn -> TestData.insertUser(conn, "maker", UserType.MAKER, clock.now()));
        LoginUser me = supporter("me");
        LoginUser other1 = supporter("other1");
        LoginUser other2 = supporter("other2");

        long p1 = tx.execute(conn -> TestData.insertApprovedProject(conn, makerId, 100000, 30, clock.now()));
        long p2 = tx.execute(conn -> TestData.insertApprovedProject(conn, makerId, 50000, 10, clock.now()));
        long p3 = tx.execute(conn -> TestData.insertApprovedProject(conn, makerId, 100000, 10, clock.now()));
        long p4 = tx.execute(conn -> TestData.insertApprovedProject(conn, makerId, 100000, 30, clock.now()));

        long r1 = insertReward(p1, UNLIMITED);
        long r2 = insertReward(p2, UNLIMITED);
        long r3 = insertReward(p3, UNLIMITED);
        long r4 = tx.execute(conn -> TestData.insertReward(conn, p4, 20000, UNLIMITED));

        // 날짜를 1시간씩 다르게 해서 정렬 순서를 확인할 수 있게 한다
        tx.execute(conn -> TestData.insertFund(conn, me.userId(), p1, r1, 2, clock.now().plusHours(1)));
        tx.execute(conn -> TestData.insertFund(conn, other2.userId(), p1, r1, 1, clock.now().plusHours(1)));
        long myP2Fund = tx.execute(conn -> TestData.insertFund(conn, me.userId(), p2, r2, 2, clock.now().plusHours(2)));
        tx.execute(conn -> TestData.insertFund(conn, other1.userId(), p2, r2, 3, clock.now().plusHours(2)));
        tx.execute(conn -> TestData.insertFund(conn, me.userId(), p3, r3, 1, clock.now().plusHours(3)));
        tx.execute(conn -> TestData.insertCanceledFund(conn, me.userId(), p4, r4, 1, "단순 변심", clock.now().plusHours(4)));
        tx.execute(conn -> {
            TestData.insertReview(conn, myP2Fund, 5, "좋아요", clock.now().plusDays(11));
            return null;
        });

        return new HistoryFixture(me, p1, p2, p3, p4);
    }

    private FundQueryDTO findByProject(List<FundQueryDTO> history, long projectId) {
        return history.stream()
                .filter(dto -> dto.getProjectId() == projectId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("내역에 프로젝트 " + projectId + "가 없습니다"));
    }

    @Test
    @DisplayName("마감 후 내역: 진행 중·성공·실패·취소가 올바르게 판정된다")
    void historyStatuses() {
        HistoryFixture f = setUpHistory();
        clock.plus(Duration.ofDays(10));   // P2, P3 마감 / P1, P4 진행 중

        List<FundQueryDTO> history = fundService().getMyFunds(f.me());

        assertEquals(4, history.size());   // 다른 사람 후원은 안 나옴

        FundQueryDTO ongoing = findByProject(history, f.p1());
        assertFalse(ongoing.isCanceled());
        assertEquals(ProjectFundingStatus.ONGOING, ongoing.getStatus());

        FundQueryDTO success = findByProject(history, f.p2());
        assertEquals(ProjectFundingStatus.SUCCESS, success.getStatus());   // 다른 사람 후원까지 합쳐 목표 달성
        assertTrue(success.isReviewed());

        FundQueryDTO failed = findByProject(history, f.p3());
        assertEquals(ProjectFundingStatus.FAILED, failed.getStatus());
        assertFalse(failed.isReviewed());

        FundQueryDTO canceled = findByProject(history, f.p4());
        assertTrue(canceled.isCanceled());
        assertNull(canceled.getStatus());
        assertEquals("단순 변심", canceled.getCancelReason());
    }

    @Test
    @DisplayName("마감 전에는 성공·실패가 아니라 모두 진행 중이다")
    void historyBeforeEnd() {
        HistoryFixture f = setUpHistory();

        List<FundQueryDTO> history = fundService().getMyFunds(f.me());

        assertEquals(ProjectFundingStatus.ONGOING, findByProject(history, f.p2()).getStatus());
        assertEquals(ProjectFundingStatus.ONGOING, findByProject(history, f.p3()).getStatus());
    }

    @Test
    @DisplayName("내역은 날짜 최신순이고, 취소는 취소일로 정렬된다")
    void historyOrder() {
        HistoryFixture f = setUpHistory();

        List<FundQueryDTO> history = fundService().getMyFunds(f.me());

        assertEquals(f.p4(), history.get(0).getProjectId());   // 취소 (+4시간)
        assertEquals(f.p3(), history.get(1).getProjectId());   // +3시간
        assertEquals(f.p2(), history.get(2).getProjectId());   // +2시간
        assertEquals(f.p1(), history.get(3).getProjectId());   // +1시간
    }

    @Test
    @DisplayName("금액은 리워드 가격 × 수량이다")
    void historyAmount() {
        HistoryFixture f = setUpHistory();

        List<FundQueryDTO> history = fundService().getMyFunds(f.me());

        assertEquals(20000, findByProject(history, f.p1()).getAmount());   // 10,000 × 2
        assertEquals(10000, findByProject(history, f.p3()).getAmount());   // 10,000 × 1
        assertEquals(20000, findByProject(history, f.p4()).getAmount());   // 20,000 × 1 (취소)
    }

    @Test
    @DisplayName("후원한 적 없으면 빈 목록이다")
    void historyEmpty() {
        assertTrue(fundService().getMyFunds(supporter("sup")).isEmpty());
    }

    @Test
    @DisplayName("메이커는 후원 내역을 볼 수 없다")
    void historyMakerRejected() {
        long makerId = tx.execute(conn -> TestData.insertUser(conn, "maker", UserType.MAKER, clock.now()));

        assertThrows(PermissionDeniedException.class,
                () -> fundService().getMyFunds(new LoginUser(makerId, UserType.MAKER)));
    }
}