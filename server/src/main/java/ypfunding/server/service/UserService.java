package ypfunding.server.service;

import ypfunding.common.dto.UserDTO;
import ypfunding.common.type.UserType;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.UsersDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class UserService extends Service{
    protected final UsersDAO usersDAO;
    public UserService(TransactionManager transactionManager, UsersDAO usersDAO) {
        super(transactionManager);
        this.usersDAO = usersDAO;
    }

    public List<UserDTO> getAllUsers() {
        return transactionManager.execute(usersDAO::getAllUsers);
    }

    /**
     * returns true if the user is not an admin.
     */
    public boolean checkNotAdmin(Connection conn, long userId) throws SQLException {
        UserDTO dto = usersDAO.getUserById(conn, userId);
        return dto == null || dto.getUserType() != UserType.ADMIN;
    }

    /**
     * returns true if the user is not an maker.
     */
    public boolean checkNotMaker(Connection conn, long userId) throws SQLException {
        UserDTO dto = usersDAO.getUserById(conn, userId);
        return dto == null || dto.getUserType() != UserType.MAKER;
    }

    /**
     * returns true if the user is not a supporter.
     */
    public boolean checkNotSupporter(Connection conn, long userId) throws SQLException {
        UserDTO dto = usersDAO.getUserById(conn, userId);
        return dto == null || dto.getUserType() != UserType.SUPPORTER;
    }



    public UserDTO getUserById(long userId) {
        return transactionManager.execute(conn -> usersDAO.getUserById(conn, userId));
    }
}
