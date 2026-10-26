package ypfunding.server.check;

import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.bootstrap.AppContext;
import ypfunding.server.service.AuthService;

import java.util.HashMap;
import java.util.Map;

public class CheckUsers {
    private final AuthService authService;
    private final Map<String, LoginUser> cache = new HashMap<>();

    public CheckUsers(AppContext context) {
        this.authService = context.getAuthService();
    }

    public LoginUser admin() {
        return get("admin1", "adminpw", UserType.ADMIN);
    }

    public LoginUser maker() {
        return get("maker1", "makerpw", UserType.MAKER);
    }

    public LoginUser maker2() {
        return get("maker2", "makerpw", UserType.MAKER);
    }

    public LoginUser supporter() {
        return get("supporter1", "supporterpw", UserType.SUPPORTER);
    }

    public LoginUser supporter2() {
        return get("supporter2", "supporterpw", UserType.SUPPORTER);
    }

    //5번 기능에서 쓸 비번 변경용 계정
    public LoginUser profileSupporter() {
        return get("profileSupporter", "supporterpw", UserType.SUPPORTER);
    }


    //있으면 꺼내고 없으면 만들기
    private synchronized LoginUser get(String id, String pw, UserType expected) {
        LoginUser user = cache.get(id);
        if(user == null) {
            user = authService.login(id, pw);

            //역할검증 안전장치
            if(user.userType() != expected)
                throw new IllegalStateException("역할 안 맞음: " + id);
            cache.put(id, user);
        }
        return user;
    }
}
