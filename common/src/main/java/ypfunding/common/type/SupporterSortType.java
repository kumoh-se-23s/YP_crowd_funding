package ypfunding.common.type;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
//서포터 프로젝트 정렬방식
public enum SupporterSortType {
    LATEST("최신순"),
    AMOUNT("모집금액순"),
    ENDSOON("마감임박순"),
    LIKES("좋아요순");

    private final String displayName;
}
