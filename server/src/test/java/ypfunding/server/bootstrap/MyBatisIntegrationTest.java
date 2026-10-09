package ypfunding.server.bootstrap;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ypfunding.common.type.UserType;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.support.DbTestBase;
import ypfunding.server.support.TestData;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MyBatis가 TransactionManager의 커넥션 위에서 설계대로 동작하는지 확인한다. (설계 10장 MyBatisConfig)
 *
 * <p>ProjectQueryDAO가 쓸 방식 그대로 {@code factory.openSession(conn)}으로 세션을 연다.
 */
class MyBatisIntegrationTest extends DbTestBase {

    private static SqlSessionFactory factory;

    /** 부모(DbTestBase)의 @BeforeAll이 AppContext를 만든 뒤에 실행된다 */
    @BeforeAll
    static void registerTestMapper() {
        factory = context.getSqlSessionFactory();
        factory.getConfiguration().addMapper(TestUserMapper.class);
    }

    @Test
    @DisplayName("같은 트랜잭션 안에서 JDBC로 넣은 (아직 커밋 안 된) 데이터를 MyBatis가 본다")
    void seesUncommittedDataInSameTransaction() {
        tx.executeWithoutResult(conn -> {
            long userId = TestData.insertUser(conn, "mb1", UserType.SUPPORTER, clock.now());

            try (SqlSession session = factory.openSession(conn)) {
                assertEquals("mb1", session.getMapper(TestUserMapper.class).findLoginId(userId));
            }
        });
    }

    @Test
    @DisplayName("MyBatis 세션을 닫아도 커넥션은 닫히지 않고, 같은 트랜잭션에서 계속 쓸 수 있다 (closeConnection=false)")
    void sessionCloseKeepsConnection() {
        tx.executeWithoutResult(conn -> {
            try (SqlSession session = factory.openSession(conn)) {
                session.getMapper(TestUserMapper.class).countUsers();
            }

            assertFalse(conn.isClosed(), "세션을 닫았더니 커넥션까지 닫혔습니다. closeConnection=false 설정을 확인하세요.");
            TestData.insertUser(conn, "after-close", UserType.SUPPORTER, clock.now());   // 계속 사용 가능
        });

        assertEquals(1, countUsersWithMyBatis());
    }

    @Test
    @DisplayName("MyBatis로 바꾼 데이터도 TransactionManager가 롤백한다")
    void rollbackIsControlledByTransactionManager() {
        long userId = tx.execute(conn -> TestData.insertUser(conn, "mb3", UserType.SUPPORTER, clock.now()));

        assertThrows(BusinessException.class, () -> tx.executeWithoutResult(conn -> {
            try (SqlSession session = factory.openSession(conn)) {
                assertEquals(1, session.getMapper(TestUserMapper.class).rename(userId, "바뀐 이름"));
            }
            throw new BusinessException("롤백 확인");
        }));

        String name = tx.execute(conn -> {
            try (SqlSession session = factory.openSession(conn)) {
                return session.getMapper(TestUserMapper.class).findName(userId);
            }
        });
        assertEquals("mb3 이름", name);   // TestData.insertUser가 넣은 원래 이름
    }

    @Test
    @DisplayName("snake_case 컬럼 → camelCase 필드, ENUM → enum, DATETIME → LocalDateTime 으로 변환된다")
    void mapsColumnsAndTypes() {
        LocalDateTime regdate = LocalDateTime.of(2026, 12, 7, 10, 30, 15);
        long userId = tx.execute(conn -> TestData.insertUser(conn, "mb4", UserType.MAKER, regdate));

        TestUserMapper.UserRow row = tx.execute(conn -> {
            try (SqlSession session = factory.openSession(conn)) {
                return session.getMapper(TestUserMapper.class).findRow(userId);
            }
        });

        assertEquals(userId, row.getUserId());
        assertEquals("mb4", row.getLoginId());
        assertEquals(UserType.MAKER, row.getUsertype());
        assertEquals(regdate, row.getRegdate());
    }

    private int countUsersWithMyBatis() {
        return tx.execute(conn -> {
            try (SqlSession session = factory.openSession(conn)) {
                return session.getMapper(TestUserMapper.class).countUsers();
            }
        });
    }
}