package ypfunding.server.util;

import ypfunding.common.dto.UserDTO;
import ypfunding.common.type.UserType;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.persistence.dao.UsersDAO;

import java.sql.Connection;
import java.sql.SQLException;

public final class UserUtils {
    private UserUtils() {}
    /**
     * returns true if the user is not an admin.
     */
    public static boolean checkNotAdmin(Connection conn, UsersDAO usersDAO, LoginUser user) throws SQLException {
        UserDTO dto = usersDAO.getUserById(conn, user.userId());
        return dto == null || dto.getUserType() != UserType.ADMIN;
    }

    /**
     * returns true if the user is not a maker.
     */
    public static boolean checkNotMaker(Connection conn, UsersDAO usersDAO, LoginUser user) throws SQLException {
        UserDTO dto = usersDAO.getUserById(conn, user.userId());
        return dto == null || dto.getUserType() != UserType.MAKER;
    }

    /**
     * returns true if the user is not a supporter.
     */
    public static boolean checkNotSupporter(Connection conn, UsersDAO usersDAO, LoginUser user) throws SQLException {
        UserDTO dto = usersDAO.getUserById(conn, user.userId());
        return dto == null || dto.getUserType() != UserType.SUPPORTER;
    }
}
