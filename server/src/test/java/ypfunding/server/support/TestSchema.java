package ypfunding.server.support;

import ypfunding.server.persistence.TransactionManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 테스트 DB(crowdfunding_test)의 테이블을 만들고 비우는 도구.
 *
 * <p>[테이블 만들기] {@link #createOnce(TransactionManager)}
 * sql/schema.sql을 읽어 세미콜론(;) 기준으로 잘라 한 문장씩 실행한다.
 * 테스트 실행(JVM) 한 번에 딱 한 번만 실행한다. schema.sql이 바뀌어도 다음 테스트 실행 때 자동 반영된다.
 * (schema.sql은 server/build.gradle 설정으로 테스트 클래스패스에 올라간다)
 *
 * <p>[테이블 비우기] {@link #clearAll(TransactionManager)}
 * 매 테스트 직전에 모든 테이블의 행을 지운다. 테스트끼리 데이터가 섞이지 않게 하기 위해서다.
 * 지우는 순서는 schema.sql의 DROP TABLE 순서(자식 → 부모)를 그대로 쓴다. 테이블 목록을 따로 관리하지 않는다.
 *
 * <p>[안전장치] 접속한 DB 이름이 "_test"로 끝나지 않으면 아무것도 하지 않고 예외를 던진다.
 * 설정 파일을 잘못 넣어 개발용 DB(crowdfunding)를 지우는 사고를 막기 위해서다.
 *
 * <p>[주의] AUTO_INCREMENT 값은 초기화하지 않는다. 테스트에서 id를 1, 2 같은 숫자로 가정하지 말고
 * {@link TestData}가 돌려준 id를 쓴다.
 */
public final class TestSchema {

    /** 클래스패스에서 찾을 schema.sql 경로 */
    private static final String SCHEMA_RESOURCE = "schema.sql";

    /** "DROP TABLE IF EXISTS 테이블명" 에서 테이블명을 뽑는 패턴 */
    private static final Pattern DROP_TABLE =
            Pattern.compile("^DROP\\s+TABLE\\s+IF\\s+EXISTS\\s+`?(\\w+)`?$", Pattern.CASE_INSENSITIVE);

    /** schema.sql 안에 있으면 안 되는 문장 (다른 스키마로 바뀌거나 스키마가 지워짐) */
    private static final Pattern FORBIDDEN =
            Pattern.compile("^(USE|CREATE\\s+DATABASE|CREATE\\s+SCHEMA|DROP\\s+DATABASE|DROP\\s+SCHEMA)\\b",
                    Pattern.CASE_INSENSITIVE);

    /** 이번 JVM에서 이미 테이블을 만들었는지 */
    private static boolean created = false;

    /** 비울 테이블 목록 (자식 → 부모 순서). createOnce에서 채운다 */
    private static List<String> tablesChildFirst = List.of();

    private TestSchema() {
    }

    /**
     * schema.sql로 테이블을 새로 만든다. 같은 JVM에서 두 번째 호출부터는 아무것도 하지 않는다.
     *
     * @param tx 테스트용 AppContext의 TransactionManager
     */
    public static synchronized void createOnce(TransactionManager tx) {
        if (created) {
            return;
        }
        List<String> statements = loadStatements();

        List<String> tables = new ArrayList<>();
        for (String sql : statements) {
            if (FORBIDDEN.matcher(sql).find()) {
                throw new IllegalStateException("schema.sql에 USE / CREATE DATABASE 같은 문장이 있으면 안 됩니다: " + firstLine(sql));
            }
            Matcher m = DROP_TABLE.matcher(sql);
            if (m.find()) {
                tables.add(m.group(1));
            }
        }

        tx.executeWithoutResult(conn -> {
            requireTestDatabase(conn);
            try (Statement st = conn.createStatement()) {
                for (String sql : statements) {
                    st.execute(sql);
                }
            }
        });

        tablesChildFirst = Collections.unmodifiableList(tables);
        created = true;
    }

    /**
     * 모든 테이블의 행을 지운다. (테이블 구조는 그대로)
     *
     * @param tx 테스트용 AppContext의 TransactionManager
     */
    public static void clearAll(TransactionManager tx) {
        if (!created) {
            throw new IllegalStateException("createOnce()를 먼저 호출해야 합니다.");
        }
        tx.executeWithoutResult(conn -> {
            requireTestDatabase(conn);
            try (Statement st = conn.createStatement()) {
                for (String table : tablesChildFirst) {
                    st.executeUpdate("DELETE FROM " + table);   // 테이블명은 schema.sql에서 온 값이라 안전
                }
            }
        });
    }

    /**
     * schema.sql을 읽어 실행할 문장 목록으로 만든다.
     * "--"로 시작하는 주석 줄은 버리고, 세미콜론으로 자른 뒤 빈 조각은 버린다.
     */
    static List<String> loadStatements() {
        String text;
        try (InputStream in = TestSchema.class.getClassLoader().getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "테스트 클래스패스에서 " + SCHEMA_RESOURCE + "를 찾을 수 없습니다. "
                                + "server/build.gradle의 sourceSets 설정(sql 폴더를 테스트 리소스로 등록)을 확인하세요.");
            }
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(SCHEMA_RESOURCE + "를 읽지 못했습니다.", e);
        }

        // 주석 줄 제거 (줄 끝에 붙은 "-- 설명"은 MySQL이 알아서 무시하므로 그대로 둔다)
        StringBuilder withoutCommentLines = new StringBuilder();
        for (String line : text.split("\\R")) {
            if (!line.strip().startsWith("--")) {
                withoutCommentLines.append(line).append('\n');
            }
        }

        List<String> statements = new ArrayList<>();
        for (String piece : withoutCommentLines.toString().split(";")) {
            String sql = piece.strip();
            if (!sql.isEmpty()) {
                statements.add(sql);
            }
        }
        return statements;
    }

    /** 지금 접속한 스키마 이름이 _test로 끝나는지 확인한다. */
    private static void requireTestDatabase(java.sql.Connection conn) throws java.sql.SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT DATABASE()")) {
            rs.next();
            String db = rs.getString(1);
            if (db == null || !db.toLowerCase(Locale.ROOT).endsWith("_test")) {
                throw new IllegalStateException(
                        "테스트가 테스트용이 아닌 DB에 연결되었습니다: " + db
                                + " (application-test.properties의 db.url이 crowdfunding_test를 가리키는지 확인하세요)");
            }
        }
    }

    private static String firstLine(String sql) {
        int nl = sql.indexOf('\n');
        return nl < 0 ? sql : sql.substring(0, nl);
    }
}