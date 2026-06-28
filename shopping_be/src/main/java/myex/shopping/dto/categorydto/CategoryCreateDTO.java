package myex.shopping.dto.categorydto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import myex.shopping.domain.Category;

@Getter
@Setter
public class CategoryCreateDTO {
    @NotBlank
    private String name;

    public CategoryCreateDTO(Category category) {
        this.name = category.getName();
    }

    //JSON -> 객체 : 역직렬화 위해서 기본 생성자 생성.
    public CategoryCreateDTO() {
    }
}
