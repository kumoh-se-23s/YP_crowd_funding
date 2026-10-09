package ypfunding.server.auth;

import ypfunding.common.type.UserType;
import java.util.Objects;

//로그인된 유저 정보
public record LoginUser(long userId, UserType userType) {
    public LoginUser {
        if(userId <= 0) {
            throw new IllegalArgumentException("올바르지 않은 유저id: " + userId);
        }
        Objects.requireNonNull(userType, "userType");
    }
}