package ypfunding.client.view;

import ypfunding.client.session.ClientSession;
import ypfunding.client.util.ConsoleIO;
import ypfunding.common.constant.DomainRules;
import ypfunding.common.dto.FundingStatusDTO;
import ypfunding.common.dto.FundingSupporterDTO;
import ypfunding.common.dto.ImageDTO;
import ypfunding.common.dto.ProjectDTO;
import ypfunding.common.dto.ProjectQueryDTO;
import ypfunding.common.dto.RejectDTO;
import ypfunding.common.dto.RewardDTO;
import ypfunding.common.dto.RewardStatusDTO;
import ypfunding.common.type.ApprovalStatus;
import ypfunding.common.type.Category;
import ypfunding.common.util.DateTimeUtil;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.bootstrap.AppContext;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.ProjectsDAO;
import ypfunding.server.persistence.dao.RejectDAO;
import ypfunding.server.persistence.dao.Tables;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLSelect;
import ypfunding.server.persistence.dao.dynamicsql.col.SimpleOrderBySelector;
import ypfunding.server.persistence.dao.dynamicsql.condition.BasicCondition;
import ypfunding.server.persistence.dao.dynamicsql.op.ArithmeticConditionOperator;
import ypfunding.server.persistence.dao.dynamicsql.table.DirectReferenceTable;
import ypfunding.server.persistence.dao.dynamicsql.token.ColumnValueToken;
import ypfunding.server.persistence.dao.dynamicsql.token.UserValueToken;
import ypfunding.server.service.FundingStatusService;
import ypfunding.server.service.ProjectQueryService;
import ypfunding.server.service.ProjectService;
import ypfunding.server.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 메이커(MAKER) 전용 CLI 화면 (B 담당 — 평가표 1, 2, 3, 4, 5, 10, 14).
 * - [1·2] 프로젝트 기본 정보 + 단일 카테고리 + 리워드(최소 2개) 및 대표 이미지 등록 신청
 * - [3] 반려된(REJECTED) 프로젝트 거절 사유 확인 및 수정·재신청
 * - [4] 프로젝트별 펀딩 현황 보기 (총 금액, 리워드별 신청/잔여수량, 서포터·배송지)
 * - [5·14] 프로젝트 상세 보기 및 최신순 리뷰 / 이미지 다운로드 연결
 */
public class MakerCliView {

    private final AppContext context;
    private final ProjectService projectService;
    private final ProjectQueryService projectQueryService;
    private final FundingStatusService fundingStatusService;
    private final TransactionManager transactionManager;
    private final ProjectsDAO projectsDAO;
    private final AuthCliView authCliView;
    private final ProjectDetailCliView projectDetailCliView;

    public MakerCliView(
            AppContext context,
            AuthCliView authCliView,
            ProjectDetailCliView projectDetailCliView) {
        this.context = context;
        this.projectService = context.getProjectService();
        this.projectQueryService = context.getProjectQueryService();
        this.fundingStatusService = context.getFundingStatusService();
        this.transactionManager = context.getTransactionManager();
        this.projectsDAO = new ProjectsDAO();
        this.authCliView = authCliView;
        this.projectDetailCliView = projectDetailCliView;
    }

    // 메이커 메인 메뉴
    // 로그인 한 상태로 바로 종료 가능하게 만들지 이야기 해봐야함
    public void showMainMenu() {
        while (ClientSession.isLoggedIn()) {
            System.out.println();
            ConsoleIO.printDoubleLine();
            System.out.printf(" [메이커 메인 메뉴] 로그인: %s (%s)%n",
                    ClientSession.getUserDto().getName(),
                    ClientSession.getUserType().name());
            ConsoleIO.printDoubleLine();
            System.out.println(" 1. 프로젝트 등록 신청 (기본 정보 + 카테고리 1개 + 리워드/대표이미지)");
            System.out.println(" 2. 내 프로젝트 목록 및 관리 (상세 보기 / 펀딩 현황 / 반려 수정·재신청)");
            System.out.println(" 3. 개인정보 수정");
            System.out.println(" 0. 로그아웃");
            ConsoleIO.printSingleLine();

            int choice = ConsoleIO.readIntInRange("선택 > ",0,3);
            switch (choice) {
                case 1 -> handleRegisterProject();
                case 2 -> handleManageMyProjects();
                case 3 -> authCliView.showProfileUpdateMenu();
                case 0 -> {
                    ClientSession.logout();
                    System.out.println("=> 로그아웃되었습니다.");
                    return;
                }
            }
        }
    }

