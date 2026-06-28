package myex.shopping.dto.postdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostEditDto {

    private Long id;
    @NotEmpty(message = "제목을 입력해주세요.")
    @Schema(description = "게시물 제목", example = "게시물 제목1 입니다.")
    private String title;
    @NotEmpty(message = "내용을 입력해주세요")
    @Schema(description = "게시물 내용", example = "게시물 내용입니다.")
    private String content;


    @Override
    public String toString() {
        return "PostEditDto{" +
                "title='" + title + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
