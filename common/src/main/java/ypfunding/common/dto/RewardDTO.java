package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RewardDTO {
    private Long rewardId;
    private Long projectId;
    private String name;
    private Long price;
    private String description;
    private Long stock;
}
