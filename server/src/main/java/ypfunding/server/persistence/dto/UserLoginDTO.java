package ypfunding.server.persistence.dto;

import lombok.Getter;
import lombok.Setter;
import ypfunding.common.type.UserType;

@Getter
@Setter
public class UserLoginDTO {
    private Long userId;
    private UserType userType;
    private String loginId;
    private String PasswordHash;
    private String salt;
}
