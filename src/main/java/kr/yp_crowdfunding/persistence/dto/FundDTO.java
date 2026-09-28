package kr.yp_crowdfunding.persistence.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString
public class FundDTO {
    //memo: 이거 amount 빠진듯
    //reward 테이블과의 연결성이 낮아보임
    //실질적으로 fund랑 reward가 서로서로 잘 연동되어 계산되어야할 거 같은데
    //이거도 회의 ㄱㄱ
    private Long userID;
    private Long projectID;
    private String rewardName;
    private Date date;
}
