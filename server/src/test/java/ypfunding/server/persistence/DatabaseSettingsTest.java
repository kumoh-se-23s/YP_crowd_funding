package ypfunding.server.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ypfunding.common.type.UserType;
import ypfunding.server.support.DbTestBase;
import ypfunding.server.support.TestData;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DB 연결 설정(시간대, 문자셋)과 날짜 처리 방식이 설계대로인지 확인한다. (설계 9.5, 9.11, 10장)
 *
 * <p>[참고] DATETIME 컬럼은 초 단위까지만 저장한다. 밀리초가 붙은 값을 넣으면 MySQL이 가까운 초로 반올림한다.
 * (예: 10:00:00.700 → 10:00:01) 테스트 시계(TestClock)는 초 단위 값만 쓰므로 영향이 없다.
 */
class DatabaseSettingsTest extends DbTestBase {

    @Test
    @DisplayName("세션 시간대가 +09:00이다 (커넥션 풀의 초기화 SQL)")
    void sessionTimeZoneIsKst() {
        String tz = tx.execute(conn -> {
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT @@session.time_zone")) {
                rs.next();
                return rs.getString(1);
            }
        });

        assertEquals("+09:00", tz);
    }

    @Test
    @DisplayName("LocalDateTime을 setObject로 넣고 getObject로 읽으면 같은 값이 나온다 (시간대 변환 없음)")
    void localDateTimeRoundTrip() {
        LocalDateTime written = LocalDateTime.of(2026, 12, 7, 23, 59, 59);   // 날짜가 바뀌기 직전: 9시간 밀리면 바로 드러난다
        long userId = tx.execute(conn -> TestData.insertUser(conn, "time1", UserType.SUPPORTER, written));

        LocalDateTime read = tx.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT regdate FROM users WHERE user_id = ?")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getObject("regdate", LocalDateTime.class);
                }
            }
        });
        // DB 안에 저장된 글자 그대로도 확인한다 (Java 쪽에서 되돌려 읽어 맞춰지는 경우까지 걸러냄)
        String stored = tx.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT DATE_FORMAT(regdate, '%Y-%m-%d %H:%i:%s') FROM users WHERE user_id = ?")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getString(1);
                }
            }
        });

        assertEquals(written, read);
        assertEquals("2026-12-07 23:59:59", stored);
    }

    @Test
    @DisplayName("end_date는 DB가 start_date + duration일로 계산한다 (생성 컬럼)")
    void endDateIsGenerated() {
        LocalDateTime start = LocalDateTime.of(2026, 12, 7, 10, 0);
        long projectId = tx.execute(conn -> {
            long makerId = TestData.insertUser(conn, "maker1", UserType.MAKER, start);
            return TestData.insertApprovedProject(conn, makerId, 100_000, 30, start);
        });

        LocalDateTime endDate = tx.execute(conn -> {
            try (PreparedStatement ps = conn.prepareStatement("SELECT end_date FROM projects WHERE project_id = ?")) {
                ps.setLong(1, projectId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getObject("end_date", LocalDateTime.class);
                }
            }
        });

        assertEquals(LocalDateTime.of(2027, 1, 6, 10, 0), endDate);
    }

    @Test
    @DisplayName("한글과 이모지(4바이트 문자)가 깨지지 않고 저장된다 (utf8mb4)")
    void storesKoreanAndEmoji() {
        String name = "한글 이름 😀";
        long userId = tx.execute(conn -> TestData.insertUser(conn, "text1", UserType.SUPPORTER, clock.now()));

        String read = tx.execute(conn -> {
            try (PreparedStatement update = conn.prepareStatement("UPDATE users SET name = ? WHERE user_id = ?")) {
                update.setString(1, name);
                update.setLong(2, userId);
                update.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT name FROM users WHERE user_id = ?")) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getString("name");
                }
            }
        });

        assertEquals(name, read);
    }
}