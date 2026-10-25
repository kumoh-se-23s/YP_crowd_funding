package ypfunding.server.service;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ypfunding.common.dto.ImageDTO;
import ypfunding.common.dto.ProjectDTO;
import ypfunding.common.dto.ProjectDetailDTO;
import ypfunding.common.dto.ReviewDTO;
import ypfunding.common.dto.RewardDTO;
import ypfunding.common.type.ApprovalStatus;
import ypfunding.common.type.Category;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.exception.NotAllowedException;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.CategoryDAO;
import ypfunding.server.persistence.dao.ImageDAO;
import ypfunding.server.persistence.dao.ProjectsDAO;
import ypfunding.server.persistence.dao.ReviewsDAO;
import ypfunding.server.persistence.dao.RewardsDAO;
import ypfunding.server.util.FileUtil;

import javax.sql.rowset.serial.SerialBlob;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B 역할(메이커·상세 보기·리뷰 조회) Service 및 DAO 통합 단위 테스트.
 * 평가표 항목 1, 2, 3, 5, 14번 및 명세서 규칙(LoginUser 검증, NULL 재고 보존, 자식->부모 삭제 순서, MIME 시그니처 판별)을 검증한다.
 */
class ProjectAndRewardServiceTest {

    private static BasicDataSource dataSource;
    private static TransactionManager tx;

    private static ProjectsDAO projectsDAO;
    private static CategoryDAO categoryDAO;
    private static RewardsDAO rewardsDAO;
    private static ImageDAO imageDAO;
    private static ReviewsDAO reviewsDAO;

    private static ProjectService projectService;
    private static RewardService rewardService;

    private LoginUser makerUser;
    private LoginUser otherMakerUser;
    private LoginUser supporterUser;
    private LoginUser adminUser;

