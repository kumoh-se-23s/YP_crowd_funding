package ypfunding.common.type;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
//관리자 프로젝트 정렬방식
public enum AdminSortType {
    LATEST("최신순"),
    TOTAL_AMOUNT("총 펀딩 금액순"),
    ACHIEVEMENT_RATE("달성순"),
    SUPPORT_COUNT("서포터 수순");

    private final String displayName;
}
