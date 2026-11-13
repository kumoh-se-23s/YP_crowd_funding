package ypfunding.client.view;

import ypfunding.client.session.ClientSession;
import ypfunding.client.util.ConsoleIO;
import ypfunding.common.dto.FundingSupporterDTO;
import ypfunding.common.dto.ProjectDTO;
import ypfunding.common.dto.ProjectQueryDTO;
import ypfunding.common.dto.RewardStatusDTO;
import ypfunding.common.dto.UserDTO;
import ypfunding.common.type.AdminSortType;
import ypfunding.common.type.ApprovalStatus;
import ypfunding.common.type.Category;
import ypfunding.common.util.DateTimeUtil;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.bootstrap.AppContext;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.*;
import ypfunding.server.persistence.dao.dynamicsql.builder.SQLInsert;
import ypfunding.server.persistence.dao.dynamicsql.col.SimpleOrderBySelector;
import ypfunding.server.service.AdminService;
import ypfunding.server.service.ProjectQueryService;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자(ADMIN) 전용 CLI 화면 (B 담당 — 평가표 6, 7, 8, 9).
 * - [6] 신청 대기(PENDING) 프로젝트 목록 조회 및 승인 처리 (START_DATE = 현재 시각)
 * - [7] 신청 대기(PENDING) 프로젝트 거절 및 거절 사유 작성
 * - [8·9] 승인된 전체 프로젝트 펀딩 현황 조회(기본 최신순 START_DATE DESC) 및 다중 기준 오름/내림차순 정렬,
 *         특정 프로젝트의 개설자(메이커) 및 후원자 상세 정보 조회
 */
public class AdminCliView {

    private final AppContext context;
    private final AdminService adminService;
    private final ProjectQueryService projectQueryService;
    private final TransactionManager transactionManager;
    private final ProjectsDAO projectsDAO;
    private final UsersDAO usersDAO;
    private final FundingStatusDAO fundingStatusDAO;
    private final ProjectDetailCliView projectDetailCliView;

    public AdminCliView(AppContext context, ProjectDetailCliView projectDetailCliView) {
        this.context = context;
        this.adminService = context.getAdminService();
        this.projectQueryService = context.getProjectQueryService();
        this.transactionManager = context.getTransactionManager();
        this.projectsDAO = new ProjectsDAO();
        this.usersDAO = new UsersDAO();
        this.fundingStatusDAO = new FundingStatusDAO();
        this.projectDetailCliView = projectDetailCliView;
    }

    // 관리자 메인 메뉴를 출력하고 선택한 기능을 수행
    public void showMainMenu() {
        while (ClientSession.isLoggedIn()) {
            System.out.println();
            ConsoleIO.printDoubleLine();
            System.out.printf(" [관리자 메인 메뉴] 로그인: %s (%s)%n",
                    ClientSession.getUserDto().getName(),
                    ClientSession.getUserType().name());
            ConsoleIO.printDoubleLine();
            System.out.println(" 1. 신청 대기(PENDING) 프로젝트 심사 (승인 / 거절 및 사유 작성) [평가표 6, 7]");
            System.out.println(" 2. 승인된 프로젝트 전체 펀딩 현황 조회 및 정렬 [평가표 8, 9]");
            System.out.println(" 0. 로그아웃");
            ConsoleIO.printSingleLine();

            int choice = ConsoleIO.readIntInRange("선택 > ",0,2);
            switch (choice) {
                case 1 -> handlePendingProjectsReview();
                case 2 -> handleApprovedProjectsFundingStatus();
                case 0 -> {
                    ClientSession.logout();
                    System.out.println("=> 로그아웃되었습니다.");
                    return;
                }
            }
        }
    }

    // =========================================================================
    // 1. 신청 대기(PENDING) 프로젝트 심사:
    // =========================================================================

