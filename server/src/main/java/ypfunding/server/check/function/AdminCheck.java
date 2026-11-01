package ypfunding.server.check.function;

import ypfunding.common.dto.ProjectQueryDTO;
import ypfunding.common.type.ApprovalStatus;
import ypfunding.common.type.Category;
import ypfunding.server.check.CheckBase;
import ypfunding.server.bootstrap.AppContext;
import ypfunding.server.check.CheckUsers;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

//C 담당 - 01, 04, 06, 07번
public class AdminCheck extends CheckBase {

    private static final int SAMPLE_PROJECT_ID = 6;
    // 07번 필터 조건
    private static final Category TEST07_CATEGORY_1 = Category.TECH_HOME_APPLIANCES;
    private static final Category TEST07_CATEGORY_2 = Category.SOCIAL;

    public AdminCheck(AppContext context, CheckUsers users) {
        super(context, users);
    }

    public void test04ApproveProject() {
        System.out.println("========= TEST 04 ========");
        context.getAdminService().approveProject(users.admin(), SAMPLE_PROJECT_ID);
    }

    public void test06ViewProjects() {
        printProjectList("전체 프로젝트 (진행 중 + 마감, 최신순)", context.getProjectQueryService().getAllActiveOrEndedProjectsRecent(users.supporter()));
    }

    public void test07FilterProjects() {
        System.out.println("========= TEST 07 ========");
        printProjectList("카테고리 검색: " + TEST07_CATEGORY_1.getDisplayName() + " (진행 중)",context.getProjectQueryService().filterActiveProjects(users.supporter(), Category.TECH_HOME_APPLIANCES));
        printProjectList("카테고리 검색: " + TEST07_CATEGORY_2.getDisplayName() + " (진행 중)",context.getProjectQueryService().filterEndedProjects(users.supporter(), Category.SOCIAL));
    }


    //6,7 출력용
    // 프로젝트 이름 | 기한 | 달성률 | 진행 상태 출력
    private void printProjectList(String title, List<ProjectQueryDTO> projects) {
        System.out.println("[" + title + "]");
        if (projects.isEmpty()) {
            System.out.println("   (없음)");
            return;
        }

        int no = 1;
        for (ProjectQueryDTO project : projects) {
            System.out.printf("   %d. %s | %s | 달성률 %.1f%% | %s%n",
                    no++,
                    project.getTitle(),
                    deadline(project),
                    project.getFundProgress(),
                    statusLabel(project));
        }
    }

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    // 종료일 -> "2026-11-17까지". 승인 전이라 종료일이 없으면 "-"
    private static String deadline(ProjectQueryDTO project) {
        return project.getEndDate() == null ? "-" : DATE_FORMAT.format(project.getEndDate()) + "까지";
    }

    // 승인 전이면 승인 상태(신청/반려), 승인됐으면 펀딩 상태(진행 중/펀딩 성공/펀딩 실패)
    private static String statusLabel(ProjectQueryDTO project) {
        if (project.getApprovalStatus() != ApprovalStatus.APPROVED) {
            return project.getApprovalStatus().getDisplayName();
        }
        return project.getProjectFundingStatus().getDisplayName();
    }
}


