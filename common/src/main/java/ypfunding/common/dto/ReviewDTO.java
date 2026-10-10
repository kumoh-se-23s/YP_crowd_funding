package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;


@Getter
@Setter
@ToString
public class ReviewDTO {
    private Long fundID;
    private Integer star;
    private String content;
    private Date date;
}
