package kr.yp_crowdfunding.persistence.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString
public class UserDTO {

    public enum UserType {
        SUPPORTER, MAKER, MANAGER
    }

    private Long userID;
    private String loginID;
    private String address;
    private String name;
    private UserType userType;
    private String encryptedPassword;
    private Date regDate;
}
