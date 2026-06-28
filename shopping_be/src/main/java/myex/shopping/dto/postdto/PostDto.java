package myex.shopping.dto.postdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import myex.shopping.domain.Post;
import myex.shopping.dto.commentdto.CommentDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Schema(description = "게시물 정보를 담는 DTO")
public class PostDto {
    @Schema(description = "게시물 ID", example = "1")
    private Long id;
    @Schema(description = "게시물 제목", example = "제목1")
    private String title;
    @Schema(description = "게시물 작성자", example = "사용자A")
    private String author;
    @Schema(description = "게시물 내용", example = "게시물 내용입니다.")
    private String content;
    @Schema(description = "작성 일자", example = "2025-10-27")
    private LocalDateTime createdDate;
    @Schema(description = "댓글들", example = "[댓글A, 댓글B, 댓글C]")
    private List<CommentDto> comments;

    public PostDto(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.author = post.getAuthor();
        this.content = post.getContent();
        this.createdDate = post.getCreatedDate();
        this.comments = post.getComments().stream()
                .map(CommentDto::new)
                .collect(Collectors.toList());
    }
}
