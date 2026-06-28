package myex.shopping.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.Post;
import myex.shopping.domain.User;
import myex.shopping.dto.postdto.PostDBDto;
import myex.shopping.dto.postdto.PostListDto;
import myex.shopping.dto.postdto.PostDto;
import myex.shopping.dto.postdto.PostEditDto;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.form.PostForm;
import myex.shopping.repository.PostRepository;
import myex.shopping.service.PostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/posts")
@Tag(name = "Post", description = "게시물 관련 API")
@Validated
public class ApiPostController {

    private final PostRepository postRepository;
    private final PostService postService;

    @Operation(summary = "전체 게시물 조회", description = "전체 게시물을 조회합니다.", responses = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 정렬 파라미터")
    })
    @GetMapping
    public ResponseEntity<?> list(@RequestParam(name = "sort", defaultValue = "desc") String sort) {
        try {
            List<PostListDto> posts = postService.findAllPostListDto(sort);
            return ResponseEntity.ok(posts);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "게시물 단건 조회", description = "게시물 단건을 조회합니다.", responses = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "조회 실패")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PostDBDto> view(@PathVariable @Positive(message = "양수만 입력 가능합니다.") Long id) {
        return postRepository.findById(id)
                .map(PostDBDto::new)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "게시물 등록", description = "게시물을 등록합니다.", responses = {
            @ApiResponse(responseCode = "401", description = "로그인 실패"),
            @ApiResponse(responseCode = "201", description = "등록 완료")
    })
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody PostForm form,
                                    @AuthenticationPrincipal PrincipalDetails principalDetails) {
        if (principalDetails == null) {
            log.info("login failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        User loginUser = principalDetails.getUser();
        Post post = setterPost(form, loginUser);
        PostDBDto postDBDto = new PostDBDto(postService.addUser(post, loginUser.getId()));
        log.info("created postDTO : {}", postDBDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(postDBDto);
    }

    @Operation(summary = "게시물 수정", description = "로그인 사용자에 대한 게시물 수정 요청을 처리합니다.", responses = {
            @ApiResponse(responseCode = "401", description = "로그인 실패"),
            @ApiResponse(responseCode = "404", description = "게시물을 찾지 못함"),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음"),
            @ApiResponse(responseCode = "200", description = "수정 완료")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable @Positive Long id,
                                    @Valid @RequestBody PostEditDto form,
                                    @AuthenticationPrincipal PrincipalDetails principalDetails) {
        if (principalDetails == null) {
            log.info("login failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        User loginUser = principalDetails.getUser();
        if (postService.isPostOwner(id, loginUser)) {
            PostDto postDto = postService.updatePost(id, form);
            return ResponseEntity.ok(postDto);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("게시물 작성자만 수정할 수 있습니다.");
    }

    @Operation(summary = "게시물 삭제", description = "게시물을 삭제합니다.", responses = {
            @ApiResponse(responseCode = "404", description = "게시물을 찾지 못함"),
            @ApiResponse(responseCode = "204", description = "삭제 성공")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable @Positive(message = "양수만 입력 가능합니다.") Long id,
                                    @AuthenticationPrincipal PrincipalDetails principalDetails) {
        if (principalDetails == null) {
            log.info("login failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User loginUser = principalDetails.getUser();
        if (postService.isPostOwner(id, loginUser)) {
            postRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("게시물 작성자만 삭제할 수 있습니다.");
    }

    private static Post setterPost(PostForm form, User loginUser) {
        Post post = new Post();
        post.setTitle(form.getTitle());
        post.setContent(form.getContent());
        post.setCreatedDate(LocalDateTime.now());
        post.setAuthor(loginUser.getName());
        return post;
    }
}
