package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class LikeDTO {
    private Long userID;
    private Long projectID;
}
