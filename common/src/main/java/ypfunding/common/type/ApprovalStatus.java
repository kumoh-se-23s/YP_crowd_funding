package ypfunding.common.type;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
//프로젝트 승인 상태
public enum ApprovalStatus {
    PENDING("신청"),
    APPROVED("승인"),
    REJECTED("반려");

    private final String displayName;
}
