package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import ypfunding.common.type.ApprovalStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProjectQueryDTO {
    private Long projectId;
    private Long userId;
    private String title;
    private String description;
    private Long goal;
    private Integer duration;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ApprovalStatus approvalStatus;
    private Long supporterCount;
    private Long soldPrice;
    private Long fundProgress;
}
