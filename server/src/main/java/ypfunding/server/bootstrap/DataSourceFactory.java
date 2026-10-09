package ypfunding.server.bootstrap;

import org.apache.commons.dbcp2.BasicDataSource;
import ypfunding.server.config.AppProperties;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

//커넥션 풀 제작소
public class DataSourceFactory {
    //TODO: 다 상수분리해야겠다
    static final int MAX_TOTAL = 10; //동시에 빌려줄 수 있는 커넥션 수
    static final Duration MAX_WAIT = Duration.ofSeconds(3); //모든 커넥션이 사용중일 때 최대 대기시간, 넘으면 SQLException
    static final String INIT_SQL = "SET time_zone = '+09:00'";

    public static BasicDataSource create (AppProperties props) {
        Objects.requireNonNull(props, "props");

        BasicDataSource ds = new BasicDataSource();

        //접속 정보
        ds.setUrl(props.getDbUrl());
        ds.setUsername(props.getDbUsername());
        ds.setPassword(props.getDbPassword());

        //풀 크기: 실행 시 10개 만들어놓고 그거만 사용
        ds.setMaxTotal(MAX_TOTAL);
        ds.setMaxIdle(MAX_TOTAL);
        ds.setInitialSize(MAX_TOTAL);
        ds.setMinIdle(MAX_TOTAL);

        //대기 시간
        ds.setMaxWait(MAX_WAIT);

        //새 커넥션 초기화
        ds.setConnectionInitSqls(List.of(INIT_SQL));

        verifyConnection(ds, props);
        return ds;
    }

    //커넥션 살아있는지 테스트
    private static void verifyConnection(BasicDataSource ds, AppProperties props) {
        try (Connection ignored = ds.getConnection()) {
            //예외 안났으면 성공이니까 바로 넘어감
        } catch (SQLException e) {
            close(ds, e);
            throw new IllegalStateException("db 접속 불가: " + props.getDbUrl() + ", 계정: " + props.getDbUsername(), e);
        }
    }

    //닫기
    private static void close(BasicDataSource ds, Throwable original) {
        try {
            ds.close();
        } catch (SQLException closeFailure) {
            original.addSuppressed(closeFailure);
        }
    }
}
