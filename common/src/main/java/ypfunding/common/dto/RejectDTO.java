package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@ToString
public class RejectDTO {
    private Long rejectReasonId;
    private Long projectId;
    private String reason;
    private LocalDateTime createdAt;
}
