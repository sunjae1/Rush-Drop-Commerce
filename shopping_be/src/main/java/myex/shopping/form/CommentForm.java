package myex.shopping.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Schema(description = "댓글 정보 담는 FORM")
public class CommentForm {
    @NotEmpty(message = "댓글 내용을 입력해주세요")
    @Size(max = 255, message = "댓글은 255자 이하로 입력해주세요")
    @Schema(description = "댓글 내용", example = "댓글 내용입니다.")
    private String content;

    // optional : postId도 넣을 수 있다.
    @Schema(description = "게시물ID", example = "1")
    private Long postId;
}
