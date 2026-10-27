package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class ProjectFunderDTO implements Serializable {
    private Long fundId;
    private String loginId;
    private String name;
    private String rewardName;
    private Long quantity;
    private Long amount;
    private LocalDateTime date;
}
