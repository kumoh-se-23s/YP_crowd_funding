package ypfunding.server.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.auth.PasswordHasher;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.exception.DuplicateLoginIdException;
import ypfunding.server.persistence.dao.UserManagedDAO;
import ypfunding.server.support.DbTestBase;
import ypfunding.server.support.TestData;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

//로그인·회원가입·비밀번호 변경 (평가표 20 로그인, 10 개인정보 수정, 서포터 회원가입)
class AuthServiceTest extends DbTestBase {

    // ---------- 회원가입 ----------

    @Test
    @DisplayName("회원가입하면 서포터로 저장되고, 비밀번호는 해시로 저장된다")
    void signupStoresSupporterWithHashedPassword() {
        AuthService auth = context.getAuthService();

        long userId = auth.signup("newbie", "abc123", "홍길동", "서울시");

        StoredUser stored = loadUser(userId);
        assertEquals("newbie", stored.loginId);
        assertEquals("SUPPORTER", stored.userType);
        assertEquals(BASE_TIME, stored.regdate);                       // 주입한 시계의 시각
        assertNotEquals("abc123", stored.passwordHash);                // 평문 저장 금지
        assertEquals(PasswordHasher.hash(stored.salt, "abc123"), stored.passwordHash);
    }

    @Test
    @DisplayName("이미 있는 아이디로 가입하면 DuplicateLoginIdException")
    void signupWithDuplicateLoginIdFails() {
        tx.execute(conn -> TestData.insertUser(conn, "dup", UserType.SUPPORTER, clock.now()));
        AuthService auth = context.getAuthService();

        assertThrows(DuplicateLoginIdException.class,
                () -> auth.signup("dup", "abc123", "홍길동", "서울시"));
        assertEquals(1, countUsers());
    }

    @Test
    @DisplayName("빈 값이나 너무 긴 값으로는 가입할 수 없다")
    void signupRejectsBlankOrTooLongInput() {
        AuthService auth = context.getAuthService();

        assertThrows(BusinessException.class, () -> auth.signup(" ", "abc123", "홍길동", "서울시"));
        assertThrows(BusinessException.class, () -> auth.signup("a".repeat(31), "abc123", "홍길동", "서울시"));
        assertThrows(BusinessException.class, () -> auth.signup("newbie", "", "홍길동", "서울시"));
        assertThrows(BusinessException.class, () -> auth.signup("newbie", "abc123", null, "서울시"));
        assertEquals(0, countUsers());
    }

    // ---------- 로그인 ----------

    @Test
    @DisplayName("가입한 아이디와 비밀번호로 로그인하면 LoginUser를 받는다")
    void loginReturnsLoginUser() {
        AuthService auth = context.getAuthService();
        long userId = auth.signup("newbie", "abc123", "홍길동", "서울시");

        LoginUser user = auth.login("newbie", "abc123");

        assertEquals(userId, user.userId());
        assertEquals(UserType.SUPPORTER, user.userType());
    }

    @Test
    @DisplayName("seed.sql 방식(MySQL SHA2)으로 만든 계정도 로그인되고 역할이 맞다")
    void loginWorksForSeedSqlStyleAccount() {
        // seed.sql처럼 MySQL SHA2로 해시한 메이커
        long makerId = tx.execute(conn ->
                TestData.insertUserWithPassword(conn, "maker1", "maker1234", UserType.MAKER, clock.now()));

        LoginUser user = context.getAuthService().login("maker1", "maker1234");

        assertEquals(makerId, user.userId());
        assertEquals(UserType.MAKER, user.userType());
    }

    @Test
    @DisplayName("비밀번호가 틀리거나 없는 아이디면 같은 메시지로 실패한다")
    void loginFailureUsesSameMessage() {
        AuthService auth = context.getAuthService();
        auth.signup("newbie", "abc123", "홍길동", "서울시");

        BusinessException wrongPassword = assertThrows(BusinessException.class,
                () -> auth.login("newbie", "wrong"));
        BusinessException unknownId = assertThrows(BusinessException.class,
                () -> auth.login("nobody", "abc123"));

        assertEquals(AuthService.LOGIN_FAILED_MESSAGE, wrongPassword.getMessage());
        assertEquals(AuthService.LOGIN_FAILED_MESSAGE, unknownId.getMessage());
    }

    // ---------- 비밀번호 변경 ----------

    @Test
    @DisplayName("비밀번호를 바꾸면 새 비밀번호로만 로그인된다")
    void changePasswordAllowsOnlyNewPassword() {
        AuthService auth = context.getAuthService();
        auth.signup("newbie", "abc123", "홍길동", "서울시");
        LoginUser user = auth.login("newbie", "abc123");

        auth.changePassword(user, "abc123", "new5678");

        assertEquals(user, auth.login("newbie", "new5678"));
        assertThrows(BusinessException.class, () -> auth.login("newbie", "abc123"));
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 바뀌지 않는다")
    void changePasswordFailsWithWrongCurrentPassword() {
        AuthService auth = context.getAuthService();
        long userId = auth.signup("newbie", "abc123", "홍길동", "서울시");
        LoginUser user = auth.login("newbie", "abc123");
        String hashBefore = loadUser(userId).passwordHash;

        assertThrows(BusinessException.class, () -> auth.changePassword(user, "wrong", "new5678"));

        assertEquals(hashBefore, loadUser(userId).passwordHash);
    }

    @Test
    @DisplayName("확인과 변경 사이에 비밀번호가 바뀌었으면 덮어쓰지 않는다 (0행)")
    void updatePasswordSkipsWhenHashChanged() {
        AuthService auth = context.getAuthService();
        long userId = auth.signup("newbie", "abc123", "홍길동", "서울시");
        LoginUser user = auth.login("newbie", "abc123");
        String hashBefore = loadUser(userId).passwordHash;

        // 확인 때 읽은 해시가 이미 낡았다고 가정: oldHash 조건이 맞지 않으면 0행
        int updated = tx.execute(conn -> new UserManagedDAO()
                .updatePassword(conn, user.userId(), "x".repeat(64), "y".repeat(64), "z".repeat(32)));

        assertEquals(0, updated);
        assertEquals(hashBefore, loadUser(userId).passwordHash);
    }

    // ---------- 확인용 도우미 ----------

    /** DB에 저장된 값 그대로 (UserDTO에는 비밀번호가 없으므로 직접 읽는다) */
    private record StoredUser(String loginId, String passwordHash, String salt,
                              String userType, LocalDateTime regdate) {
    }

    private StoredUser loadUser(long userId) {
        return tx.execute(conn -> {
            String sql = "SELECT login_id, password, salt, usertype, regdate FROM users WHERE user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next(), "사용자가 없음: " + userId);
                    return new StoredUser(rs.getString("login_id"), rs.getString("password"),
                            rs.getString("salt"), rs.getString("usertype"),
                            rs.getObject("regdate", LocalDateTime.class));
                }
            }
        });
    }

    private int countUsers() {
        return tx.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        });
    }
}