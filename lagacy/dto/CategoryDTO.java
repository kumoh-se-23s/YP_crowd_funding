package kr.yp_crowdfunding.persistence.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CategoryDTO {

    public enum Category{
        GAME, PET, BOOK
    }

    private Long projectID;
    private Category category;
}