    /** 테스트용 완전한 1x1 투명 PNG 이미지 바이트 (헤더 + IHDR + IDAT + IEND) */
    private static final byte[] SAMPLE_PNG_BYTES = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
    );

    @BeforeAll
    static void setUpAll() throws Exception {
        Properties props = loadTestProperties();

        dataSource = new BasicDataSource();
        dataSource.setUrl(props.getProperty("db.url"));
        dataSource.setUsername(props.getProperty("db.username"));
        dataSource.setPassword(props.getProperty("db.password"));
        dataSource.setInitialSize(5);
        dataSource.setMaxTotal(10);
        dataSource.setConnectionInitSqls(List.of("SET time_zone = '+09:00'"));

        tx = new TransactionManager(dataSource);
        projectsDAO = new ProjectsDAO();
        categoryDAO = new CategoryDAO();
        rewardsDAO = new RewardsDAO();
        imageDAO = new ImageDAO();
        reviewsDAO = new ReviewsDAO();

        projectService = new ProjectService(tx, projectsDAO, categoryDAO, rewardsDAO, imageDAO, reviewsDAO);
        rewardService = new RewardService(tx, projectsDAO, rewardsDAO, imageDAO);
    }

    @AfterAll
    static void tearDownAll() throws Exception {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @BeforeEach
    void setUp() {
        // 각 테스트마다 깨끗한 상태에서 시작하도록 테이블 초기화 및 기본 사용자 생성
        tx.execute(conn -> {
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

            long makerId = insertTestUser(conn, "maker1", "메이커1", UserType.MAKER);
            long otherMakerId = insertTestUser(conn, "maker2", "메이커2", UserType.MAKER);
            long supporterId = insertTestUser(conn, "supporter1", "서포터1", UserType.SUPPORTER);
            long adminId = insertTestUser(conn, "admin1", "관리자1", UserType.ADMIN);

            makerUser = new LoginUser(makerId, UserType.MAKER);
            otherMakerUser = new LoginUser(otherMakerId, UserType.MAKER);
            supporterUser = new LoginUser(supporterId, UserType.SUPPORTER);
            adminUser = new LoginUser(adminId, UserType.ADMIN);
            return null;
        });
    }

    // =================================================================
    // 평가표 1·2번: 프로젝트 기본 정보 및 리워드 등록 후 신청 (PENDING)
    // =================================================================

    @Test
    @DisplayName("평가표 1·2: 메이커가 기본 정보, 카테고리, 리워드(무제한/제한), 이미지를 한 번에 등록하면 PENDING 상태로 저장된다")
    void createProject_Success() throws Exception {
        ProjectDTO projectInput = createSampleProjectDTO("스마트 텀블러 프로젝트", 1_000_000L, 30);
        List<Category> categories = List.of(Category.TECH_HOME_APPLIANCES, Category.HOME_LIVING);

        RewardDTO unlimitedReward = createSampleRewardDTO("얼리버드 단품 (무제한)", 25_000L, "텀블러 1개", null);
        RewardDTO limitedReward = createSampleRewardDTO("스페셜 패키지 (한정판)", 45_000L, "텀블러 2개 + 파우치", 50L);

        ImageDTO imageInput = new ImageDTO();
        imageInput.setName("C:\\fakepath\\tumbler_special.png");
        imageInput.setType("text/plain"); // 클라이언트가 잘못된 MIME을 보내도 서버가 바이트 시그니처로 덮어써야 함
        imageInput.setImageData(new SerialBlob(SAMPLE_PNG_BYTES));

        long projectId = projectService.createProject(
                makerUser,
                projectInput,
                categories,
                List.of(unlimitedReward, limitedReward),
                Map.of(1, imageInput) // 두 번째 리워드(index 1)에만 이미지 첨부
        );

        assertTrue(projectId > 0);

        // 메이커 본인이 상세 조회하여 저장 결과 검증
        ProjectDetailDTO detail = projectService.getProjectDetail(makerUser, projectId);
        assertNotNull(detail);
        assertEquals("스마트 텀블러 프로젝트", detail.getProject().getTitle());
        assertEquals(ApprovalStatus.PENDING, detail.getProject().getApprovalStatus());
        assertNull(detail.getProject().getStartDate());
        assertNull(detail.getProject().getEndDate());
        assertEquals(2, detail.getCategories().size());

        List<RewardDTO> savedRewards = detail.getRewards();
        assertEquals(2, savedRewards.size());
        // 중요: 무제한 리워드의 STOCK이 0이 아닌 null로 정확히 읽히는지 검증 (명세서 9.8)
        assertNull(savedRewards.get(0).getStock(), "무제한 리워드의 stock은 null이어야 합니다.");
        assertEquals(50L, savedRewards.get(1).getStock());

        // 상세 조회에서는 두 번째 리워드에만 이미지가 존재한다고 표시되어야 함
        assertEquals(List.of(savedRewards.get(1).getRewardId()), detail.getRewardIdsWithImage());

        // RewardService로 실제 이미지 조회 시 경로 제거된 파일명과 바이트 시그니처 MIME(image/png) 확인
        ImageDTO savedImage = rewardService.getRewardImage(makerUser, savedRewards.get(1).getRewardId());
        assertNotNull(savedImage);
        assertEquals("tumbler_special.png", savedImage.getName());
        assertEquals("image/png", savedImage.getType());
    }

    @Test
    @DisplayName("권한 검증: 서포터가 프로젝트 등록을 시도하면 NotAllowedException이 발생한다")
    void createProject_BySupporter_ThrowsNotAllowed() {
        ProjectDTO projectInput = createSampleProjectDTO("서포터가 시도하는 프로젝트", 500_000L, 15);
        List<Category> categories = List.of(Category.TECH_HOME_APPLIANCES, Category.DESIGN);
        List<RewardDTO> rewards = List.of(
                createSampleRewardDTO("리워드1", 10_000L, "설명1", null),
                createSampleRewardDTO("리워드2", 20_000L, "설명2", 10L)
        );

        assertThrows(NotAllowedException.class, () ->
                projectService.createProject(supporterUser, projectInput, categories, rewards)
        );
    }

    @Test
    @DisplayName("입력 검증: 리워드가 2개 미만이거나 중복 카테고리를 선택하면 BusinessException이 발생한다")
    void createProject_InvalidRewardsOrCategories_ThrowsBusinessException() {
        ProjectDTO projectInput = createSampleProjectDTO("검증 실패 프로젝트", 500_000L, 15);

        // 1. 리워드 1개만 넣은 경우
        List<Category> validCategories = List.of(Category.TECH_HOME_APPLIANCES, Category.DESIGN);
        List<RewardDTO> singleReward = List.of(createSampleRewardDTO("리워드1", 10_000L, "설명1", null));
        assertThrows(BusinessException.class, () ->
                projectService.createProject(makerUser, projectInput, validCategories, singleReward)
        );

        // 2. 중복 카테고리를 넣은 경우
        List<Category> duplicateCategories = List.of(Category.FOOD, Category.FOOD);
        List<RewardDTO> twoRewards = List.of(
                createSampleRewardDTO("리워드1", 10_000L, "설명1", null),
                createSampleRewardDTO("리워드2", 20_000L, "설명2", 10L)
        );
        assertThrows(BusinessException.class, () ->
                projectService.createProject(makerUser, projectInput, duplicateCategories, twoRewards)
        );
    }

    // =================================================================
    // 평가표 3번: 거절(REJECTED) 시 수정 및 재신청
    // =================================================================

    @Test
    @DisplayName("평가표 3: 반려(REJECTED)된 프로젝트를 수정·재신청하면 기존 이미지·리워드가 교체되고 PENDING으로 변경된다")
    void resubmitRejectedProject_Success() throws Exception {
        // 1. 초기 프로젝트 및 이미지 포함 리워드 등록
        ProjectDTO initialProject = createSampleProjectDTO("초기 제목", 1_000_000L, 20);
        ImageDTO initialImage = new ImageDTO();
        initialImage.setName("old_image.png");
        initialImage.setImageData(new SerialBlob(SAMPLE_PNG_BYTES));

        long projectId = projectService.createProject(
                makerUser,
                initialProject,
                List.of(Category.GAME, Category.GOODS),
                List.of(
                        createSampleRewardDTO("구형 리워드1", 10_000L, "구형 설명1", 10L),
                        createSampleRewardDTO("구형 리워드2", 20_000L, "구형 설명2", null)
                ),
                Map.of(0, initialImage)
        );

        // 2. PENDING 상태에서 바로 재신청 시도 -> 실패해야 함 (REJECTED에서만 가능)
        ProjectDTO updateAttempt = createSampleProjectDTO("수정 제목", 2_000_000L, 30);
        updateAttempt.setProjectId(projectId);
        assertThrows(BusinessException.class, () ->
                projectService.resubmitRejectedProject(
                        makerUser,
                        updateAttempt,
                        List.of(Category.BOOK, Category.ART),
                        List.of(
                                createSampleRewardDTO("신형 리워드1", 15_000L, "신형 설명1", 30L),
                                createSampleRewardDTO("신형 리워드2", 30_000L, "신형 설명2", null)
                        )
                )
        );

        // 3. 관리자가 프로젝트를 반려(REJECTED) 처리한 상황 재현
        tx.execute(conn -> {
            assertEquals(1, projectsDAO.rejectProjectUpdate(conn, projectId));
            return null;
        });

        // 4. 다른 메이커가 남의 반려 프로젝트를 수정 시도 -> NotAllowedException 발생
        assertThrows(NotAllowedException.class, () ->
                projectService.resubmitRejectedProject(
                        otherMakerUser,
                        updateAttempt,
                        List.of(Category.BOOK, Category.ART),
                        List.of(
                                createSampleRewardDTO("신형 리워드1", 15_000L, "신형 설명1", 30L),
                                createSampleRewardDTO("신형 리워드2", 30_000L, "신형 설명2", null)
                        )
                )
        );

        // 5. 본인 메이커가 수정 후 재신청 -> 성공 (자식 IMAGE -> 부모 REWARDS 순 삭제 후 재등록)
        projectService.resubmitRejectedProject(
                makerUser,
                updateAttempt,
                List.of(Category.BOOK, Category.ART),
                List.of(
                        createSampleRewardDTO("신형 리워드1", 15_000L, "신형 설명1", 30L),
                        createSampleRewardDTO("신형 리워드2", 30_000L, "신형 설명2", null)
                )
        );

        ProjectDetailDTO updatedDetail = projectService.getProjectDetail(makerUser, projectId);
        assertEquals(ApprovalStatus.PENDING, updatedDetail.getProject().getApprovalStatus());
        assertEquals("수정 제목", updatedDetail.getProject().getTitle());
        assertEquals(2_000_000L, updatedDetail.getProject().getGoal());
        assertEquals(List.of(Category.BOOK, Category.ART), updatedDetail.getCategories());
        assertEquals("신형 리워드1", updatedDetail.getRewards().get(0).getName());
        assertTrue(updatedDetail.getRewardIdsWithImage().isEmpty(), "교체 후에는 이전 이미지가 남아있지 않아야 합니다.");
    }

    // =================================================================
    // 평가표 14번 & 5번: 프로젝트 상세 보기 및 리뷰 최신순 조회
    // =================================================================

    @Test
    @DisplayName("평가표 14 & 5: 승인된 프로젝트 상세 보기 시 제공 시기(=종료일)가 계산되고 리뷰가 최신순으로 조회된다")
    void getProjectDetail_And_ReviewsLatestOrder() {
        long projectId = projectService.createProject(
                makerUser,
                createSampleProjectDTO("승인될 프로젝트", 500_000L, 14),
                List.of(Category.FOOD, Category.HOME_LIVING),
                List.of(
                        createSampleRewardDTO("기본 세트", 12_000L, "수제청 1병", 100L),
                        createSampleRewardDTO("선물 세트", 22_000L, "수제청 2병", null)
                )
        );

        // 승인 전(PENDING)에는 서포터가 상세 조회를 할 수 없다 (명세서 3장)
        assertThrows(NotAllowedException.class, () ->
                projectService.getProjectDetail(supporterUser, projectId)
        );

        // 승인 처리 및 후원 2건 + 시간차 리뷰 2건 삽입
        LocalDateTime approvedAt = LocalDateTime.of(2026, 10, 1, 12, 0, 0);
        tx.execute(conn -> {
            assertEquals(1, projectsDAO.approveProjectUpdate(conn, projectId));
            List<RewardDTO> rewards = rewardsDAO.findByProjectId(conn, projectId);
            long rewardId = rewards.get(0).getRewardId();

            long supporter2Id = insertTestUser(conn, "supporter2", "서포터2", UserType.SUPPORTER);
            long fundId1 = insertTestFund(conn, supporterUser.userId(), projectId, rewardId, 1, approvedAt.plusDays(1));
            long fundId2 = insertTestFund(conn, supporter2Id, projectId, rewardId, 2, approvedAt.plusDays(2));

            // 일부러 오래된 리뷰를 먼저 넣고 최신 리뷰를 나중에 넣음
            ReviewDTO olderReview = new ReviewDTO();
            olderReview.setFundID(fundId1);
            olderReview.setStar(4);
            olderReview.setContent("먼저 작성한 리뷰입니다.");
            olderReview.setDate(LocalDateTime.of(2026, 10, 20, 10, 0, 0));
            reviewsDAO.insert(conn, olderReview);

            ReviewDTO newerReview = new ReviewDTO();
            newerReview.setFundID(fundId2);
            newerReview.setStar(5);
            newerReview.setContent("나중에 작성한 최신 리뷰입니다.");
            newerReview.setDate(LocalDateTime.of(2026, 10, 22, 15, 30, 0));
            reviewsDAO.insert(conn, newerReview);
            return null;
        });

        // 승인 후 서포터가 상세 조회
        ProjectDetailDTO detail = projectService.getProjectDetail(supporterUser, projectId);
        assertEquals(ApprovalStatus.APPROVED, detail.getProject().getApprovalStatus());
        assertNotNull(detail.getProject().getEndDate(), "DB 생성 컬럼인 END_DATE가 자동 계산되어야 합니다.");
        assertEquals(detail.getProject().getEndDate(), detail.getRewardDeliveryDate(), "리워드 제공 시기는 펀딩 종료일과 같아야 합니다.");

        // 리뷰 최신순(내림차순) 정렬 검증 (평가표 5번)
        List<ReviewDTO> reviews = detail.getReviews();
        assertEquals(2, reviews.size());
        assertEquals("나중에 작성한 최신 리뷰입니다.", reviews.get(0).getContent());
        assertEquals("먼저 작성한 리뷰입니다.", reviews.get(1).getContent());
    }

    @Test
    @DisplayName("FileUtil 검증: 바이트 시그니처가 이미지가 아니면 BusinessException을 던진다")
    void fileUtil_InvalidMagicBytes_ThrowsBusinessException() {
        byte[] fakeBytes = "This is not an image file".getBytes();
        assertThrows(BusinessException.class, () -> FileUtil.detectImageMimeType(fakeBytes));
        assertEquals("image/png", FileUtil.detectImageMimeType(SAMPLE_PNG_BYTES));
    }

    // =================================================================
    // 테스트 픽스처 생성 헬퍼
    // =================================================================

    private static Properties loadTestProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream in = ProjectAndRewardServiceTest.class.getClassLoader()
                .getResourceAsStream("config/application-test.properties")) {
            if (in != null) {
                props.load(in);
                return props;
            }
        }
        try (InputStream in = ProjectAndRewardServiceTest.class.getClassLoader()
                .getResourceAsStream("config/application.properties")) {
            if (in != null) {
                props.load(in);
                return props;
            }
        }
        throw new IllegalStateException("classpath에서 config/application-test.properties 또는 config/application.properties를 찾을 수 없습니다.");
    }

    private static ProjectDTO createSampleProjectDTO(String title, long goal, int duration) {
        ProjectDTO dto = new ProjectDTO();
        dto.setTitle(title);
        dto.setDescription(title + " 상세 설명");
        dto.setGoal(goal);
        dto.setDuration(duration);
        return dto;
    }

    private static RewardDTO createSampleRewardDTO(String name, long price, String description, Long stock) {
        RewardDTO dto = new RewardDTO();
        dto.setName(name);
        dto.setPrice(price);
        dto.setDescription(description);
        dto.setStock(stock);
        return dto;
    }

    private static long insertTestUser(java.sql.Connection conn, String loginId, String name, UserType userType) throws SQLException {
        String sql = """
                INSERT INTO users (login_id, password, salt, name, address, usertype, regdate)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, loginId);
            ps.setString(2, "0".repeat(64));
            ps.setString(3, "a".repeat(32));
            ps.setString(4, name);
            ps.setString(5, "서울특별시 테스트구");
            ps.setString(6, userType.name());
            ps.setObject(7, LocalDateTime.of(2026, 10, 1, 9, 0));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private static long insertTestFund(
            java.sql.Connection conn,
            long userId,
            long projectId,
            long rewardId,
            int quantity,
            LocalDateTime date) throws SQLException {
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
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }
}