package ypfunding.server.service;

import lombok.Getter;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.UserManagedDAO;
import ypfunding.server.persistence.dto.UserLoginDTO;

public class UserManagedService extends Service{
    @Getter
    private final UserManagedDAO managed;
    public UserManagedService(TransactionManager transactionManager, UserManagedDAO managed) {
        super(transactionManager);
        this.managed = managed;
    }

    public UserLoginDTO getUserByLoginId(String loginId) {
        return transactionManager.execute(conn -> managed.getUserByLoginId(conn, loginId));
    }
}
