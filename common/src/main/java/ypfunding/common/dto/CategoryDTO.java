package ypfunding.common.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ypfunding.common.type.Category;

@Getter
@Setter
@ToString
public class CategoryDTO {


    private Long projectId;
    private Long categoryId;
    private Category categoryName;
}
