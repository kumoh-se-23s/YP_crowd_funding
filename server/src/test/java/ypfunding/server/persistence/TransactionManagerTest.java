package ypfunding.server.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ypfunding.common.type.UserType;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.exception.DataAccessException;
import ypfunding.server.support.Concurrent;
import ypfunding.server.support.DbTestBase;
import ypfunding.server.support.TestData;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TransactionManager가 실제 MySQL에서 설계대로 동작하는지 확인한다. (설계 9.2, 9.9)
 *
 * <p>모든 테스트 끝에 DbTestBase가 커넥션 누수(반납 안 된 커넥션)도 자동으로 검사한다.
 */
class TransactionManagerTest extends DbTestBase {

    @Test
    @DisplayName("정상 종료하면 commit: 트랜잭션 밖에서도 데이터가 보인다")
    void commitsOnSuccess() {
        tx.execute(conn -> TestData.insertUser(conn, "user1", UserType.SUPPORTER, clock.now()));

        assertEquals(1, countUsers());
    }

    @Test
    @DisplayName("결과 없는 작업(executeWithoutResult)도 정상 종료하면 commit")
    void executeWithoutResultCommits() {
        tx.executeWithoutResult(conn -> TestData.insertUser(conn, "user1", UserType.SUPPORTER, clock.now()));

        assertEquals(1, countUsers());
    }

    @Test
    @DisplayName("BusinessException이 나면 앞서 실행한 SQL이 모두 롤백되고, 같은 예외가 그대로 나온다")
    void rollsBackOnBusinessException() {
        BusinessException thrown = new BusinessException("재고가 부족합니다.");

        BusinessException caught = assertThrows(BusinessException.class, () -> tx.executeWithoutResult(conn -> {
            long makerId = TestData.insertUser(conn, "maker1", UserType.MAKER, clock.now());
            TestData.insertApprovedProject(conn, makerId, 100_000, 30, clock.now());
            throw thrown;
        }));

        assertSame(thrown, caught);                         // 다른 예외로 감싸지 않는다 (사용자 메시지 유지)
        assertEquals(0, countUsers());                      // 두 INSERT 모두 롤백
        assertEquals(0, countRows("projects"));
    }

    @Test
    @DisplayName("SQL 오류(SQLException)가 나면 롤백하고 DataAccessException으로 감싼다. 원래 에러 코드는 cause에 남는다")
    void rollsBackAndWrapsSqlException() {
        DataAccessException e = assertThrows(DataAccessException.class, () -> tx.executeWithoutResult(conn -> {
            TestData.insertUser(conn, "same-id", UserType.SUPPORTER, clock.now());
            TestData.insertUser(conn, "same-id", UserType.SUPPORTER, clock.now());   // login_id 중복 → 1062
        }));

        SQLException cause = assertInstanceOf(SQLException.class, e.getCause());
        assertEquals(1062, cause.getErrorCode());
        assertEquals(0, countUsers());                      // 첫 번째 INSERT도 롤백
    }

    @Test
    @DisplayName("트랜잭션 안에서 tx.execute를 다시 부르면 예외, 바깥 트랜잭션도 롤백된다")
    void rejectsNestedTransaction() {
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(conn -> {
            TestData.insertUser(conn, "outer", UserType.SUPPORTER, clock.now());
            tx.execute(inner -> 1);                         // 중첩 호출
        }));

        assertEquals(0, countUsers());
    }

    @Test
    @DisplayName("중첩 호출로 실패한 뒤에도 같은 스레드에서 트랜잭션을 정상적으로 쓸 수 있다")
    void usableAfterNestedFailure() {
        assertThrows(IllegalStateException.class, () -> tx.execute(conn -> tx.execute(inner -> 1)));

        int result = tx.execute(conn -> 7);
        assertEquals(7, result);
    }

    @Test
    @DisplayName("다른 스레드의 트랜잭션은 중첩이 아니다 (클라이언트마다 스레드가 다름)")
    void otherThreadIsNotNested() {
        tx.executeWithoutResult(conn -> {
            List<Throwable> results = Concurrent.runAtOnce(1, i -> tx.execute(c -> 1));
            assertNull(results.get(0));
        });
    }

    @Test
    @DisplayName("실패한 트랜잭션도 커넥션을 반납한다")
    void returnsConnectionAfterFailure() {
        assertThrows(BusinessException.class, () -> tx.execute(conn -> {
            throw new BusinessException("실패");
        }));

        assertEquals(0, context.getActiveConnectionCount());
    }

    private int countUsers() {
        return countRows("users");
    }

    private int countRows(String table) {
        return tx.execute(conn -> {
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM " + table)) {
                rs.next();
                return rs.getInt(1);
            }
        });
    }
}