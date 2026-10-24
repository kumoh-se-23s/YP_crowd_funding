package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ypfunding.common.type.UserType;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@ToString
public class UserDTO {

    private Long userID;
    private String loginID;
    private String encryptedPassword;
    private String name;
    private String address;
    private UserType userType;
    private LocalDateTime regDate;
}
