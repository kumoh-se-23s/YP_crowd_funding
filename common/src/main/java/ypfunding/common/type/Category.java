package ypfunding.common.type;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
//카테고리 종류
public enum Category {
    TECH_HOME_APPLIANCES("테크·가전"),
    HOME_LIVING("홈·리빙"),
    BEAUTY("뷰티"),
    FASHION("패션"),
    FOOD("푸드"),
    BOOK("출판"),
    KIDS("키즈"),
    SPORTS("스포츠"),
    GOODS("굿즈"),
    TRAVEL("여행"),
    FANDOM("팬덤"),
    PET("반려동물"),
    DESIGN("디자인"),
    ART("예술"),
    CAR("자동차"),
    GAME("게임"),
    MOVIE("영화"),
    MUSIC("음악"),
    PHOTO("사진"),
    WEBTOON("웹툰"),
    MEMBERSHIP("멤버십"),
    SOCIAL("소셜");

    private final String displayName;
}
