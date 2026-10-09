package ypfunding.common.constant;

//개발용 상수 모음
//다 static이니까 new로 생성자 선언하지 말것!!!!!!!!!
//너무 많아진다 or 쓰기 불편하다싶으면 서비스별 상수 등으로 파일분리 해도 됨
public class DomainRules {
    //프로젝트-------------------------
    //프로젝트 하나 당 최소 카테고리 수
    //TODO: 조건 정확히 파악 후 수정 필요
    public static final int MIN_CATEGORIES_PER_PROJECT = 2;
    //프로젝트 하나 당 최대 카테고리 수
    //TODO: 조건 정확히 파악 후 수정 필요
    public static final int MAX_CATEGORIES_PER_PROJECT = 2;
    //프로젝트 하나 당 최소 리워드 수
    public static final int MIN_REWARDS_PER_PROJECT = 2;

    //펀딩--------------------------------
    //구매 리워드 최소 수량 ( = ck_funds_quantity)
    public static final int MIN_FUND_QUANTITY = 1;
    //구매 리워드 최대 수량
    public static final int MAX_FUND_QUANTITY = 10;

    //리뷰---------------------------------
    //별점 최소값 ( = ck_reviews_star)
    public static final int MIN_STAR = 1;
    //별점 최댓값 ( = ck_reviews_star)
    public static final int MAX_STAR = 5;

    //리워드 이미지----------------------------
    //이미지 최대 크기
    //TODO: DB에서 16MB로 제한되어있긴한데 통신 부담 보면서 조절하기 위해 상수화해서 자바에서도 관리
    public static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024; //5MB

    //문자열 최대 길이 ( = VARCHAR 길이)-------------
    //로그인 id ( = users.login_id )
    public static final int MAX_LOGIN_ID_LENGTH = 30;
    //유저네임 ( = users.name )
    public static final int MAX_USER_NAME_LENGTH = 30;
    //주소 ( = users.address )
    public static final int MAX_ADDRESS_LENGTH = 200;
    //프로젝트 제목 ( = projectes.title )
    public static final int MAX_PROJECT_TITLE_LENGTH = 100;
    //리워드 이름 ( = rewards.name )
    public static final int MAX_REWARD_NAME_LENGTH = 50;
    //이미지 이름 ( = image.name )
    public static final int MAX_IMAGE_NAME_LENGTH = 255;

}
