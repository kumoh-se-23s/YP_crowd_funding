package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class FundDTO implements Serializable {
    private Long fundId;
    private Long userId;
    private Long projectId;
    private Long rewardId;
    private Long quantity;
    private LocalDateTime date;
}
