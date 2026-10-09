package ypfunding.server.persistence;

import ypfunding.server.exception.DataAccessException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public class TransactionManager {
    //커넥션 풀
    private final DataSource dataSource;

    //중첩호출 감지용 스레드별 트랜젝션 감지
    private final ThreadLocal<Boolean> inTransaction = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public TransactionManager(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    //작업을 받아서 트랜잭션 하나로 실행, 결과 돌려줌
    public <T> T execute(SqlWork<T> work) {
        Objects.requireNonNull(work, "work");

        //중첩 트랜잭션 검사
        if(inTransaction.get()) {
            throw new IllegalStateException("중첩 트랜잭션 감지");
        }
        inTransaction.set(Boolean.TRUE);

        try (Connection conn = dataSource.getConnection()) { //풀에서 커넥션 들고 오기, 끝나면 반납
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            //트랜잭션 내부 동작
            try {
                T result = work.run(conn);
                conn.commit(); //정상 종료시 커밋
                return result;
            } catch (SQLException e) { //db오류
                rollback(conn, e);
                throw new DataAccessException("트랜잭션 처리 중 db오류 발생", e);
            } catch (RuntimeException | Error e) { //businessException 등
                rollback(conn, e);
                throw e;
            } finally {
                restoreAutoCommit(conn, originalAutoCommit);
            }
        } catch (SQLException e) { //접속 실패
            throw new DataAccessException("db 커넥션 연결 실패", e);
        } finally {
            inTransaction.remove();
        }
    }

    //리턴 없는 작업
    public void executeWithoutResult(SqlVoidWork work) {
        Objects.requireNonNull(work, "work");
        execute(conn ->  {
            work.run(conn);
            return null;
        });
    }

    //롤백 (실패 시 원래 예외에 붙임)
    private static void rollback(Connection conn, Throwable original) {
        try {
            conn.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    //자동 커밋 설정 복구: 원본 상태 그대로 반납
    private static void restoreAutoCommit(Connection conn, boolean originalAutoCommit) {
        try {
            conn.setAutoCommit(originalAutoCommit);
        } catch (SQLException ignored) {} //여기서 올리면 얘가 진짜 문제를 먹어버리므로 무시
    }
}
