package ypfunding.server.bootstrap;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import ypfunding.common.util.DateTimeUtil;
import ypfunding.server.config.AppProperties;
import ypfunding.server.exception.DataAccessException;
import ypfunding.server.persistence.TransactionManager;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;
import lombok.Getter;

public class AppContext implements AutoCloseable {
    @Getter
    private final Clock clock;
    private final BasicDataSource dataSource;
    @Getter
    private final TransactionManager transactionManager;
    @Getter
    private final SqlSessionFactory sqlSessionFactory;

    //DAO
    //private final ~DAO;

    //service
    //private final ~Service;

    public AppContext() {
        this(AppProperties.DEFAULT_PATH, Clock.system(DateTimeUtil.KST));
    }

    public AppContext(String propertiesPath, Clock clock) {
        Objects.requireNonNull(propertiesPath, "propertiesPath");

        this.clock = Objects.requireNonNull(clock, "clock");

        AppProperties props = AppProperties.load(propertiesPath); //설정 읽기
        this.dataSource = DataSourceFactory.create(props); //커넥션 풀 꺼내기

        try {
            this.transactionManager = new TransactionManager(dataSource); //관리자
            this.sqlSessionFactory = MyBatisConfig.create(dataSource); //MyBatis
            //this.~DAO = new ~DAO
            //this.~Service = new ~Service
        } catch (RuntimeException | Error e) {
            closeQuietly(e);
            throw e;
        }
    }

    //현재 사용중인 커넥션 수
    public int getActiveConnectionCount() {
        return dataSource.getNumActive();
    }

    //커넥션 닫기
    public void close() {
        try {
            dataSource.close();
        } catch (SQLException e) {
            throw new DataAccessException("커넥션 풀 닫기 실패", e);
        }
    }

    //생성 도중 실패 시 닫기
    public void closeQuietly(Throwable original) {
        try {
            dataSource.close();
        } catch (SQLException closeFailure) {
            original.addSuppressed(closeFailure);
        }
    }

}
