package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@ToString
public class ProjectDTO {

    public enum ApprovalStatus{
        PENDING, APPROVED, REJECTED
    }

    private Long projectId;
    private Long userId;
    private String title;
    private String description;
    private Long goal;
    private Integer duration;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ApprovalStatus approvalStatus;
}
