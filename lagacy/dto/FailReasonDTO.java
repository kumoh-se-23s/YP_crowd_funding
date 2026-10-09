package kr.yp_crowdfunding.persistence.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString
public class FailReasonDTO {
    private Long id;
    private Long projectID;
    private String reason;
    private Date date;
}
