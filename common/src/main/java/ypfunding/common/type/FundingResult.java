package ypfunding.common.type;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
//펀딩 상태
public enum FundingResult {
    ONGOING("진행 중"),
    SUCCESS("펀딩 성공"),
    FAILED("펀딩 실패");

    private final String displayName;
}
