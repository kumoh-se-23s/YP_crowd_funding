package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.Blob;

@Getter
@Setter
@ToString
public class ImageDTO {
    private Long imageId;
    private Long rewardId;
    private String name;
    private String type;
    //DB랑 연결된 스트림만 가져서 서비스 로직이 끝나면 바로 커넥션이 닫혀서
    //Service 밖에서 반환한 뒤에 이미지를 읽으면 커넥션이 닫혔다는 SQL 에러 뜬다캄
    private Blob imageData;
}
