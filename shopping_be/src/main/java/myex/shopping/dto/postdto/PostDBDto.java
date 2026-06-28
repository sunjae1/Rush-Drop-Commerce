package myex.shopping.dto.postdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import myex.shopping.domain.Post;
import myex.shopping.dto.commentdto.CommentDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Schema(description = "DB에서 꺼낸 게시물 정보 담는 DTO")
public class PostDBDto {
    @Schema(description = "DB게시물ID", example = "1")
    private Long id;
    @Schema(description = "게시물 제목", example = "게시물 제목입니다.")
    private String title;
    @Schema(description = "게시물 내용", example = "게시물 내용입니다.")
    private String content;
    @Schema(description = "게시물 작성자", example = "사용자A")
    private String authorName; //User 엔티티 대신 user.getName()
    @Schema(description = "게시물 안 댓글들", example = "[댓글A, 댓글B]")
    private List<CommentDto> comments;

    @Schema(description = "작성일자", example = "2025-10-27")
    private LocalDateTime createdDate;


    public PostDBDto(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.createdDate = post.getCreatedDate();
        this.authorName = post.getUser().getName(); //LAZY 로딩 초기화
        this.comments = post.getComments().stream() //LAZY 로딩 초기화
                .map(CommentDto::new)
                .collect(Collectors.toList());

    }
}
