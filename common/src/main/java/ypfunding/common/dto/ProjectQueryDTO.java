package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectQueryDTO {
    private Long projectId;
    private String projectTitle;
    private Long rewardId;
    private String rewardName;
    private String rewardDescription;
    private Long price;
    private Long totalStock;
    private Long totalPrice;
    private Long remainingStock;
}
