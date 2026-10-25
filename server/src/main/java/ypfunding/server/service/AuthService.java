package ypfunding.server.service;

import ypfunding.server.auth.LoginUser;
import ypfunding.server.auth.PasswordHasher;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.UserManagedDAO;
import ypfunding.server.persistence.dto.UserLoginDTO;

import java.time.Clock;
import java.util.Objects;

//비밀번호, 해시, salt를 다루는 service
public class AuthService extends Service{
    public static final String LOGIN_FAILED_MESSAGE = "아이디 또는 비밀번호가 올바르지 않음";

    private final UserManagedDAO userManagedDAO;
    private final Clock clock;

    public AuthService(TransactionManager transactionManager, UserManagedDAO userManagedDAO, Clock clock) {
        super(transactionManager);
        this.userManagedDAO = Objects.requireNonNull(userManagedDAO, "userManagedDAO");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    //로그인
    public LoginUser login(String loginId, String password) {
        if(loginId == null || password == null) {
            throw new BusinessException(LOGIN_FAILED_MESSAGE);
        }

        UserLoginDTO loginInfo = transactionManager.execute(conn -> userManagedDAO.getByLoginId(conn, loginId));

        //해시 비교
        if(loginInfo == null || !PasswordHasher.matches(loginInfo.getSalt(), password, loginInfo.getPasswordHash())) {
            throw new BusinessException(LOGIN_FAILED_MESSAGE);
        }
        return new LoginUser(loginInfo.getUserId(), loginInfo.getUserType());
    }

    //서포터 회원가입

    //비번 변경
}
