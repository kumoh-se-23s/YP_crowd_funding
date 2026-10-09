package ypfunding.server.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
AppProperties가 설정 파일을 올바르게 읽고, 잘못된 설정을 바로 알려주는지 확인
DB 접속 없이 파일 읽기만 검사
테스트용 설정 파일은 server/src/test/resources/config/test-props/ 아래
 */
class AppPropertiesTest {

    @Test
    @DisplayName("정상 파일: 세 값을 모두 읽고, 앞뒤 공백을 잘라낸다")
    void loadsValuesAndTrims() {
        AppProperties props = AppProperties.load("config/test-props/valid.properties");

        assertEquals("jdbc:mysql://localhost:3306/some_db", props.getDbUrl());
        assertEquals("tester", props.getDbUsername());
        assertEquals("pw 1234", props.getDbPassword());   // 앞뒤 공백만 제거, 중간 공백은 유지
    }

    @Test
    @DisplayName("한글 값도 깨지지 않고 읽는다 (UTF-8로 읽는지 확인)")
    void readsUtf8() {
        AppProperties props = AppProperties.load("config/test-props/korean.properties");

        assertEquals("한글계정", props.getDbUsername());
    }

    @Test
    @DisplayName("비밀번호 값이 비어 있어도 된다 (비밀번호 없는 로컬 계정)")
    void allowsEmptyPassword() {
        AppProperties props = AppProperties.load("config/test-props/empty-password.properties");

        assertEquals("", props.getDbPassword());
    }

    @Test
    @DisplayName("파일이 없으면 즉시 예외, 메시지에 해결 방법이 있다")
    void failsWhenFileMissing() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> AppProperties.load("config/test-props/no-such-file.properties"));

        assertTrue(e.getMessage().contains(".example"));
    }

    @Test
    @DisplayName("db.username 값이 비어 있으면 즉시 예외")
    void failsWhenUsernameBlank() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> AppProperties.load("config/test-props/blank-username.properties"));

        assertTrue(e.getMessage().contains("db.username"));
    }

    @Test
    @DisplayName("db.password 항목 자체가 없으면 즉시 예외")
    void failsWhenPasswordKeyMissing() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> AppProperties.load("config/test-props/no-password-key.properties"));

        assertTrue(e.getMessage().contains("db.password"));
    }

    @Test
    @DisplayName("toString에 비밀번호가 그대로 나오지 않는다")
    void toStringHidesPassword() {
        AppProperties props = AppProperties.load("config/test-props/valid.properties");

        assertFalse(props.toString().contains("pw 1234"));
    }
}
