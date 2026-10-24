package ypfunding.common.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class CanceledFundDTO implements Serializable {
    //memo: 이거 amount 빠진듯
    //reward 테이블과의 연결성이 낮아보임
    //실질적으로 fund랑 reward가 서로서로 잘 연동되어 계산되어야할 거 같은데
    //이거도 회의 ㄱㄱ
    private Long canceledFundId;
    private Long userId;
    private Long projectId;
    private Long rewardId;
    private Long quantity;
    private String content;
    private LocalDateTime cancelDate;
}
