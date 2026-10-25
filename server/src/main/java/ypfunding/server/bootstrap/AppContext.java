package ypfunding.server.bootstrap;

import org.apache.commons.dbcp2.BasicDataSource;
import ypfunding.common.util.DateTimeUtil;
import ypfunding.server.config.AppProperties;
import ypfunding.server.exception.DataAccessException;
import ypfunding.server.persistence.TransactionManager;

import java.sql.SQLException;
import java.time.Clock;
import java.util.Objects;
import lombok.Getter;
import ypfunding.server.persistence.dao.*;
import ypfunding.server.service.*;

public class AppContext implements AutoCloseable {
    @Getter
    private final Clock clock;
    private final BasicDataSource dataSource;
    @Getter
    private final TransactionManager transactionManager;

    //DAO
    //private final ~DAO;
    private final UserManagedDAO managedDAO;
    private final UsersDAO usersDAO;
    private final LikesDAO likesDAO;
    private final ProjectsDAO projectsDAO;
    private final RejectDAO rejectsDAO;

    //service
    //private final ~Service;
    @Getter
    private final UserManagedService managedService;
    @Getter
    private final UserService userService;
    @Getter
    private final AdminService adminService;
    @Getter
    private final MakerService makerService;
    @Getter
    private final SupporterService supporterService;
    @Getter
    private final LikeService likeService;

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
            //this.*DAO = new *DAO
            //this.*Service = new *Service
            this.managedDAO = new UserManagedDAO();
            this.usersDAO = new UsersDAO();
            this.likesDAO = new LikesDAO();
            this.projectsDAO = new ProjectsDAO();
            this.rejectsDAO = new RejectDAO();
            this.managedService = new UserManagedService(transactionManager, managedDAO);
            this.userService = new UserService(transactionManager, usersDAO);
            this.adminService = new AdminService(transactionManager, usersDAO, projectsDAO, rejectsDAO);
            this.makerService = new MakerService(transactionManager, usersDAO);
            this.supporterService = new SupporterService(transactionManager, usersDAO);
            this.likeService = new LikeService(transactionManager, likesDAO, projectsDAO, clock);
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
    private void closeQuietly(Throwable original) {
        try {
            dataSource.close();
        } catch (SQLException closeFailure) {
            original.addSuppressed(closeFailure);
        }
    }

}
