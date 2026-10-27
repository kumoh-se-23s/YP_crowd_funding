package ypfunding.server.check.function;

import ypfunding.server.auth.LoginUser;
import ypfunding.server.check.CheckBase;
import ypfunding.server.bootstrap.AppContext;
import ypfunding.server.check.CheckUsers;
import ypfunding.server.service.FundService;
import ypfunding.server.service.ReviewService;

//D 담당 - 08, 09, 10번
public class FundingCheck extends CheckBase {
    //리워드 신청
    private static final long APPLY_REWARD_ID = 1L;
    private static final long APPLY_OTHER_REWARD_ID = 2L;
    private static final int DUPLICATE_QUANTITY = 1;
    private static final int APPLY_QUANTITY = 2;

    //리뷰 등록
    //마감, 성공 프로젝트에 supporter1이 seed로 해둔 후원
    private static final long REVIEW_SUCCESS_FUND_ID = 1L;
    //마감, 실패 프로젝트에 supporter1이 seed로 해둔 후원
    private static final long REVIEW_FAILED_FUND_ID = 2L;
    private static final int REVIEW_STAR = 5;
    private static final String REVIEW_CONTENT = "가격이 저렴하고 품질이 좋아요";

    //제공 가능 수량
    //진행 중 프로젝트의 stock 3 리워드. supporter1, supporter2 모두 이 프로젝트에 후원 없음
    private static final long STOCK_REWARD_ID = 5L;
    private static final int ENOUGH_QUANTITY = 2;
    private static final int SHORTAGE_QUANTITY = 2;

    public FundingCheck(AppContext context, CheckUsers users) {
        super(context, users);
    }

    /*
    [실행 전, 후] 리워드 수량이 줄었는지 확인
    //SELECT r.reward_id, r.name, r.stock, r.stock - (SELECT COALESCE(SUM(f.quantity), 0) FROM funds f WHERE f.reward_id = r.reward_id) AS remaining FROM rewards r WHERE r.reward_id = 1;
    [실행 후] 후원 기록이 생겼는지 확인
    //SELECT f.fund_id, u.login_id, f.project_id, f.reward_id, f.quantity, f.date FROM funds f JOIN users u ON u.user_id = f.user_id WHERE u.login_id = 'supporter1' ORDER BY f.fund_id DESC;
    [실행 후] 메이커 기준 누가 후원했는지 확인
    //SELECT p.project_id, p.title, su.login_id AS supporter, su.name, r.name AS reward_name, f.quantity, r.price * f.quantity AS amount, f.date FROM funds f JOIN projects p ON p.project_id = f.project_id JOIN users mu ON mu.user_id = p.user_id JOIN users su ON su.user_id = f.user_id JOIN rewards r ON r.reward_id = f.reward_id WHERE mu.login_id = 'maker1' ORDER BY f.date DESC;
    */
    public void test08ApplyReward() {
        LoginUser supporter = users.supporter();
        FundService fundService = context.getFundService();

        //정상 신청 -> 후원 기록 생성, 리워드 수량 감소
        fundService.applyFund(supporter, APPLY_REWARD_ID, APPLY_QUANTITY);

        //같은 프로젝트에 다른 리워드 신청 -> 한 프로젝트에 한 리워드만 신청 가능으로 예외 발생
        fundService.applyFund(supporter, APPLY_OTHER_REWARD_ID, DUPLICATE_QUANTITY);
    }

    // [실행 후] 리뷰 목록
    //SELECT u.login_id, p.title, r.name AS reward_name, rv.star, rv.content, rv.date FROM reviews rv JOIN funds f ON f.fund_id = rv.fund_id JOIN users u ON u.user_id = f.user_id JOIN projects p ON p.project_id = f.project_id JOIN rewards r ON r.reward_id = f.reward_id ORDER BY rv.date DESC;
    public void test09RegisterReview() {
        LoginUser supporter = users.supporter();
        ReviewService reviewService = context.getReviewService();

        //마감 + 성공 프로젝트 -> 등록
        reviewService.writeReview(supporter, REVIEW_SUCCESS_FUND_ID, REVIEW_STAR, REVIEW_CONTENT);

        //마감 + 실패 프로젝트 -> 예외 발생
        reviewService.writeReview(supporter, REVIEW_FAILED_FUND_ID, REVIEW_STAR, REVIEW_CONTENT);
    }

    // [실행 전·후] 리워드 남은 수량 (3 → 1, 실패 후에도 1)
    //SELECT r.reward_id, r.name, r.stock, r.stock - (SELECT COALESCE(SUM(f.quantity), 0) FROM funds f WHERE f.reward_id = r.reward_id) AS remaining FROM rewards r WHERE r.reward_id = 5;
    // [실행 후] 후원 기록 (supporter1 행만 있고 supporter2 행은 없어야 함)
    //SELECT f.fund_id, u.login_id, f.reward_id, f.quantity, f.date FROM funds f JOIN users u ON u.user_id = f.user_id WHERE f.reward_id = 5 ORDER BY f.date;
    public void test10ConcurrencyControl() {
        LoginUser supporter = users.supporter();
        LoginUser supporter2 = users.supporter2();
        FundService fundService = context.getFundService();

        //수량 충분 -> 기록 남음, 남은 수량 감소
        fundService.applyFund(supporter, STOCK_REWARD_ID, ENOUGH_QUANTITY);

        //수량 부족 -> 예외 발생
        fundService.applyFund(supporter2, STOCK_REWARD_ID, SHORTAGE_QUANTITY);
    }
}
