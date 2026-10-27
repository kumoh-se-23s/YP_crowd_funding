package ypfunding.server.util;

import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.PermissionDeniedException;

public final class UserUtils {
    private UserUtils() {}

    public static void assertAdmin(LoginUser user) {
        if (checkNotAdmin(user)) {
            throw new PermissionDeniedException(user + " is not admin");
        }
    }
    public static void assertMaker(LoginUser user) {
        if (checkNotMaker(user)) {
            throw new PermissionDeniedException(user + " is not maker");
        }
    }

    public static void assertSupporter(LoginUser user) {
        if (checkNotSupporter(user)) {
            throw new PermissionDeniedException(user + " is not supporter");
        }
    }

    /**
     * returns true if the user is not an admin.
     */
    public static boolean checkNotAdmin(LoginUser user) {
        return user.userType() != UserType.ADMIN;
    }

    /**
     * returns true if the user is not a maker.
     */
    public static boolean checkNotMaker(LoginUser user) {
        return user.userType() != UserType.MAKER;
    }

    /**
     * returns true if the user is not a supporter.
     */
    public static boolean checkNotSupporter(LoginUser user) {
        return user.userType() != UserType.SUPPORTER;
    }
}
