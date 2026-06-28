package myex.shopping.controller.api;

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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiCommentControllerTest {

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private UserRepository userRepository;
        @Autowired
        private PostRepository postRepository;
        @Autowired
        private CommentRepository commentRepository;
        private User testUser;
        private Post testPost;
        private PrincipalDetails testUserDetails;

        @BeforeEach
        void setUp() throws Exception {
                testUser = new User("testuser@example.com", "테스트유저", "password");
                userRepository.save(testUser);

                testPost = new Post();
                testPost.setTitle("테스트 게시글");
                testPost.setContent("테스트 내용입니다.");
                testPost.setUser(testUser);
                postRepository.save(testPost);

                testUserDetails = new PrincipalDetails(testUser);
        }

        @Test
        @DisplayName("댓글 추가 API 테스트")
        void addComment_shouldCreateComment() throws Exception {
                mockMvc.perform(post("/api/posts/{postId}/comments", testPost.getId())
                                .with(user(testUserDetails))
                                .param("reply_content", "새로운 댓글입니다."))
                                .andDo(print())
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.content", is("새로운 댓글입니다.")))
                                .andExpect(jsonPath("$.username", is(testUser.getName())));
        }

        @Test
        @DisplayName("댓글 수정 API 테스트 - 성공")
        void updateComment_shouldUpdate() throws Exception {
                Comment comment = new Comment();
                comment.setContent("원본 댓글");
                comment.setUser(testUser);
                testPost.addComment(comment);
                commentRepository.save(comment);

                mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", testPost.getId(), comment.getId())
                                .with(user(testUserDetails))
                                .param("reply_content", "수정된 댓글입니다."))
                                .andDo(print())
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", is("수정된 댓글입니다.")));
        }

        @Test
        @DisplayName("댓글 추가 API 테스트 - 255자 초과시 실패")
        void addComment_shouldRejectTooLongContent() throws Exception {
                String tooLongComment = "a".repeat(256);

                mockMvc.perform(post("/api/posts/{postId}/comments", testPost.getId())
                                .with(user(testUserDetails))
                                .param("reply_content", tooLongComment))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$['addComment.reply_content']", hasItem("댓글은 255자 이하로 입력해주세요")));
        }

        @Test
        @DisplayName("댓글 수정 API 테스트 - 255자 초과시 실패")
        void updateComment_shouldRejectTooLongContent() throws Exception {
                Comment comment = new Comment();
                comment.setContent("원본 댓글");
                comment.setUser(testUser);
                testPost.addComment(comment);
                commentRepository.save(comment);

                String tooLongComment = "b".repeat(256);

                mockMvc.perform(put("/api/posts/{postId}/comments/{commentId}", testPost.getId(), comment.getId())
                                .with(user(testUserDetails))
                                .param("reply_content", tooLongComment))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$['updateComment.reply_content']", hasItem("댓글은 255자 이하로 입력해주세요")));
        }

        @Test
        @DisplayName("댓글 삭제 API 테스트 - 성공")
        void deleteComment_shouldDelete() throws Exception {
                Comment comment = new Comment();
                comment.setContent("삭제될 댓글");
                comment.setUser(testUser);
                testPost.addComment(comment);
                commentRepository.save(comment);

                mockMvc.perform(delete("/api/posts/{postId}/comments/{commentId}", testPost.getId(), comment.getId())
                                .with(user(testUserDetails)))
                                .andDo(print())
                                .andExpect(status().isNoContent());
        }
}
