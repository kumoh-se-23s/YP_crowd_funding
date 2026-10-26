package ypfunding.server.service;

import ypfunding.common.constant.DomainRules;
import ypfunding.common.dto.UserDTO;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.auth.PasswordHasher;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.UserManagedDAO;
import ypfunding.server.persistence.dto.UserLoginDTO;
import ypfunding.server.util.InputValidator;

import java.time.Clock;
import java.time.LocalDateTime;
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
    public long signup(String loginId, String password, String name, String address) {
        //입력 검증
        InputValidator.requireText(loginId, "아이디", DomainRules.MAX_LOGIN_ID_LENGTH);
        InputValidator.requireNotBlank(password, "비밀번호");
        InputValidator.requireText(name, "이름", DomainRules.MAX_USER_NAME_LENGTH);
        InputValidator.requireText(address, "주소", DomainRules.MAX_ADDRESS_LENGTH);

        //시간 정의
        LocalDateTime now = LocalDateTime.now(clock);

        //해싱
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash(salt, password);

        //생성
        UserDTO user = new UserDTO();
        user.setLoginId(loginId);
        user.setName(name);
        user.setAddress(address);
        user.setUserType(UserType.SUPPORTER);
        user.setRegDate(now);

        //insert
        return transactionManager.execute(conn -> userManagedDAO.insert(conn, user, hash, salt));
    }

    //비번 변경
    public void changePassword(LoginUser user, String currentPassword, String newPassword) {
        //입력 검증
        Objects.requireNonNull(user, "user");
        InputValidator.requireNotBlank(currentPassword, "현재 비밀번호");
        InputValidator.requireNotBlank(newPassword, "새 비밀번호");

        //요청 유저 조회
        UserLoginDTO loginInfo = transactionManager.execute(conn ->
                userManagedDAO.getById(conn, user.userId()));
        if (loginInfo == null) {
            throw new BusinessException("사용자를 찾을 수 없습니다.");
        }

        //비번 확인
        if (!PasswordHasher.matches(loginInfo.getSalt(), currentPassword, loginInfo.getPasswordHash())) {
            throw new BusinessException("현재 비밀번호가 올바르지 않습니다.");
        }

        //새 해싱
        String newSalt = PasswordHasher.newSalt();
        String newHash = PasswordHasher.hash(newSalt, newPassword);

        //update, 0건이면 그 사이에 어디서 건드린거
        int updated = transactionManager.execute(conn ->
                userManagedDAO.updatePassword(conn, user.userId(), loginInfo.getPasswordHash(), newHash, newSalt));
        if (updated == 0) {
            throw new BusinessException("비밀번호가 다른 곳에서 변경되었습니다.");
        }
    }
}
