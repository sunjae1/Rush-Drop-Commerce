package myex.shopping.controller.web;

import myex.shopping.domain.Post;
import myex.shopping.domain.User;
import myex.shopping.dto.userdto.PrincipalDetails;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class WebPostControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepository postRepository;

    @SuppressWarnings("FieldCanBeLocal")
    private User testUser;
    private Post testPost;
    private PrincipalDetails testUserDetails;

    @BeforeEach
    void setUp() {
        testUser = new User("posttest@example.com", "게시글유저", "password");
        testUser.setActive(true);
        userRepository.save(testUser);

        testPost = new Post();
        testPost.setTitle("테스트 게시글");
        testPost.setContent("테스트 내용입니다.");
        testPost.setUser(testUser);
        postRepository.save(testPost);

        testUserDetails = new PrincipalDetails(testUser);
    }

    // ──────────────────── 게시판 목록 ────────────────────

    @Test
    @DisplayName("게시판 목록 - 비인증 사용자도 조회 가능 (loginUser 모델에 없음)")
    void list_withoutAuth_returns200() throws Exception {
        mockMvc.perform(get("/posts"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"))
                .andExpect(model().attributeDoesNotExist("loginUser"));
    }

    @Test
    @DisplayName("게시판 목록 - 인증된 사용자 조회 시 loginUser 모델에 포함")
    void list_withAuth_loginUserInModel() throws Exception {
        mockMvc.perform(get("/posts")
                        .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("posts/list"))
                .andExpect(model().attributeExists("loginUser"));
    }

    // ──────────────────── 게시물 상세 ────────────────────

    @Test
    @DisplayName("게시물 상세 - 인증된 사용자 조회 시 loginUser 모델에 이름 포함")
    void view_withAuth_loginUserInModel() throws Exception {
        mockMvc.perform(get("/posts/{id}", testPost.getId())
                        .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("posts/view"))
                .andExpect(model().attributeExists("loginUser"))
                .andExpect(model().attributeExists("post"));
    }

    @Test
    @DisplayName("게시물 상세 - 비인증 사용자도 조회 가능 (loginUser 없음)")
    void view_withoutAuth_returns200() throws Exception {
        mockMvc.perform(get("/posts/{id}", testPost.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("posts/view"))
                .andExpect(model().attributeDoesNotExist("loginUser"));
    }

    // ──────────────────── 게시물 등록 ────────────────────

    @Test
    @DisplayName("게시물 등록 성공 - 인증된 사용자가 /posts로 리다이렉트")
    void create_success() throws Exception {
        mockMvc.perform(post("/posts/new")
                        .with(user(testUserDetails))
                        .with(csrf())
                        .param("title", "새 게시물 제목")
                        .param("content", "새 게시물 내용입니다."))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));
    }

    @Test
    @DisplayName("게시물 등록 - 빈 제목 검증 실패 시 posts/new 뷰 반환")
    void create_emptyTitle_returnsNewPage() throws Exception {
        mockMvc.perform(post("/posts/new")
                        .with(user(testUserDetails))
                        .with(csrf())
                        .param("title", "")
                        .param("content", "내용은 있음"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("posts/new"));
    }

    // ──────────────────── 게시물 수정 ────────────────────

    @Test
    @DisplayName("게시물 수정 성공 - 수정 후 게시글 상세로 리다이렉트")
    void updatePost_success() throws Exception {
        mockMvc.perform(post("/posts/{id}/update", testPost.getId())
                        .with(user(testUserDetails))
                        .with(csrf())
                        .param("title", "수정된 제목")
                        .param("content", "수정된 내용입니다."))
                .andDo(print())
                .andExpect(status().is3xxRedirection());

        Post updated = postRepository.findById(testPost.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("수정된 제목");
    }

    // ──────────────────── 게시물 삭제 ────────────────────

    @Test
    @DisplayName("게시물 삭제 성공 - 삭제 후 /posts로 리다이렉트")
    void delete_success() throws Exception {
        mockMvc.perform(post("/posts/{id}/delete", testPost.getId())
                        .with(user(testUserDetails))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        assertThat(postRepository.findById(testPost.getId())).isEmpty();
    }

    @Test
    @DisplayName("게시물 삭제 - redirectInfo=mypage 파라미터 시 마이페이지로 리다이렉트")
    void delete_withMypageRedirect() throws Exception {
        mockMvc.perform(post("/posts/{id}/delete", testPost.getId())
                        .with(user(testUserDetails))
                        .with(csrf())
                        .param("redirectInfo", "mypage"))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mypage"));
    }
}






