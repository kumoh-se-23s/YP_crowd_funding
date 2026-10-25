package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ypfunding.common.type.ProjectFundingStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class FundQueryDTO implements Serializable {
    private Long fundId;                 // 유효한 후원이면 값, 취소면 null
    private Long canceledFundId;         // 취소면 값, 유효한 후원이면 null
    private Long projectId;
    private String projectTitle;
    private String rewardName;
    private Long quantity;
    private Long amount;                 // 가격 × 수량
    private LocalDateTime date;          // 유효: 신청일, 취소: 취소일
    private boolean canceled;            // 유효한 후원(false) / 사용자 취소(true) 구분
    private ProjectFundingStatus status; // 유효한 후원만. 취소면 null
    private String cancelReason;         // 취소만
    private boolean reviewed;            // 유효한 후원만
}