package myex.shopping.controller.web;

import myex.shopping.domain.Comment;
import myex.shopping.domain.Post;
import myex.shopping.domain.User;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.repository.CommentRepository;
import myex.shopping.repository.PostRepository;
import myex.shopping.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class WebCommentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CommentRepository commentRepository;

    private Post testPost;
    @SuppressWarnings("FieldCanBeLocal")
    private User testUser;
    @SuppressWarnings("FieldCanBeLocal")
    private User otherUser;
    private PrincipalDetails testUserDetails;
    private PrincipalDetails otherUserDetails;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser@example.com", "테스트유저", "password");
        testUser.setActive(true);
        userRepository.save(testUser);

        otherUser = new User("other@example.com", "다른유저", "password");
        otherUser.setActive(true);
        userRepository.save(otherUser);

        testPost = new Post();
        testPost.setTitle("테스트 게시글");
        testPost.setContent("테스트 내용입니다.");
        testPost.setUser(testUser);
        postRepository.save(testPost);

        testUserDetails = new PrincipalDetails(testUser);
        otherUserDetails = new PrincipalDetails(otherUser);
    }

    // ──────────────────── 댓글 추가 ────────────────────

    @Test
    @DisplayName("댓글 추가 성공 - @AuthenticationPrincipal로 작성자 이름이 DB에 저장된다")
    void addComment_success_authorNameIsSaved() throws Exception {
        mockMvc.perform(post("/posts/{postId}/comments", testPost.getId())
                        .with(user(testUserDetails))
                        .with(csrf())
                        .param("content", "새로운 댓글입니다."))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts/" + testPost.getId()));

        // 댓글 작성자 이름이 null이 아닌지 검증 (핵심 버그 수정 확인)
        Comment saved = commentRepository.findAll().stream()
                .filter(c -> "새로운 댓글입니다.".equals(c.getContent()))
                .findFirst()
                .orElseThrow();
        assertThat(saved.getUser()).isNotNull();
        assertThat(saved.getUser().getName()).isEqualTo("테스트유저");
    }

    @Test
    @DisplayName("댓글 추가 - 비인증 요청 시 로그인 페이지로 리다이렉트")
    void addComment_withoutAuth_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/posts/{postId}/comments", testPost.getId())
                        .with(csrf())
                        .param("content", "비인증 댓글"))
                .andDo(print())
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("댓글 추가 - 빈 내용 검증 실패 시 posts/view 뷰 반환")
    void addComment_emptyContent_returnsViewPage() throws Exception {
        mockMvc.perform(post("/posts/{postId}/comments", testPost.getId())
                        .with(user(testUserDetails))
                        .with(csrf())
                        .param("content", ""))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("posts/view"));
    }

    // ──────────────────── 댓글 수정 ────────────────────

    @Test
    @DisplayName("댓글 수정 성공 - 본인 댓글 수정 후 게시글 상세로 리다이렉트")
    void updateComment_success_byOwner() throws Exception {
        Comment comment = new Comment();
        comment.setContent("원본 댓글");
        comment.setUser(testUser);
        testPost.addComment(comment);
        commentRepository.save(comment);

        mockMvc.perform(post("/posts/{postId}/comments/{commentId}/update",
                        testPost.getId(), comment.getId())
                        .with(user(testUserDetails))
                        .with(csrf())
                        .param("content", "수정된 댓글입니다."))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts/" + testPost.getId()));

        Comment updated = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(updated.getContent()).isEqualTo("수정된 댓글입니다.");
    }

    @Test
    @DisplayName("댓글 수정 - 타인이 수정 요청 시 변경 없이 리다이렉트")
    void updateComment_byOther_notChanged() throws Exception {
        Comment comment = new Comment();
        comment.setContent("원본 댓글");
        comment.setUser(testUser);
        testPost.addComment(comment);
        commentRepository.save(comment);

        mockMvc.perform(post("/posts/{postId}/comments/{commentId}/update",
                        testPost.getId(), comment.getId())
                        .with(user(otherUserDetails))
                        .with(csrf())
                        .param("content", "타인이 수정하려는 댓글"))
                .andDo(print())
                .andExpect(status().is3xxRedirection());

        // 내용이 변경되지 않았는지 확인
        Comment notChanged = commentRepository.findById(comment.getId()).orElseThrow();
        assertThat(notChanged.getContent()).isEqualTo("원본 댓글");
    }

    // ──────────────────── 댓글 삭제 ────────────────────

    @Test
    @DisplayName("댓글 삭제 성공 - 본인 댓글 삭제 후 리다이렉트")
    void deleteComment_success_byOwner() throws Exception {
        Comment comment = new Comment();
        comment.setContent("삭제될 댓글");
        comment.setUser(testUser);
        testPost.addComment(comment);
        commentRepository.save(comment);
        Long commentId = comment.getId();

        mockMvc.perform(post("/posts/{postId}/comments/{commentId}",
                        testPost.getId(), commentId)
                        .with(user(testUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts/" + testPost.getId()));
    }

    @Test
    @DisplayName("댓글 삭제 - 타인의 댓글 삭제 요청 무시 후 리다이렉트")
    void deleteComment_byOther_ignored() throws Exception {
        Comment comment = new Comment();
        comment.setContent("타인이 삭제 못함");
        comment.setUser(testUser);
        testPost.addComment(comment);
        commentRepository.save(comment);

        mockMvc.perform(post("/posts/{postId}/comments/{commentId}",
                        testPost.getId(), comment.getId())
                        .with(user(otherUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().is3xxRedirection());
    }
}






