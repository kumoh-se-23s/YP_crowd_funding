package ypfunding.server.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//PasswordHasher 단위 테스트 (DB 없음)
class PasswordHasherTest {

    private static final String SALT = "0123456789abcdef0123456789abcdef";

    @Test
    @DisplayName("해시는 MySQL SHA2(CONCAT(salt, pw), 256)와 같은 값이다")
    void hashEqualsMySqlSha2() {
        //기대값 = MySQL에서 SELECT SHA2(CONCAT('0123456789abcdef0123456789abcdef', 'abc123'), 256); 실행 결과
        String expected = "ba47e688148b9b9abb54977eca77ced8aa811d1da511e97f7ce3c63ea2d34b03";
        assertEquals(expected, PasswordHasher.hash(SALT, "abc123"));
    }

    @Test
    @DisplayName("salt는 소문자 hex 32자이고 매번 다르다")
    void saltIsLowercaseHex32AndRandom() {
        String salt1 = PasswordHasher.newSalt();
        String salt2 = PasswordHasher.newSalt();

        assertTrue(salt1.matches("[0-9a-f]{32}"), salt1);
        assertNotEquals(salt1, salt2);
    }

    @Test
    @DisplayName("같은 비밀번호라도 salt가 다르면 해시가 다르다")
    void differentSaltGivesDifferentHash() {
        String hash1 = PasswordHasher.hash(PasswordHasher.newSalt(), "abc123");
        String hash2 = PasswordHasher.hash(PasswordHasher.newSalt(), "abc123");

        assertTrue(hash1.matches("[0-9a-f]{64}"), hash1);
        assertNotEquals(hash1, hash2);
    }

    @Test
    @DisplayName("비밀번호가 맞을 때만 matches가 true다")
    void matchesOnlyWithCorrectPassword() {
        String hash = PasswordHasher.hash(SALT, "abc123");

        assertTrue(PasswordHasher.matches(SALT, "abc123", hash));
        assertFalse(PasswordHasher.matches(SALT, "abc124", hash));
        assertFalse(PasswordHasher.matches(SALT, "", hash));
    }
}