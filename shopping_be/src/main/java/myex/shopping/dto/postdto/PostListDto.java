package myex.shopping.dto.postdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import myex.shopping.domain.Post;

import java.time.LocalDateTime;

@Getter
@Schema(description = "Post summary for list view")
public class PostListDto {
    @Schema(description = "Post ID", example = "1")
    private final Long id;

    @Schema(description = "Post title", example = "Sample title")
    private final String title;

    @Schema(description = "Post content", example = "Sample content")
    private final String content;

    @Schema(description = "Author name", example = "userA")
    private final String authorName;

    @Schema(description = "Created date", example = "2025-10-27T10:00:00")
    private final LocalDateTime createdDate;

    public PostListDto(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.authorName = post.getUser().getName();
        this.createdDate = post.getCreatedDate();
    }
}
