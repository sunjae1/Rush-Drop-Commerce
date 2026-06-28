package myex.shopping.dto.categorydto;

import lombok.Getter;
import lombok.Setter;
import myex.shopping.domain.Category;

@Getter
@Setter
public class CategoryEditDTO {
    private String name;

    public CategoryEditDTO(Category category) {
        this.name = category.getName();
    }

    //JSON -> 객체 : 역직렬화 위해서 기본 생성자 생성.
    public CategoryEditDTO() {
    }
}
