package kr.yp_crowdfunding.persistence.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;


@Getter
@Setter
@ToString
public class ReviewDTO {
    private Long userID;
    private Long projectID;
    private String rewardName;
    private Integer star;
    private String contents;
    private Date date;
}
