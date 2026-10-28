package ypfunding.server.check.function;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ypfunding.common.dto.ProjectDTO;
import ypfunding.common.dto.RewardDTO;
import ypfunding.common.type.ApprovalStatus;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.bootstrap.AppContext;
import ypfunding.server.check.CheckUsers;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MakerCheckTest {

    private static AppContext context;
    private MakerCheck makerCheck;
    private LoginUser maker;

    @BeforeAll
    static void setUpAll() {
        // 테스트용 설정 파일(application-test.properties)로 AppContext 조립
        context = new AppContext(
                "config/application-test.properties",
                Clock.system(ZoneId.of("Asia/Seoul"))
        );
    }

    @AfterAll
    static void tearDownAll() {
        if (context != null) {
            context.close();
        }
    }

    @BeforeEach
    void setUp() {
        // 1. 트랜잭션 안에서는 순수 DB INSERT만 수행 (authService 호출 금지)
        context.getTransactionManager().execute(conn -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM reviews");
                stmt.executeUpdate("DELETE FROM canceled_funds");
                stmt.executeUpdate("DELETE FROM funds");
                stmt.executeUpdate("DELETE FROM likes");
                stmt.executeUpdate("DELETE FROM reject");
                stmt.executeUpdate("DELETE FROM image");
                stmt.executeUpdate("DELETE FROM rewards");
                stmt.executeUpdate("DELETE FROM category");
                stmt.executeUpdate("DELETE FROM projects");
                stmt.executeUpdate("DELETE FROM users");
            }

            // 기본 유저 5명 생성
            try {
                insertUserWithId(conn, 1L, "admin", "관리자", UserType.ADMIN);
                insertUserWithId(conn, 2L, "maker1", "메이커1", UserType.MAKER);
                insertUserWithId(conn, 3L, "maker2", "메이커2", UserType.MAKER);
                insertUserWithId(conn, 4L, "supporter1", "서포터1", UserType.SUPPORTER);
                insertUserWithId(conn, 5L, "supporter2", "서포터2", UserType.SUPPORTER);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // test03 대상 프로젝트 사전 생성
            String projectSql = """
                    INSERT INTO projects (project_id, user_id, title, description, goal, duration, approval_status)
                    VALUES (5, 2, '[사전데이터] 리워드 등록 대상 프로젝트', '설명', 1000000, 20, 'PENDING')
                    """;
            try (PreparedStatement ps = conn.prepareStatement(projectSql)) {
                ps.executeUpdate();
            }
            return null;
        });

        // 2. 트랜잭션 바깥(여기)에서 LoginUser를 직접 생성해서 넘기거나,
        // 만약 CheckUsers 내부 login()을 꼭 태워야 한다면 트랜잭션이 없는 상태에서 호출되어야 합니다.
        // 아래처럼 직접 LoginUser를 만들어서 MakerCheck에 주입하세요.
        maker = new LoginUser(2L, UserType.MAKER);

        // CheckUsers를 생성하되, 내부 login()을 호출하지 않고 필드를 우회할 수 없다면
        // 익명 클래스나 상속을 통해 CheckUsers를 오버라이드하여 테스트용으로 사용합니다.
        CheckUsers checkUsers = new CheckUsers(context) {
            @Override
            public LoginUser maker() {
                return new LoginUser(2L, UserType.MAKER);
            }
            @Override
            public LoginUser admin() {
                return new LoginUser(1L, UserType.ADMIN);
            }
            @Override
            public LoginUser supporter() {
                return new LoginUser(4L, UserType.SUPPORTER);
            }
        };

        makerCheck = new MakerCheck(context, checkUsers);
    }

    @Test
    @DisplayName("발표 시연 02번: test02RegisterProject 실행 시 프로젝트 기본 정보와 카테고리가 정상 등록된다")
    void test02RegisterProject_Success() {
        makerCheck.test02RegisterProject();

        // 고정 ID 6L 대신, 데이터베이스에서 가장 최근에 생성된 프로젝트의 ID를 가져와서 검증
        ProjectDTO project = context.getTransactionManager().execute(conn -> {
            String sql = "SELECT * FROM projects ORDER BY project_id DESC LIMIT 1";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ProjectDTO dto = new ProjectDTO();
                    dto.setProjectId(rs.getLong("project_id"));
                    dto.setTitle(rs.getString("title"));
                    dto.setApprovalStatus(ApprovalStatus.valueOf(rs.getString("approval_status")));
                    return dto;
                }
            }
            return null;
        });

        assertNotNull(project);
        assertEquals("[시연02] 초경량 휴대용 에스프레소 머신", project.getTitle());
        assertEquals(ApprovalStatus.PENDING, project.getApprovalStatus());
    }
    @Test
    @DisplayName("발표 시연 03번: test03RegisterReward 단독 실행 시 5번 프로젝트에 한정/무제한 리워드 2종이 등록된다")
    void test03RegisterReward_Success() {
        // test02를 실행하지 않고 test03만 독립적으로 실행!
        makerCheck.test03RegisterReward();

        List<RewardDTO> rewards = context.getRewardService().getRewardsByProject(maker, 5L);
        assertEquals(2, rewards.size());
        assertEquals(20L, rewards.get(0).getStock());
        assertNull(rewards.get(1).getStock(), "두 번째 리워드는 무제한(null)이어야 합니다.");
    }

    private void insertUserWithId(java.sql.Connection conn, long userId, String loginId, String name, UserType type) throws Exception {
        String sql = """
                INSERT INTO users (user_id, login_id, password, salt, name, address, usertype, regdate)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, loginId);
            ps.setString(3, "0".repeat(64));
            ps.setString(4, "a".repeat(32));
            ps.setString(5, name);
            ps.setString(6, "구미시 대학로 61");
            ps.setString(7, type.name());
            ps.setObject(8, LocalDateTime.of(2026, 10, 1, 9, 0));
            ps.executeUpdate();
        }
    }
}