package ypfunding.server.service;

import ypfunding.common.constant.DomainRules;
import ypfunding.common.dto.UserDTO;
import ypfunding.server.auth.LoginUser;
import ypfunding.server.exception.BusinessException;
import ypfunding.server.persistence.TransactionManager;
import ypfunding.server.persistence.dao.UsersDAO;
import ypfunding.server.util.InputValidator;
import java.util.Objects;

public class UserService extends Service{
    private final static String USER_NOT_FOUND_MESSAGE = "사용자를 찾을 수 없습니다.";

    private final UsersDAO usersDAO;

    public UserService(TransactionManager transactionManager, UsersDAO usersDAO) {
        super(transactionManager);
        this.usersDAO = Objects.requireNonNull(usersDAO, "usersDAO");
    }

    public UserDTO getMyInfo(LoginUser user) {
        Objects.requireNonNull(user, "user");
        UserDTO userDTO = transactionManager.execute(conn -> usersDAO.getUserById(conn, user.userId()));
        if(userDTO == null)
            throw new BusinessException(USER_NOT_FOUND_MESSAGE);
        return userDTO;
    }

    public void updateProfile(LoginUser user, String name, String address) {
        //입력 검증
        Objects.requireNonNull(user, "user");
        InputValidator.requireText(name, "이름", DomainRules.MAX_USER_NAME_LENGTH);
        InputValidator.requireText(address, "주소", DomainRules.MAX_ADDRESS_LENGTH);

        //update
        int updated = transactionManager.execute(conn -> usersDAO.updateProfile(conn, user.userId(), name, address));
        if(updated == 0)
            throw new BusinessException(USER_NOT_FOUND_MESSAGE);
    }
}
