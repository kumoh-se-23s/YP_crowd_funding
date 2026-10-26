package ypfunding.server.check.function;

import ypfunding.server.auth.LoginUser;
import ypfunding.server.check.CheckBase;
import ypfunding.server.bootstrap.AppContext;
import ypfunding.server.check.CheckUsers;
import ypfunding.server.service.AuthService;
import ypfunding.server.service.UserService;

//A 담당 - 05번
public class AuthCheck extends CheckBase {
    public AuthCheck(AppContext context, CheckUsers users) {
        super(context, users);
    }

    private static final String NEW_NAME = "새이름";
    private static final String NEW_ADDRESS = "부산광역시 해운대구 센텀로 1";
    private static final String CUR_PASSWORD = "supporterpw";
    private static final String NEW_PASSWORD = "newpw1234";

    //SELECT user_id, login_id, name, address, password, salt FROM users WHERE login_id = 'profileSupporter';
    public void test05UpdateProfile() {
        LoginUser user = users.profileSupporter();
        UserService userService = context.getUserService();
        AuthService authService = context.getAuthService();

        userService.updateProfile(user, NEW_NAME, NEW_ADDRESS);
        authService.changePassword(user, CUR_PASSWORD, NEW_PASSWORD);
    }
}