    // =========================================================================
    // 1. 프로젝트 등록 신청
    // =========================================================================

    /**
     * 프로젝트 기본 정보 + 단일 카테고리와 리워드 최소 2개 + 대표 이미지를 입력받아
     * 단일 트랜잭션으로 프로젝트 등록을 신청
     */
    private void handleRegisterProject() {
        LoginUser maker = ClientSession.getLoginUser();
        System.out.println();
        System.out.println("---------------- [Step 1. 프로젝트 기본 정보 등록] ----------------");
        ProjectDTO projectDTO = promptProjectBasicInfo();

        System.out.println();
        System.out.println("---------------- [Step 2. 리워드 및 대표 이미지 등록 (최소 2개)] ----------------");
        List<RewardDTO> rewards = new ArrayList<>();
        Map<Integer, ImageDTO> rewardImagesByIndex = new HashMap<>();
        promptRewardsAndImages(rewards, rewardImagesByIndex);

        ConsoleIO.printSingleLine();
        if (!ConsoleIO.readYesNo("• 위 내용으로 프로젝트 등록을 신청하시겠습니까? (y/n) : ")) {
            System.out.println("=> 프로젝트 등록 신청이 취소되었습니다.");
            return;
        }

        try {
            long projectId = projectService.createProject(maker, projectDTO, rewards, rewardImagesByIndex);
            ConsoleIO.printlnSuccess(String.format(
                    "프로젝트 등록 신청이 완료되었습니다! (생성된 프로젝트 번호: #%d, 상태: PENDING)", projectId));
        } catch (BusinessException e) {
            ConsoleIO.printlnError(e.getMessage());
        } catch (Exception e) {
            ConsoleIO.printlnError("프로젝트 등록 신청 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 프로젝트 기본 정보를 콘솔에서 입력받아 ProjectDTO를 생성
    private ProjectDTO promptProjectBasicInfo() {
        String title = ConsoleIO.readNonBlankString("• 프로젝트 제목 (최대 100자) : ");
        String description = ConsoleIO.readNonBlankString("• 프로젝트 설명 : ");
        Category category = ConsoleIO.selectSingleCategory();
        long goal = ConsoleIO.readLongMin("• 목표 금액 (원) : ", 1L);
        int duration = ConsoleIO.readIntInRange("• 펀딩 기간 (일) : ", 1, 3650);

        ProjectDTO dto = new ProjectDTO();
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setCategoryName(category);
        dto.setGoal(goal);
        dto.setDuration(duration);
        return dto;
    }

    // 최소 2개 이상의 리워드 정보와 선택적 대표 이미지를 입력받음
    private void promptRewardsAndImages(List<RewardDTO> rewards, Map<Integer, ImageDTO> rewardImagesByIndex) {
        int index = 0;
        while (true) {
            System.out.printf("%n[리워드 #%d]%n", index + 1);
            String name = ConsoleIO.readNonBlankString("• 리워드 이름 : ");
            long price = ConsoleIO.readLongMin("• 리워드 가격 (원) : ", 1L);
            String description = ConsoleIO.readNonBlankString("• 리워드 설명 : ");
            long stock = ConsoleIO.readLongMin("• 제공 수량 (0 입력 시 무제한) : ", 0L);

            RewardDTO rewardDTO = new RewardDTO();
            rewardDTO.setName(name);
            rewardDTO.setPrice(price);
            rewardDTO.setDescription(description);
            rewardDTO.setStock(stock);
            rewards.add(rewardDTO);

            if (ConsoleIO.readYesNo("• 대표 이미지를 함께 등록하시겠습니까? (y/n) : ")) {
                ImageDTO imageDTO = promptValidImageFile();
                if (imageDTO != null) {
                    rewardImagesByIndex.put(index, imageDTO);
                }
            }

            //최소 2개 이상 신청을 보장
            index++;
            if (index >= DomainRules.MIN_REWARDS_PER_PROJECT) {
                if (!ConsoleIO.readYesNo("%n• 리워드를 더 추가하시겠습니까? (y/n) : ".formatted())) {
                    break;
                }
            }
        }
    }

    // 로컬 파일 경로를 입력받아 path로 읽고 형식과 크기를 검증하여 ImageDTO로 반환
    private ImageDTO promptValidImageFile() {
        while (true) {
            Path filePath = ConsoleIO.readPath("• 원본 이미지 로컬 파일 경로 입력 (취소하려면 엔터) : ", true);
            if (filePath == null) {
                System.out.println(" -> 이미지 첨부를 건너뜁니다.");
                return null;
            }
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                ConsoleIO.printlnError("파일이 존재하지 않거나 일반 파일이 아닙니다: " + filePath);
                continue;
            }
            try {
                ImageDTO imageDTO = FileUtil.readImageFromFile(filePath);
                if (imageDTO.getType() == null) {
                    ConsoleIO.printlnError("지원하지 않는 이미지 형식입니다. (PNG, JPEG, GIF, WEBP, BMP만 허용)");
                    continue;
                }
                if (imageDTO.getImageData() == null || imageDTO.getImageData().length == 0) {
                    ConsoleIO.printlnError("이미지 파일이 비어 있습니다.");
                    continue;
                }
                if (imageDTO.getImageData().length > DomainRules.MAX_IMAGE_BYTES) {
                    ConsoleIO.printlnError("이미지 크기는 최대 5MB를 초과할 수 없습니다.");
                    continue;
                }
                // 상세 화면에서 다운로드 시 원본 폴더 비교에 활용할 수 있도록 업로드 원본 디렉토리 기록
                ProjectDetailCliView.recordUploadedSourceDir(filePath.toAbsolutePath().normalize().getParent());

                System.out.printf(" -> [로드 완료] 파일명: %s | 형식: %s | 크기: %,d bytes%n",
                        imageDTO.getName(), imageDTO.getType(), imageDTO.getImageData().length);
                return imageDTO;
            } catch (IOException e) {
                ConsoleIO.printlnError("이미지 파일을 읽는 중 오류가 발생했습니다: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // 2. 내 프로젝트 목록 및 상세 관리
    // =========================================================================

    /**
     * 메이커 본인이 등록한 프로젝트 목록을 출력하고, 선택한 프로젝트에 대해
     * (1) 상세 및 리뷰 보기, (2) 펀딩 현황 보기, (3) 반려 사유 확인 및 수정·재신청 하위 메뉴를 제공
     */
    private void handleManageMyProjects() {
        LoginUser maker = ClientSession.getLoginUser();

        while (true) {
            List<ProjectQueryDTO> myProjects;
            Map<Long, Category> categoryMap = new HashMap<>();
            try {
                myProjects = new ArrayList<>(projectQueryService.getMyProjects(maker, null, null));
                myProjects.sort(Comparator.comparing(ProjectQueryDTO::getProjectId));
                // 각 프로젝트의 카테고리 표시를 위해 ProjectsDAO에서 보충 조회
                transactionManager.execute(conn -> {
                    for (ProjectDTO p : projectsDAO.findByUserId(conn, maker.userId())) {
                        categoryMap.put(p.getProjectId(), p.getCategoryName());
                    }
                    return null;
                });
            } catch (Exception e) {
                ConsoleIO.printlnError("내 프로젝트 목록을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
                return;
            }

            System.out.println();
            ConsoleIO.printDoubleLine();
            System.out.println(" [내 프로젝트 목록]");
            ConsoleIO.printSingleLine();
            System.out.println(" 번호 | 제목 | 카테고리 | 목표금액 | 승인상태 | 진행상태 | 달성률");
            ConsoleIO.printSingleLine();

            if (myProjects.isEmpty()) {
                System.out.println(" 등록한 프로젝트가 없습니다.");
                ConsoleIO.printDoubleLine();
                return;
            }

            for (ProjectQueryDTO p : myProjects) {
                Category cat = categoryMap.get(p.getProjectId());
                String catName = (cat != null) ? cat.getDisplayName() : "-";
                // 승인 전(PENDING, REJECTED)이면 진행상태와 달성률은 '-'로 표시
                boolean approved = p.getApprovalStatus() == ApprovalStatus.APPROVED;
                String fundingState = (approved && p.getProjectFundingStatus() != null)
                        ? p.getProjectFundingStatus().name()
                        : "-";
                String progressText = (approved && p.getFundProgress() != null)
                        ? String.format("%.1f%%", p.getFundProgress())
                        : "-";

                System.out.printf(" #%d | %s | %s | %s | %s | %s | %s%n",
                        p.getProjectId(),
                        p.getTitle(),
                        catName,
                        ConsoleIO.formatWon(p.getGoal()),
                        p.getApprovalStatus().name(),
                        fundingState,
                        progressText);
            }
            ConsoleIO.printDoubleLine();

            long selectedId = ConsoleIO.readLong("관리할 프로젝트 번호 입력 (0: 뒤로가기) > ");
            if (selectedId == 0L) {
                return;
            }

            ProjectQueryDTO selectedProject = myProjects.stream()
                    .filter(p -> p.getProjectId() != null && p.getProjectId() == selectedId)
                    .findFirst()
                    .orElse(null);

            if (selectedProject == null) {
                ConsoleIO.printlnError("본인이 등록한 프로젝트 번호만 선택할 수 있습니다.");
                continue;
            }

            showSingleProjectManageSubMenu(selectedProject);
        }
    }

    // 선택한 내 프로젝트 1건에 대한 관리 하위 메뉴
    private void showSingleProjectManageSubMenu(ProjectQueryDTO selectedProject) {
        long projectId = selectedProject.getProjectId();
        while (true) {
            System.out.println();
            System.out.printf("---------------- [프로젝트 #%d (%s) 관리 메뉴] ----------------%n",
                    projectId, selectedProject.getTitle());
            System.out.println(" 1. 프로젝트 상세 보기 및 리뷰(최신순)/이미지 다운로드 [평가표 5, 14]");
            System.out.println(" 2. 프로젝트별 펀딩 현황 (총금액, 리워드 신청/잔여수량, 서포터·배송지) [평가표 4]");
            System.out.println(" 3. 반려(REJECTED) 프로젝트 사유 확인 및 수정·재신청 [평가표 3]");
            System.out.println(" 0. 내 프로젝트 목록으로 돌아가기");
            ConsoleIO.printSingleLine();

            int subChoice = ConsoleIO.readIntInRange("선택 > ",0, 3);
            switch (subChoice) {
                case 1 -> projectDetailCliView.showProjectDetail(projectId);
                case 2 -> handleShowFundingStatus(projectId);
                case 3 -> {
                    if (handleResubmitRejectedProject(projectId)) {
                        return; // 재신청 성공 시 목록으로 복귀하여 변경된 상태(PENDING) 확인
                    }
                }
                case 0 -> {
                    return;
                }
            }
        }
    }

    // 프로젝트별 펀딩 현황(총 펀딩 금액, 리워드별 신청/잔여수량, 서포터 및 배송지)을 조회하여 출력
    private void handleShowFundingStatus(long projectId) {
        LoginUser maker = ClientSession.getLoginUser();
        try {
            FundingStatusDTO status = fundingStatusService.getFundingStatus(maker, projectId);
            ProjectQueryDTO p = status.getProject();

            System.out.println();
            ConsoleIO.printDoubleLine();
            System.out.printf(" [프로젝트별 펀딩 현황] #%d %s%n", p.getProjectId(), p.getTitle());
            ConsoleIO.printDoubleLine();
            System.out.printf("• 승인 상태 : %s | 진행 상태 : %s%n",
                    p.getApprovalStatus().name(),
                    p.getApprovalStatus() == ApprovalStatus.APPROVED && p.getProjectFundingStatus() != null
                            ? p.getProjectFundingStatus().name()
                            : "-");
            System.out.printf("• 총 펀딩 금액 : %s / 목표 금액 : %s (달성률: %.1f%%, 참여 서포터: %d명)%n",
                    ConsoleIO.formatWon(p.getSoldPrice()),
                    ConsoleIO.formatWon(p.getGoal()),
                    p.getFundProgress() != null ? p.getFundProgress() : 0.0,
                    p.getSupporterCount() != null ? p.getSupporterCount() : 0L);

            System.out.println();
            System.out.println("---------------- [리워드별 신청 / 잔여 수량] ----------------");
            System.out.println(" 리워드ID | 리워드명 | 단가 | 초기제공수량 | 신청수량(SUM) | 잔여수량");
            ConsoleIO.printSingleLine();
            List<RewardStatusDTO> rewards = status.getRewards();
            if (rewards == null || rewards.isEmpty()) {
                System.out.println(" 등록된 리워드가 없습니다.");
            } else {
                for (RewardStatusDTO r : rewards) {
                    String initialStock = r.isUnlimited() ? "무제한(0)" : r.getStock() + "개";
                    String remainingStock = r.isUnlimited() ? "무제한" : r.getRemainingQuantity() + "개";
                    System.out.printf(" #%d | %s | %s | %s | %d개 | %s%n",
                            r.getRewardId(),
                            r.getName(),
                            ConsoleIO.formatWon(r.getPrice()),
                            initialStock,
                            r.getAppliedQuantity() != null ? r.getAppliedQuantity() : 0L,
                            remainingStock);
                }
            }

            System.out.println();
            System.out.println("---------------- [후원 서포터 및 배송지 목록] ----------------");
            System.out.println(" 후원ID | 서포터 이름 | 배송지(주소) | 선택 리워드 | 수량 | 후원금액 | 후원일시");
            ConsoleIO.printSingleLine();
            List<FundingSupporterDTO> supporters = status.getSupporters();
            if (supporters == null || supporters.isEmpty()) {
                System.out.println(" 아직 참여한 서포터가 없습니다.");
            } else {
                for (FundingSupporterDTO s : supporters) {
                    System.out.printf(" #%d | %s | %s | %s | %d개 | %s | %s%n",
                            s.getFundId(),
                            s.getName(),
                            s.getAddress(),
                            s.getRewardName(),
                            s.getQuantity(),
                            ConsoleIO.formatWon(s.getAmount()),
                            DateTimeUtil.formatDateTime(s.getDate()));
                }
            }
            ConsoleIO.printDoubleLine();
        } catch (BusinessException e) {
            ConsoleIO.printlnError(e.getMessage());
        } catch (Exception e) {
            ConsoleIO.printlnError("펀딩 현황 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 반려(REJECTED) 상태인 프로젝트의 최신 거절 사유를 출력하고, 수정 정보를 입력받아 재신청
     *
     * @return 재신청까지 완료되었으면 true, 아니면 false
     */
    private boolean handleResubmitRejectedProject(long projectId) {
        LoginUser maker = ClientSession.getLoginUser();

        ProjectDTO existing;
        try {
            existing = projectService.getProjectDetail(maker, projectId).getProject();
        } catch (Exception e) {
            ConsoleIO.printlnError("프로젝트 정보를 조회할 수 없습니다: " + e.getMessage());
            return false;
        }

        if (existing.getApprovalStatus() != ApprovalStatus.REJECTED) {
            ConsoleIO.printlnError("반려(REJECTED) 상태인 프로젝트만 수정 및 재신청할 수 있습니다. (현재 상태: "
                    + existing.getApprovalStatus().name() + ")");
            return false;
        }

        RejectDTO latestReject = findLatestRejectReason(projectId);
        System.out.println();
        ConsoleIO.printDoubleLine();
        System.out.printf(" [반려된 프로젝트 #%d 거절 사유 확인]%n", projectId);
        ConsoleIO.printSingleLine();
        if (latestReject != null) {
            System.out.println("• 최근 거절 사유 : " + latestReject.getReason());
            System.out.println("• 거절 일시 : " + DateTimeUtil.formatDateTime(latestReject.getCreatedAt()));
        } else {
            System.out.println("• 등록된 거절 사유 이력이 없습니다.");
        }
        ConsoleIO.printDoubleLine();

        if (!ConsoleIO.readYesNo("• 프로젝트를 수정하여 재신청하시겠습니까? (y/n) : ")) {
            return false;
        }

        System.out.println();
        System.out.println("---------------- [Step 1. 수정할 프로젝트 기본 정보 입력] ----------------");
        ProjectDTO updatedProject = promptProjectBasicInfo();
        updatedProject.setProjectId(projectId);

        System.out.println();
        System.out.println("---------------- [Step 2. 교체할 리워드 및 대표 이미지 입력 (최소 2개)] ----------------");
        List<RewardDTO> newRewards = new ArrayList<>();
        Map<Integer, ImageDTO> newRewardImages = new HashMap<>();
        promptRewardsAndImages(newRewards, newRewardImages);

        ConsoleIO.printSingleLine();
        if (!ConsoleIO.readYesNo("• 위 내용으로 수정 및 재신청하시겠습니까? (y/n) : ")) {
            System.out.println("=> 수정 및 재신청이 취소되었습니다.");
            return false;
        }

        try {
            projectService.resubmitRejectedProject(maker, updatedProject, newRewards, newRewardImages);
            ConsoleIO.printlnSuccess(String.format(
                    "프로젝트 #%d 수정 및 재신청이 완료되었습니다! (현재 상태: PENDING)", projectId));
            return true;
        } catch (BusinessException e) {
            ConsoleIO.printlnError(e.getMessage());
            return false;
        } catch (Exception e) {
            ConsoleIO.printlnError("프로젝트 재신청 중 오류가 발생했습니다: " + e.getMessage());
            return false;
        }
    }

    // reject 테이블에서 해당 프로젝트의 가장 최근 거절 사유를 조회
    private RejectDTO findLatestRejectReason(long projectId) {
        return transactionManager.execute(conn -> {
            try (ResultSet rs = SQLSelect.allColumns(new DirectReferenceTable(Tables.REJECT))
                    .addCondition(new BasicCondition(
                            new ColumnValueToken(RejectDAO.Columns.PROJECT_ID),
                            ArithmeticConditionOperator.EQ,
                            new UserValueToken(projectId)))
                    .orderBy(
                            new SimpleOrderBySelector(RejectDAO.Columns.CREATED_AT, false),
                            new SimpleOrderBySelector(RejectDAO.Columns.REJECTREASON_ID, false))
                    .execute(conn)) {
                if (rs.next()) {
                    RejectDTO dto = new RejectDTO();
                    dto.setRejectReasonId(rs.getLong(RejectDAO.Columns.REJECTREASON_ID.toString()));
                    dto.setProjectId(rs.getLong(RejectDAO.Columns.PROJECT_ID.toString()));
                    dto.setReason(rs.getString(RejectDAO.Columns.REASON.toString()));
                    dto.setCreatedAt(rs.getObject(RejectDAO.Columns.CREATED_AT.toString(), LocalDateTime.class));
                    return dto;
                }
                return null;
            }
        });
    }
}