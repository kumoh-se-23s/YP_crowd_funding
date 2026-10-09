package kr.yp_crowdfunding.persistence.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString
public class ProjectDTO {

    public enum ApprovalStatus{
        PENDING, APPROVED, REJECTED
    }

    private Long id;
    private String title;
    private Integer duration;
    private Date startDate;
    private Long goal;
    private Long writerID;
    private ApprovalStatus approvalStatus;
}
