package kr.yp_crowdfunding.persistence.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RewardDTO {
    //얘도 찾아보니까 name이 projectId랑 묶여있으면
    //리워드 오타수정(update)가 발생하면 참조 무결성 위반 및 대규모 cascade 발생
    //심하면 변경 이전의 name의 data들이 그냥 방치되어있을 수도 있음
    //단일pk로 rewardId 만들 필요 성의 대한 회의 필요
    //그리고 중복 불가를 pk말고 그냥 unique 속성 활용으로 고려해도 될듯?
    private Long projectID;
    private String name;
    private String description;
    private Integer price;
    private Integer count;
}
