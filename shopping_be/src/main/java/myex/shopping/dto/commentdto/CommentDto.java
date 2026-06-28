package myex.shopping.dto.commentdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import myex.shopping.domain.Comment;

import java.time.LocalDateTime;

@Getter
@Schema(description = "댓글 정보를 담는 DTO")
public class CommentDto {
    @Schema(description = "댓글 ID", example = "1")
    private Long id;
    @Schema(description = "댓글 내용", example = "댓글 내용입니다.")
    private String content;
    @Schema(description = "작성자 이름", example = "사용자A")
    private String username;
    @Schema(description = "댓글 작성 일자", example = "2025-10-27")
    private LocalDateTime createdDate;


    public CommentDto() {
    }

    public CommentDto(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        if (comment.getUser() != null)
            this.username = comment.getUser().getName();
        this.createdDate = comment.getCreatedDate();
    }
}
