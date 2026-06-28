package myex.shopping.dto.mypagedto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import myex.shopping.domain.Post;

import java.time.LocalDateTime;

@Getter
@Schema(description = "마이 페이지 보낼 게시물 정보 담는 DTO")
public class MyPagePostDBDto {
    @Schema(description = "마이페이지 게시물 ID", example = "1")
    private Long id;
    @Schema(description = "게시물 제목", example = "내가 쓴 게시물 제목입니다.")
    private String title;
    @Schema(description = "게시물 내용", example = "게시물 내용입니다.")
    private String content;
    @Schema(description = "작성자 이름", example = "사용자A")
    private String authorName; //User 엔티티 대신 user.getName()

    @Schema(description = "작성날짜", example = "2025-10-27")
    private LocalDateTime createdDate;


    public MyPagePostDBDto(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.createdDate = post.getCreatedDate();
        this.authorName = post.getUser().getName(); //LAZY 로딩 초기화

    }
}
