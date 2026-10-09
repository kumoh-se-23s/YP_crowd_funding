package ypfunding.common.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
//유저타입
public enum UserType {
    ADMIN("관리자"),
    MAKER("메이커"),
    SUPPORTER("서포터");

    private final String displayName;
}
