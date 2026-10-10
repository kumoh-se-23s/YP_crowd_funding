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
    private Blob imageData;
}
