package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CategoryDTO {

    public enum Category{
        TECH_HOME_APPLIANCES,
        HOME_LIVING,
        BEAUTY,
        FASHION,
        FOOD,
        BOOK,
        KIDS,
        SPORTS,
        GOODS,
        TRAVEL,
        FANDOM,
        PET,
        DESIGN,
        ART,
        CAR,
        GAME,
        MOVIE,
        MUSIC,
        PHOTO,
        WEBTOON,
        MEMBERSHIP,
        SOCIAL
    }

    private Long projectId;
    private Long categoryId;
    private Category categoryName;
}
