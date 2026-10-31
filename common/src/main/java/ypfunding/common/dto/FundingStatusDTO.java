package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class FundingStatusDTO {
    private ProjectQueryDTO project;
    private List<RewardStatusDTO> rewards;
    private List<FundingSupporterDTO> supporters;
}
