package ypfunding.server.support;

import ypfunding.common.type.ApprovalStatus;
import ypfunding.common.type.UserType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

/**
 * 테스트용 데이터를 DB에 직접 넣는 도우미. (DAO를 거치지 않는 순수 SQL)
 *
 * <p>DAO를 쓰지 않는 이유: 테스트하려는 대상이 DAO와 Service다.
 * 준비 데이터까지 같은 DAO로 넣으면, DAO에 버그가 있을 때 준비 단계와 검사 단계가 함께 틀려서 버그가 숨는다.
 *
 * <p>[쓰는 법] 모든 메서드는 Connection을 받는다. tx.execute 안에서 호출한다.
 * <pre>
 *   long makerId   = tx.execute(conn -&gt; TestData.insertUser(conn, "maker1", UserType.MAKER, clock.now()));
 *   long projectId = tx.execute(conn -&gt;
 *           TestData.insertApprovedProject(conn, makerId, 100_000, 30, clock.now()));
 *   long rewardId  = tx.execute(conn -&gt; TestData.insertReward(conn, projectId, 10_000, 1));
 * </pre>
 *
 * <p>각 담당은 자기 테스트에 필요한 insert 메서드를 이 파일에 추가한다. (예: insertFund, insertLike)
 */
public final class TestData {

    /** 테스트 사용자의 비밀번호 해시 자리 채움 값 (64자). 로그인 테스트는 실제 해시로 따로 만든다 */
    public static final String DUMMY_PASSWORD_HASH = "0".repeat(64);

    /** 테스트 사용자의 salt 자리 채움 값 (32자) */
    public static final String DUMMY_SALT = "0".repeat(32);

    private TestData() {
    }

