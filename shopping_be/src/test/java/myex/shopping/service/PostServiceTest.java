package myex.shopping.service;

import myex.shopping.domain.Post;
import myex.shopping.domain.User;
import myex.shopping.dto.mypagedto.MyPagePostDBDto;
import myex.shopping.dto.postdto.PostDBDto;
import myex.shopping.dto.postdto.PostListDto;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.repository.PostRepository;
import myex.shopping.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostService postService;

    @Test
    @DisplayName("포스트에 사용자를 추가하고 저장한다")
    void addUser() {
        // given
        User user = new User();
        user.setId(1L);
        Post post = new Post("Test Title", "Test Content");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        Post result = postService.addUser(post, 1L);

        // then
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(user.getPosts()).contains(result);
    }

    @Test
    @DisplayName("포스트 ID로 DTO를 조회한다")
    void changeToDto() {
        // given
        User user = new User();
        user.setName("tester");
        Post post = new Post("Test Title", "Test Content");
        post.setUser(user);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        // when
        PostDBDto dto = postService.changeToDto(1L);

        // then
        assertThat(dto.getTitle()).isEqualTo("Test Title");
        assertThat(dto.getAuthorName()).isEqualTo("tester");
    }

    @Test
    @DisplayName("존재하지 않는 포스트 ID 조회 시 예외가 발생한다")
    void changeToDto_NotFound() {
        // given
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> postService.changeToDto(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post Not Found");
    }

    @Test
    @DisplayName("특정 사용자의 포스트 목록을 DTO로 변환하여 반환한다")
    void changeToDtoList() {
        // given
        User user = new User();
        user.setName("testuser"); // Set user name
        Post post1 = new Post("Title1", "Content1");
        post1.setUser(user); // Set user for post1
        Post post2 = new Post("Title2", "Content2");
        post2.setUser(user); // Set user for post2
        user.getPosts().addAll(Arrays.asList(post1, post2));
        
        when(postRepository.findByUser(user)).thenReturn(Arrays.asList(post1, post2));

        // when
        List<MyPagePostDBDto> dtoList = postService.changeToDtoList(user);

        // then
        assertThat(dtoList).hasSize(2);
        assertThat(dtoList.get(0).getTitle()).isEqualTo("Title1");
        assertThat(dtoList.get(0).getAuthorName()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("모든 포스트 목록을 DTO로 변환하여 반환한다")
    void findAllPostListDto() {
        // given
        Post post1 = new Post("Title1", "Content1");
        post1.setUser(new User());
        Post post2 = new Post("Title2", "Content2");
        post2.setUser(new User());
        when(postRepository.findAll()).thenReturn(Arrays.asList(post1, post2));

        // when
        List<PostListDto> dtoList = postService.findAllPostListDto();

        // then
        assertThat(dtoList).hasSize(2);
        assertThat(dtoList.get(0).getTitle()).isEqualTo("Title1");
    }
}
