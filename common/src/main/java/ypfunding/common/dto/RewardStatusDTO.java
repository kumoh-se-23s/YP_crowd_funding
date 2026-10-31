package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RewardStatusDTO {
    private Long rewardId;
    private String name;
    private Long price;
    private Long stock;
    private Long appliedQuantity;
    private Long remainingQuantity;
    private boolean unlimited;
}
