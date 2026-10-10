package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString
public class RejectDTO {
    private Long rejectReasonId;
    private Long projectID;
    private String reason;
    private Date createdAt;
}
