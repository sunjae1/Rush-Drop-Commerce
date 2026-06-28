package myex.shopping.dto.categorydto;

import lombok.Getter;
import lombok.Setter;
import myex.shopping.domain.Category;

@Getter
@Setter
public class CategoryDTO {
    private Long id;
    private String name;
    private String representativeImageUrl;
    private int itemCount;

    public CategoryDTO(Category category) {
        this.id = category.getId();
        this.name = category.getName();
    }

    //JSON -> 객체 : 역직렬화 위해서 기본 생성자 생성.
    public CategoryDTO() {
    }
}
