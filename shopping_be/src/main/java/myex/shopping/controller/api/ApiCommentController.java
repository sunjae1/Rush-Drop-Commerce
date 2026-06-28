package myex.shopping.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import myex.shopping.dto.userdto.PrincipalDetails;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.Comment;
import myex.shopping.domain.Post;
import myex.shopping.domain.User;
import myex.shopping.dto.commentdto.CommentDto;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.form.CommentForm;
import myex.shopping.repository.CommentRepository;
import myex.shopping.repository.PostRepository;
import myex.shopping.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Tag(name = "Comment", description = "댓글 관련 API")
@Validated
public class ApiCommentController {
        private final PostRepository postRepository;
        private final CommentRepository commentRepository;
        private final CommentService commentService;

        @Operation(summary = "댓글 추가", description = "로그인 사용자로 댓글을 추가합니다.", responses = {
                        @ApiResponse(responseCode = "404", description = "게시물을 찾지 못함"),
                        @ApiResponse(responseCode = "401", description = "로그인 실패"),
                        @ApiResponse(responseCode = "201", description = "댓글 생성 성공")
        })
        // 댓글 추가 :
        // @RequestParam : form-data로 보내기.
        // @RequestBody : Dto 써서, JSON 으로 보내기 가능. (고민중)
        @PostMapping("/{postId}/comments")
        public ResponseEntity<?> addComment(@PathVariable @Positive(message = "양수만 입력 가능합니다.") Long postId,
                        @RequestParam
                        @NotBlank(message = "댓글 내용을 입력해주세요")
                        @Size(max = 255, message = "댓글은 255자 이하로 입력해주세요")
                        String reply_content,
                        @AuthenticationPrincipal PrincipalDetails principalDetails) {
                if (principalDetails == null) {
                        log.info("로그인 실패");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body("로그인이 필요합니다.");
                }
                User loginUser = principalDetails.getUser();
                CommentForm form = new CommentForm();
                form.setContent(reply_content);
                CommentDto commentDto = new CommentDto(commentService.addComment(postId, form, loginUser));
                return ResponseEntity.status(HttpStatus.CREATED).body(commentDto);

        }

        @Operation(summary = "댓글 수정", description = "로그인 사용자에 대해 댓글 수정 요처을 처리합니다.", responses = {
                        @ApiResponse(responseCode = "401", description = "로그인 실패"),
                        @ApiResponse(responseCode = "404", description = "댓글이나 게시물을 찾지 못함"),
                        @ApiResponse(responseCode = "200", description = "댓글 수정 완료")
        })
        // 댓글 수정
        @PutMapping("/{postId}/comments/{commentId}")
        public ResponseEntity<?> updateComment(@PathVariable @Positive(message = "양수만 입력 가능합니다.") Long postId,
                        @PathVariable @Positive(message = "양수만 입력 가능합니다.") Long commentId,
                        @RequestParam
                        @NotBlank(message = "댓글 수정 내용을 입력해주세요")
                        @Size(max = 255, message = "댓글은 255자 이하로 입력해주세요")
                        String reply_content,
                        @AuthenticationPrincipal PrincipalDetails principalDetails) {
                if (principalDetails == null) {
                        log.info("로그인 실패");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body("로그인이 필요합니다.");
                }
                User loginUser = principalDetails.getUser();
                CommentForm form = new CommentForm();
                form.setContent(reply_content);
                form.setPostId(postId);
                // 작성자 본인만 수정 가능.
                if (commentService.isCommentOwner(commentId, loginUser)) {
                        Comment comment = commentService.updateComment(commentId, form, loginUser.getId());
                        return ResponseEntity.ok(new CommentDto(comment));
                } else {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body("댓글 작성자만 수정할 수 있습니다.");
                }

        }

        @Operation(summary = "댓글 삭제", description = "로그인 사용자에 대해 댓글 삭제 요청을 처리합니다.", responses = {
                        @ApiResponse(responseCode = "401", description = "로그인 실패"),
                        @ApiResponse(responseCode = "404", description = "댓글이나 게시물을 찾지 못함."),
                        @ApiResponse(responseCode = "403", description = "삭제 권한 없음(댓글 사용자 본인이 아님)"),
                        @ApiResponse(responseCode = "204", description = "삭제 완료")
        })
        // 댓글 삭제
        @DeleteMapping("/{postId}/comments/{commentId}")
        public ResponseEntity<?> deleteComment(@PathVariable @Positive(message = "양수만 입력가능합니다.") Long postId,
                        @PathVariable @Positive(message = "양수만 입력가능합니다.") Long commentId,
                        @AuthenticationPrincipal PrincipalDetails principalDetails) {
                if (principalDetails == null) {
                        log.info("로그인 실패");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .build();
                }
                User loginUser = principalDetails.getUser();

                // 작성자 본인만 삭제 가능
                if (commentService.isCommentOwner(commentId, loginUser)) {
                        commentService.deleteComment(postId, commentId);
                        return ResponseEntity.noContent().build(); // 204 No Content
                } else {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body("댓글 작성자만 삭제할 수 있습니다.");
                }

        }

}
