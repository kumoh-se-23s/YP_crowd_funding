package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Date;


@Getter
@Setter
@ToString
public class ReviewDTO {
    private Long fundId;
    private Integer star;
    private String content;
    private LocalDateTime date;
}