    /**
     * 사용자를 넣는다. 비밀번호는 자리 채움 값이다.
     *
     * @param conn     커넥션
     * @param loginId  로그인 아이디 (테스트 안에서 겹치지 않게)
     * @param userType 역할
     * @param regdate  가입 시각
     * @return 생성된 user_id
     * @throws SQLException DB 오류
     */
    public static long insertUser(Connection conn, String loginId, UserType userType, LocalDateTime regdate)
            throws SQLException {
        String sql = """
                INSERT INTO users (login_id, password, salt, name, address, usertype, regdate)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, loginId);
            ps.setString(2, DUMMY_PASSWORD_HASH);
            ps.setString(3, DUMMY_SALT);
            ps.setString(4, loginId + " 이름");
            ps.setString(5, loginId + " 주소");
            ps.setString(6, userType.name());
            ps.setObject(7, regdate);
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    /**
     * 승인된(진행 중인) 프로젝트를 넣는다. start_date = 승인 시각이므로 end_date = startDate + duration일.
     *
     * @param conn      커넥션
     * @param makerId   작성 메이커의 user_id
     * @param goal      목표 금액
     * @param duration  펀딩 기간(일)
     * @param startDate 승인 시각 (= 펀딩 시작 시각)
     * @return 생성된 project_id
     * @throws SQLException DB 오류
     */
    public static long insertApprovedProject(Connection conn, long makerId, long goal, int duration,
                                             LocalDateTime startDate) throws SQLException {
        return insertProject(conn, makerId, goal, duration, ApprovalStatus.APPROVED, startDate);
    }

    /**
     * 승인 대기(PENDING) 프로젝트를 넣는다. start_date는 NULL이다.
     *
     * @param conn     커넥션
     * @param makerId  작성 메이커의 user_id
     * @param goal     목표 금액
     * @param duration 펀딩 기간(일)
     * @return 생성된 project_id
     * @throws SQLException DB 오류
     */
    public static long insertPendingProject(Connection conn, long makerId, long goal, int duration)
            throws SQLException {
        return insertProject(conn, makerId, goal, duration, ApprovalStatus.PENDING, null);
    }

    private static long insertProject(Connection conn, long makerId, long goal, int duration,
                                      ApprovalStatus status, LocalDateTime startDate) throws SQLException {
        // end_date는 생성 컬럼이라 넣지 않는다 (넣으면 에러 3105)
        String sql = """
                INSERT INTO projects (user_id, title, description, goal, duration, start_date, approval_status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, makerId);
            ps.setString(2, "테스트 프로젝트");
            ps.setString(3, "테스트 설명");
            ps.setLong(4, goal);
            ps.setInt(5, duration);
            ps.setObject(6, startDate);   // null이면 NULL로 들어간다
            ps.setString(7, status.name());
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    /**
     * 리워드를 넣는다.
     *
     * @param conn      커넥션
     * @param projectId 소속 프로젝트
     * @param price     1개당 가격
     * @param stock     총 제공 수량. 0 = 무제한 (남은 수량 = stock - 후원 수량 합계)
     * @return 생성된 reward_id
     * @throws SQLException DB 오류
     */
    public static long insertReward(Connection conn, long projectId, int price, int stock) throws SQLException {
        String sql = """
                INSERT INTO rewards (project_id, name, price, description, stock)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, projectId);
            ps.setString(2, "테스트 리워드");
            ps.setInt(3, price);
            ps.setString(4, "테스트 리워드 설명");
            ps.setInt(5, stock);          // stock은 NOT NULL. 무제한도 NULL이 아니라 0으로 넣는다
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    /** INSERT 직후 AUTO_INCREMENT로 만들어진 id를 꺼낸다. */
    private static long generatedId(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("생성된 id를 받지 못했습니다.");
            }
            return keys.getLong(1);
        }
    }

    /** 좋아요를 넣는다. likes는 AUTO_INCREMENT id가 없어서 돌려주는 값이 없다. */
    public static void insertLike(Connection conn, long userId, long projectId) throws SQLException {
        String sql = "INSERT INTO likes (user_id, project_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, projectId);
            ps.executeUpdate();
        }
    }

    /** 프로젝트의 좋아요 수를 센다.*/
    public static int countLikes(Connection conn, long projectId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM likes WHERE project_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** 리워드의 팔린 수량 합계 (검증용) */
    public static long sumFundQuantity(Connection conn, long rewardId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM funds WHERE reward_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, rewardId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
    /** 비밀번호를 실제 해시로 넣은 사용자 (로그인 테스트용) */
    public static long insertUserWithPassword(Connection conn, String loginId, String password,
                                              UserType userType, LocalDateTime regdate) throws SQLException {
        String salt = "0123456789abcdef0123456789abcdef";   // 32자
        String sql = """
                INSERT INTO users (login_id, password, salt, name, address, usertype, regdate)
                VALUES (?, SHA2(CONCAT(?, ?), 256), ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, loginId);
            ps.setString(2, salt);
            ps.setString(3, password);
            ps.setString(4, salt);
            ps.setString(5, loginId + " 이름");
            ps.setString(6, loginId + " 주소");
            ps.setString(7, userType.name());
            ps.setObject(8, regdate);
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    /** 리워드의 stock 값*/
    public static long getStock(Connection conn, long rewardId) throws SQLException {
        String sql = "SELECT stock FROM rewards WHERE reward_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, rewardId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /** 후원을 넣는다. 생성된 fund_id 반환 */
    public static long insertFund(Connection conn, long userId, long projectId, long rewardId,
                                  int quantity, LocalDateTime date) throws SQLException {
        String sql = """
            INSERT INTO funds (user_id, project_id, reward_id, quantity, date)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setLong(2, projectId);
            ps.setLong(3, rewardId);
            ps.setInt(4, quantity);
            ps.setObject(5, date);
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    /** 사용자의 취소 기록 개수*/
    public static int countCanceledFunds(Connection conn, long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM canceled_funds WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** 사용자의 취소 기록을 넣고 canceled_id 반환*/
    public static long insertCanceledFund(Connection conn, long userId, long projectId, long rewardId,
                                          int quantity, String content, LocalDateTime cancelDate) throws SQLException {
        String sql = """
            INSERT INTO canceled_funds (user_id, project_id, reward_id, quantity, content, cancel_date)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setLong(2, projectId);
            ps.setLong(3, rewardId);
            ps.setInt(4, quantity);
            ps.setString(5, content);
            ps.setObject(6, cancelDate);
            ps.executeUpdate();
            return generatedId(ps);
        }
    }

    /** 리뷰를 넣는다 */
    public static void insertReview(Connection conn, long fundId, int star, String content,
                                    LocalDateTime date) throws SQLException {
        String sql = "INSERT INTO reviews (fund_id, star, content, date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, fundId);
            ps.setInt(2, star);
            ps.setString(3, content);
            ps.setObject(4, date);
            ps.executeUpdate();
        }
    }

    /** 후원의 리뷰 개수 (0 또는 1)*/
    public static int countReviews(Connection conn, long fundId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reviews WHERE fund_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, fundId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}