package ypfunding.server.check.function;

import ypfunding.common.dto.ProjectDTO;
import ypfunding.common.dto.RewardDTO;
import ypfunding.common.type.Category;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.bootstrap.AppContext;
import ypfunding.server.check.CheckBase;
import ypfunding.server.check.CheckUsers;
import ypfunding.server.service.ProjectService;
import ypfunding.server.service.RewardService;

import java.util.List;

public class MakerCheck extends CheckBase {

    // =================================================================
    // 시연용 상수 정의 (최상단 분리 — seed.sql 변동 시 여기만 수정)
    // =================================================================

    // [02번 시연용] 프로젝트 기본 정보 등록 상수
    private static final String TEST02_PROJECT_TITLE = "[시연02] 초경량 휴대용 에스프레소 머신";
    private static final String TEST02_PROJECT_DESC = "캠핑과 여행지에서 캡슐과 원두 모두 추출 가능한 포터블 머신";
    private static final long TEST02_PROJECT_GOAL = 1_500_000L;
    private static final int TEST02_PROJECT_DURATION = 30;
    //이거 나중에 카테고리 1개 되면 줄여야함!!
    private static final List<Category> TEST02_CATEGORIES = List.of(
            Category.TECH_HOME_APPLIANCES,
            Category.HOME_LIVING
    );

    // [03번 시연용] 리워드 등록 대상 프로젝트 PK
    // 빠른 테스트를 위해 이미 등록된 거 기준으로 갈 생각이긴한데 2 -> 3 이어지게 하려면
    // DB까서 프로젝트 코드 확인하고 시연전에 바꿔줘야함
    private static final long TEST03_TARGET_PROJECT_ID = 5L;

    // [03번 시연용] 리워드 1 (수량 제한 리워드)
    private static final String TEST03_REWARD1_NAME = "[슈퍼얼리버드] 한정판 풀패키지";
    private static final long TEST03_REWARD1_PRICE = 59_000L;
    private static final String TEST03_REWARD1_DESC = "본품 1개 + 전용 하드케이스 + 원두 200g";
    private static final Long TEST03_REWARD1_STOCK = 20L;

    // [03번 시연용] 리워드 2 (무제한 리워드: STOCK = NULL)
    private static final String TEST03_REWARD2_NAME = "[얼리버드] 기본 단품 구성 (무제한)";
    private static final long TEST03_REWARD2_PRICE = 69_000L;
    private static final String TEST03_REWARD2_DESC = "본품 1개 + 기본 파우치";
    private static final Long TEST03_REWARD2_STOCK = null;

    public MakerCheck(AppContext context, CheckUsers users) {
        super(context, users);
    }

    /*
     * [02번 시연]
     *
     * -- 1) 등록된 프로젝트 기본 정보 확인 쿼리
     * SELECT project_id, user_id, title, description, goal, duration, start_date, end_date, approval_status
     *   FROM projects
     *  ORDER BY project_id DESC;
     *
     * -- 2) 등록된 프로젝트의 카테고리 확인 쿼리
     * SELECT c.category_id, c.project_id, p.title, c.category_name
     *   FROM category c
     *   JOIN projects p ON c.project_id = p.project_id
     *  ORDER BY c.project_id DESC, c.category_id ASC;
     */
    public void test02RegisterProject() {
        LoginUser maker = users.maker();
        ProjectService projectService = context.getProjectService();

        ProjectDTO projectDTO = new ProjectDTO();
        projectDTO.setTitle(TEST02_PROJECT_TITLE);
        projectDTO.setDescription(TEST02_PROJECT_DESC);
        projectDTO.setGoal(TEST02_PROJECT_GOAL);
        projectDTO.setDuration(TEST02_PROJECT_DURATION);

        projectService.createProject(maker, projectDTO, TEST02_CATEGORIES);
    }

    /*
     * [03번 시연]
     *
     * -- 1) 대상 프로젝트에 등록된 리워드 확인 쿼리(한정 수량 20 및 무제한 NULL 확인)
     * SELECT r.reward_id, r.project_id, p.title, p.approval_status, r.name, r.price, r.description, r.stock
     *   FROM rewards r
     *   JOIN projects p ON r.project_id = p.project_id
     *  WHERE r.project_id = 5
     *  ORDER BY r.reward_id ASC;
     */
    public void test03RegisterReward() {
        LoginUser maker = users.maker();
        RewardService rewardService = context.getRewardService();

        RewardDTO limitedReward = new RewardDTO();
        limitedReward.setName(TEST03_REWARD1_NAME);
        limitedReward.setPrice(TEST03_REWARD1_PRICE);
        limitedReward.setDescription(TEST03_REWARD1_DESC);
        limitedReward.setStock(TEST03_REWARD1_STOCK);

        RewardDTO unlimitedReward = new RewardDTO();
        unlimitedReward.setName(TEST03_REWARD2_NAME);
        unlimitedReward.setPrice(TEST03_REWARD2_PRICE);
        unlimitedReward.setDescription(TEST03_REWARD2_DESC);
        unlimitedReward.setStock(TEST03_REWARD2_STOCK);

        rewardService.createRewards(
                maker,
                TEST03_TARGET_PROJECT_ID,
                List.of(limitedReward, unlimitedReward)
        );
    }
}