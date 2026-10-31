package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class FundingSupporterDTO {
    private Long fundId;
    private String name;
    private String address;
    private Long rewardId;
    private String rewardName;
    private Long quantity;
    private Long amount;
    private LocalDateTime date;
}
