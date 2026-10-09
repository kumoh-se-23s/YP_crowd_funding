package ypfunding.server.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ypfunding.common.type.UserType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * LoginUser가 잘못된 값으로 만들어지지 않는지 확인한다. (DB 없이 실행)
 */
class LoginUserTest {

    @Test
    @DisplayName("정상 값이면 그대로 담긴다")
    void createsWithValidValues() {
        LoginUser user = new LoginUser(7, UserType.SUPPORTER);

        assertEquals(7, user.userId());
        assertEquals(UserType.SUPPORTER, user.userType());
    }

    @Test
    @DisplayName("userId가 0 이하이면 예외")
    void rejectsNonPositiveId() {
        assertThrows(IllegalArgumentException.class, () -> new LoginUser(0, UserType.MAKER));
        assertThrows(IllegalArgumentException.class, () -> new LoginUser(-1, UserType.MAKER));
    }

    @Test
    @DisplayName("userType이 null이면 예외")
    void rejectsNullType() {
        assertThrows(NullPointerException.class, () -> new LoginUser(1, null));
    }
}