    // 승인 대기(PENDING) 프로젝트 목록을 조회하고 특정 프로젝트를 선택하여 상세 확인, 승인, 거절 처리를 수행
    private void handlePendingProjectsReview() {
        LoginUser admin = ClientSession.getLoginUser();

        while (true) {
            List<ProjectQueryDTO> pendingList;
            Map<Long, Category> categoryMap = new HashMap<>();
            try {
                pendingList = new ArrayList<>(projectQueryService.getAllPendingProjects(
                        admin, new SimpleOrderBySelector(ProjectsDAO.Columns.PROJECT_ID, true)));
                transactionManager.execute(conn -> {
                    for (ProjectDTO p : projectsDAO.getAllProjects(conn)) {
                        categoryMap.put(p.getProjectId(), p.getCategoryName());
                    }
                    return null;
                });
            } catch (Exception e) {
                ConsoleIO.printlnError("승인 대기 목록을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
                return;
            }

            System.out.println();
            System.out.println("================ [승인 대기(PENDING) 프로젝트 목록] ================");
            System.out.println(" 번호 | 메이커ID | 제목 | 카테고리 | 목표금액 | 기간");
            System.out.println("--------------------------------------------------------------------");

            if (pendingList.isEmpty()) {
                System.out.println(" 현재 승인 대기(PENDING) 중인 프로젝트가 없습니다.");
                System.out.println("====================================================================");
                return;
            }

            for (ProjectQueryDTO p : pendingList) {
                Category cat = categoryMap.get(p.getProjectId());
                String catName = (cat != null) ? cat.getDisplayName() : "-";
                System.out.printf(" #%d | %d | %s | %s | %s | %d일%n",
                        p.getProjectId(),
                        p.getUserId(),
                        p.getTitle(),
                        catName,
                        ConsoleIO.formatWon(p.getGoal()),
                        p.getDuration());
            }
            System.out.println("====================================================================");

            long projectId = ConsoleIO.readLong("심사할 프로젝트 번호 입력 (0: 뒤로가기) > ");
            if (projectId == 0L) {
                return;
            }

            ProjectQueryDTO selected = pendingList.stream()
                    .filter(p -> p.getProjectId() != null && p.getProjectId() == projectId)
                    .findFirst()
                    .orElse(null);

            if (selected == null) {
                ConsoleIO.printlnError("승인 대기 목록에 있는 프로젝트 번호만 선택할 수 있습니다.");
                continue;
            }

            reviewSinglePendingProject(projectId);
        }
    }

    // 선택한 PENDING 프로젝트 1건에 대해 상세 확인, 승인(, 거절을 수행
    private void reviewSinglePendingProject(long projectId) {
        LoginUser admin = ClientSession.getLoginUser();

        while (true) {
            System.out.println();
            System.out.println(" 1. 프로젝트 상세 및 리워드 확인");
            System.out.println(" 2. 프로젝트 승인 (APPROVED 전환 및 시작일 기록)");
            System.out.println(" 3. 프로젝트 거절 (REJECTED 전환 및 거절 사유 등록)");
            System.out.println(" 0. 뒤로가기");

            int choice = ConsoleIO.readIntInRange("선택 > ",0,3);
            switch (choice) {
                case 1 -> projectDetailCliView.showProjectDetail(projectId);
                case 2 -> {
                    try {
                        adminService.approveProject(admin, projectId);
                        ConsoleIO.printlnSuccess(String.format(
                                "프로젝트 #%d가 승인(APPROVED) 처리되었으며 펀딩이 시작되었습니다.", projectId));
                        return;
                    } catch (BusinessException e) {
                        ConsoleIO.printlnError(e.getMessage());
                        return;
                    } catch (Exception e) {
                        ConsoleIO.printlnError("프로젝트 승인 중 오류가 발생했습니다: " + e.getMessage());
                        return;
                    }
                }
                case 3 -> {
                    String reason = ConsoleIO.readNonBlankString("• 거절 사유 입력 : ");
                    try {
                        rejectPendingProjectSafely(admin, projectId, reason);
                        ConsoleIO.printlnSuccess(String.format(
                                "프로젝트 #%d가 거절(REJECTED) 처리되었으며 사유가 저장되었습니다.", projectId));
                        return;
                    } catch (BusinessException e) {
                        ConsoleIO.printlnError(e.getMessage());
                        return;
                    } catch (Exception e) {
                        ConsoleIO.printlnError("프로젝트 거절 처리 중 오류가 발생했습니다: " + e.getMessage());
                        return;
                    }
                }
                case 0 -> {
                    return;
                }
            }
        }
    }

    // 신청 대기 프로젝트를 REJECTED로 상태 전이하고 reject 테이블에 거절 사유를 한 트랜잭션으로 저장
    private void rejectPendingProjectSafely(LoginUser admin, long projectId, String reason) {
        LocalDateTime now = LocalDateTime.now(context.getClock());
        transactionManager.execute(conn -> {
            int updated = projectsDAO.rejectProjectUpdate(conn, projectId);
            if (updated != 1) {
                throw new BusinessException("승인 대기(PENDING) 상태인 프로젝트만 거절할 수 있습니다.");
            }
            new SQLInsert(Tables.REJECT)
                    .addTarget(RejectDAO.Columns.PROJECT_ID, projectId)
                    .addTarget(RejectDAO.Columns.REASON, reason.trim())
                    .addTarget(RejectDAO.Columns.CREATED_AT, now)
                    .execute(conn);
            return null;
        });
    }

    // =========================================================================
    // 2. 승인된 전체 프로젝트 펀딩 현황 조회 및 정렬
    // =========================================================================

    /**
     * 승인된(APPROVED) 모든 프로젝트(진행·마감 포함)의 펀딩 현황을 기본 최신순(START_DATE DESC)으로 출력 후,
     * 정렬 기준(최신순 / 총 펀딩 금액순 / 달성률순 / 서포터 수순, 오름·내림차순) 변경 및
     * 특정 프로젝트의 개설자·후원자 상세 정보 조회를 제공
     */
    private void handleApprovedProjectsFundingStatus() {
        LoginUser admin = ClientSession.getLoginUser();
        AdminSortType currentSortType = AdminSortType.LATEST;
        boolean ascending = false; // 기본: 최신순 내림차순(DESC)

        while (true) {
            List<ProjectQueryDTO> approvedProjects;
            try {
                approvedProjects = new ArrayList<>(projectQueryService.getAllActiveOrEndedProjectsRecent(admin));
            } catch (Exception e) {
                ConsoleIO.printlnError("승인된 프로젝트 현황을 불러오는 중 오류가 발생했습니다: " + e.getMessage());
                return;
            }

            sortApprovedProjects(approvedProjects, currentSortType, ascending);

            String sortHeader = String.format("%s %s",
                    currentSortType.getDisplayName(),
                    ascending ? "오름차순(ASC)" : "내림차순(DESC)");

            System.out.println();
            System.out.printf("================ [관리자 펀딩 현황 (현재 정렬: %s)] ================%n", sortHeader);
            System.out.println(" 번호 | 제목 | 상태 | 총 펀딩 금액 | 목표 금액 | 달성률 | 서포터 수 | 시작일");
            System.out.println("-------------------------------------------------------------------------------------------");

            if (approvedProjects.isEmpty()) {
                System.out.println(" 승인된 프로젝트가 없습니다.");
            } else {
                for (ProjectQueryDTO p : approvedProjects) {
                    String statusName = p.getProjectFundingStatus() != null
                            ? p.getProjectFundingStatus().name()
                            : "-";
                    double progress = p.getFundProgress() != null ? p.getFundProgress() : 0.0;
                    long supporters = p.getSupporterCount() != null ? p.getSupporterCount() : 0L;
                    System.out.printf(" #%d | %s | %s | %s | %s | %.1f%% | %d명 | %s%n",
                            p.getProjectId(),
                            p.getTitle(),
                            statusName,
                            ConsoleIO.formatWon(p.getSoldPrice()),
                            ConsoleIO.formatWon(p.getGoal()),
                            progress,
                            supporters,
                            DateTimeUtil.formatDate(p.getStartDate()));
                }
            }
            System.out.println("===========================================================================================");
            System.out.println(" [하위 메뉴]");
            System.out.println(" 1. 정렬 기준 변경 (총 펀딩 금액순 / 달성률순 / 서포터 수순 / 최신순 — 오름·내림차순) [평가표 9]");
            System.out.println(" 2. 특정 프로젝트의 개설자 및 후원자 상세 정보 조회");
            System.out.println(" 0. 뒤로가기");

            int choice = ConsoleIO.readIntInRange("선택 > ",0,2);
            switch (choice) {
                case 1 -> {
                    int colChoice = ConsoleIO.readIntInRange(
                            "• 정렬 컬럼 선택 (1: 최신순, 2: 총 펀딩 금액순, 3: 달성률순, 4: 서포터 수순) : ", 1, 4);
                    int dirChoice = ConsoleIO.readIntInRange(
                            "• 정렬 방향 선택 (1: 내림차순 DESC, 2: 오름차순 ASC) : ", 1, 2);

                    currentSortType = switch (colChoice) {
                        case 1 -> AdminSortType.LATEST;
                        case 2 -> AdminSortType.TOTAL_AMOUNT;
                        case 3 -> AdminSortType.ACHIEVEMENT_RATE;
                        default -> AdminSortType.SUPPORT_COUNT;
                    };
                    ascending = (dirChoice == 2);
                }
                case 2 -> {
                    long projectId = ConsoleIO.readLong("• 상세 조회할 프로젝트 번호 입력 (0: 취소) : ");
                    if (projectId > 0L) {
                        showMakerAndSupportersForAdmin(projectId, approvedProjects);
                    }
                }
                case 0 -> {
                    return;
                }
            }
        }
    }

    /**
     * 관리자 정렬 기준(AdminSortType)과 오름/내림차순 방향에 맞춰 승인 프로젝트 목록을 정렬한다.
     * 동점 행의 순서가 항상 일정하도록 보조 정렬 키(projectId DESC)를 마지막에 적용
     */
    private void sortApprovedProjects(
            List<ProjectQueryDTO> projects,
            AdminSortType sortType,
            boolean ascending) {
        Comparator<ProjectQueryDTO> primary = switch (sortType) {
            case LATEST -> Comparator.comparing(
                    ProjectQueryDTO::getStartDate,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            case TOTAL_AMOUNT -> Comparator.comparingLong(
                    p -> p.getSoldPrice() != null ? p.getSoldPrice() : 0L);
            case ACHIEVEMENT_RATE -> Comparator.comparingDouble(
                    p -> p.getFundProgress() != null ? p.getFundProgress() : 0.0);
            case SUPPORT_COUNT -> Comparator.comparingLong(
                    p -> p.getSupporterCount() != null ? p.getSupporterCount() : 0L);
        };

        if (!ascending) {
            primary = primary.reversed();
        }
        // 보조 정렬 키: project_id DESC
        Comparator<ProjectQueryDTO> withTieBreaker = primary.thenComparing(
                Comparator.comparingLong(ProjectQueryDTO::getProjectId).reversed());

        projects.sort(withTieBreaker);
    }

    // 특정 승인 프로젝트의 개설자(메이커) 정보, 리워드별 수량 현황, 후원자 및 배송지 목록을 상세 출력
    private void showMakerAndSupportersForAdmin(long projectId, List<ProjectQueryDTO> approvedProjects) {
        ProjectQueryDTO target = approvedProjects.stream()
                .filter(p -> p.getProjectId() != null && p.getProjectId() == projectId)
                .findFirst()
                .orElse(null);

        if (target == null) {
            ConsoleIO.printlnError("승인된 프로젝트 목록에 존재하지 않는 프로젝트 번호입니다.");
            return;
        }

        try {
            transactionManager.execute(conn -> {
                UserDTO maker = usersDAO.getUserById(conn, target.getUserId());
                List<RewardStatusDTO> rewards = fundingStatusDAO.findRewardStatuses(conn, projectId);
                List<FundingSupporterDTO> supporters = fundingStatusDAO.findSupporters(conn, projectId);

                System.out.println();
                ConsoleIO.printDoubleLine();
                System.out.printf(" [프로젝트 #%d 개설자 및 후원자 상세 정보] %s%n",
                        target.getProjectId(), target.getTitle());
                ConsoleIO.printDoubleLine();
                if (maker != null) {
                    System.out.printf("• 개설자(메이커) : %s (아이디: %s, 회원번호: #%d)%n",
                            maker.getName(), maker.getLoginId(), maker.getUserId());
                    System.out.printf("• 개설자 주소 : %s%n", maker.getAddress());
                }
                System.out.printf("• 펀딩 상태 : %s | 총 펀딩 금액 : %s / 목표 금액 : %s (달성률: %.1f%%, 서포터: %d명)%n",
                        target.getProjectFundingStatus() != null ? target.getProjectFundingStatus().name() : "-",
                        ConsoleIO.formatWon(target.getSoldPrice()),
                        ConsoleIO.formatWon(target.getGoal()),
                        target.getFundProgress() != null ? target.getFundProgress() : 0.0,
                        target.getSupporterCount() != null ? target.getSupporterCount() : 0L);

                System.out.println();
                System.out.println("---------------- [리워드별 신청 / 잔여 수량] ----------------");
                for (RewardStatusDTO r : rewards) {
                    String stockInfo = r.isUnlimited() ? "무제한(0)" : r.getStock() + "개";
                    String remainInfo = r.isUnlimited() ? "무제한" : r.getRemainingQuantity() + "개";
                    System.out.printf(" • [리워드 #%d] %s | 단가: %s | 제공수량: %s | 신청수량: %d개 | 잔여수량: %s%n",
                            r.getRewardId(),
                            r.getName(),
                            ConsoleIO.formatWon(r.getPrice()),
                            stockInfo,
                            r.getAppliedQuantity() != null ? r.getAppliedQuantity() : 0L,
                            remainInfo);
                }

                System.out.println();
                System.out.println("---------------- [후원자(서포터) 및 배송지 목록] ----------------");
                if (supporters.isEmpty()) {
                    System.out.println(" 아직 후원한 서포터가 없습니다.");
                } else {
                    for (FundingSupporterDTO s : supporters) {
                        System.out.printf(" • [후원 #%d] 서포터: %s | 배송지: %s | 리워드: %s (%d개, %s) | 일시: %s%n",
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
                return null;
            });
        } catch (Exception e) {
            ConsoleIO.printlnError("개설자 및 후원자 정보를 조회하는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}