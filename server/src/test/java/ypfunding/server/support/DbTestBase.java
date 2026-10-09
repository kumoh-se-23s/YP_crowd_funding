package ypfunding.server.support;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import ypfunding.server.bootstrap.AppContext;
import ypfunding.server.persistence.TransactionManager;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DB를 쓰는 테스트의 부모 클래스. Service 테스트는 이 클래스를 상속한다.
 *
 * <p>[자동으로 해 주는 일]
 * <ul>
 *   <li>테스트 클래스 시작 시: 테스트용 AppContext 생성 (crowdfunding_test + {@link TestClock}),
 *       테이블이 아직 없으면 schema.sql로 생성</li>
 *   <li>각 테스트 시작 전: 모든 테이블 비우기, 시계를 {@link #BASE_TIME}으로 되돌리기</li>
 *   <li>각 테스트 끝난 후: 빌려 간 커넥션이 모두 반납되었는지 검사 (커넥션 누수 검사)</li>
 *   <li>테스트 클래스 끝날 때: AppContext 닫기 (커넥션 풀 종료)</li>
 * </ul>
 *
 * <p>[쓰는 법]
 * <pre>
 *   class FundServiceTest extends DbTestBase {
 *       &#64;Test
 *       void 재고_1개에_20명이_동시_신청하면_1명만_성공한다() {
 *           long makerId = tx.execute(conn -&gt; TestData.insertUser(conn, "maker", UserType.MAKER, clock.now()));
 *           ...
 *           FundService fundService = context.getFundService();
 *           ...
 *       }
 *   }
 * </pre>
 *
 * <p>[주의] 테스트는 한 번에 하나씩 실행되어야 한다. (모든 테스트가 같은 테이블을 쓰므로)
 * JUnit 병렬 실행 설정을 켜지 않는다.
 */
public abstract class DbTestBase {

    /** 테스트용 설정 파일 (클래스패스 기준) */
    protected static final String TEST_PROPERTIES = "config/application-test.properties";

    /** 매 테스트가 시작될 때의 현재 시각 (한국 시각). 값 자체에 의미는 없고, 고정되어 있다는 것이 중요하다 */
    protected static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 12, 7, 10, 0);

    /** 테스트용 시계. 테스트 안에서 clock.set(...) / clock.plus(...)로 현재 시각을 바꿀 수 있다 */
    protected static TestClock clock;

    /** 테스트용 AppContext. Service는 context.getXxxService()로 꺼낸다 */
    protected static AppContext context;

    /** 데이터 준비·확인용 트랜잭션 관리자 (context.getTransactionManager()와 같은 객체) */
    protected static TransactionManager tx;

    @BeforeAll
    static void openContext() {
        clock = new TestClock(BASE_TIME);
        context = new AppContext(TEST_PROPERTIES, clock);
        tx = context.getTransactionManager();
        TestSchema.createOnce(tx);
    }

    @AfterAll
    static void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @BeforeEach
    void resetState() {
        TestSchema.clearAll(tx);
        clock.set(BASE_TIME);
    }

    @AfterEach
    void assertNoConnectionLeak() {
        assertEquals(0, context.getActiveConnectionCount(),
                "테스트가 끝났는데 반납되지 않은 커넥션이 있습니다. (커넥션 누수)");
    }
}