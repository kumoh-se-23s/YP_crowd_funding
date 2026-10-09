package ypfunding.server.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ypfunding.common.type.UserType;

import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 테스트 인프라(TestSchema, TestData, TestClock, Concurrent, DbTestBase) 자체가 제대로 동작하는지 확인한다.
 */
class TestInfraTest extends DbTestBase {

    @Test
    @DisplayName("schema.sql의 테이블 10개가 모두 만들어진다")
    void createsAllTables() {
        int count = tx.execute(conn -> {
            try (ResultSet rs = conn.createStatement().executeQuery("""
                    SELECT COUNT(*) FROM information_schema.tables
                     WHERE table_schema = DATABASE()
                       AND table_name IN ('users', 'projects', 'category', 'rewards', 'image',
                                          'reject', 'likes', 'funds', 'canceled_funds', 'reviews')
                    """)) {
                rs.next();
                return rs.getInt(1);
            }
        });
        assertEquals(10, count);
    }

    @Test
    @DisplayName("TestData로 넣은 데이터는 다음 테스트 전에 지워진다 (1/2: 넣기)")
    void insertsData() {
        insertSample();
        assertEquals(1, countUsers());
    }

    @Test
    @DisplayName("TestData로 넣은 데이터는 다음 테스트 전에 지워진다 (2/2: 다시 넣어도 1명)")
    void dataIsClearedBetweenTests() {
        insertSample();            // 이전 테스트의 데이터가 남아 있으면 login_id 중복(1062)으로 실패한다
        assertEquals(1, countUsers());
    }

    @Test
    @DisplayName("무제한 리워드는 stock이 NULL로 저장된다")
    void unlimitedRewardIsNull() {
        long rewardId = insertSample();
        Integer stock = tx.execute(conn -> {
            try (ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT stock FROM rewards WHERE reward_id = " + rewardId)) {
                rs.next();
                return rs.getObject("stock", Integer.class);
            }
        });
        assertNull(stock);
    }

    @Test
    @DisplayName("TestClock: 시작 시각이 BASE_TIME이고, 옮길 수 있다")
    void clockCanMove() {
        assertEquals(BASE_TIME, clock.now());
        assertEquals(BASE_TIME, LocalDateTime.now(context.getClock()));   // AppContext도 같은 시계를 쓴다

        clock.plus(Duration.ofDays(31));
        assertEquals(BASE_TIME.plusDays(31), LocalDateTime.now(context.getClock()));
    }

    @Test
    @DisplayName("Concurrent: 모든 스레드가 실행되고, 실패한 스레드의 예외가 그 자리에 담긴다")
    void concurrentRunsAll() {
        AtomicInteger ran = new AtomicInteger();
        List<Throwable> results = Concurrent.runAtOnce(20, i -> {
            ran.incrementAndGet();
            if (i % 2 == 1) {
                throw new IllegalStateException("홀수 실패");
            }
        });

        assertEquals(20, ran.get());
        assertEquals(10, Concurrent.successCount(results));
        assertEquals(10, Concurrent.countOf(results, IllegalStateException.class));
        assertTrue(results.get(1) instanceof IllegalStateException);
    }

    @Test
    @DisplayName("Concurrent: 20개 스레드가 동시에 DB를 써도 커넥션이 모두 반납된다")
    void concurrentDbAccessReturnsConnections() {
        List<Throwable> results = Concurrent.runAtOnce(20, i ->
                tx.execute(conn -> TestData.insertUser(conn, "user" + i, UserType.SUPPORTER, clock.now())));

        assertEquals(20, Concurrent.successCount(results));
        assertEquals(20, countUsers());
        // 커넥션 반납 여부는 DbTestBase의 @AfterEach가 검사한다
    }

    /** 메이커 1명, 진행 중 프로젝트 1개, 무제한 리워드 1개를 넣고 리워드 id를 돌려준다 */
    private long insertSample() {
        return tx.execute(conn -> {
            long makerId = TestData.insertUser(conn, "maker1", UserType.MAKER, clock.now());
            long projectId = TestData.insertApprovedProject(conn, makerId, 100_000, 30, clock.now());
            return TestData.insertReward(conn, projectId, 10_000, null);
        });
    }

    private int countUsers() {
        return tx.execute(conn -> {
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM users")) {
                rs.next();
                return rs.getInt(1);
            }
        });
    }
}