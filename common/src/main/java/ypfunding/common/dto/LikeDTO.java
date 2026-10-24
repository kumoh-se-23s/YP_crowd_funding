package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;


@Getter
@Setter
@ToString
public class LikeDTO implements Serializable {
    private Long userID;
    private Long projectID;
}
