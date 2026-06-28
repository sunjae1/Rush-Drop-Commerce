package myex.shopping.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Schema(description = "게시물 정보 담는 FORM")
public class PostForm {

    //post.id
    private Long id;

    @NotEmpty(message = "제목을 입력해주세요.")
    @Schema(description = "게시물 제목", example = "게시물 제목1 입니다.")
    private String title;
    @NotEmpty(message = "내용을 입력해주세요")
    @Schema(description = "게시물 내용", example = "게시물 내용입니다.")
    private String content;
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Override
    public String toString() {
        return "PostForm{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", userId=" + userId +
                '}';
    }
}
