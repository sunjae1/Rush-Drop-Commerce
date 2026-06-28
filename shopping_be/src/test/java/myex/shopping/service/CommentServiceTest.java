package myex.shopping.service;

import myex.shopping.domain.Comment;
import myex.shopping.domain.Post;
import myex.shopping.domain.User;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.form.CommentForm;
import myex.shopping.repository.CommentRepository;
import myex.shopping.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("포스트에 댓글을 추가한다")
    void addComment() {
        // given
        Long postId = 1L;
        User user = new User();
        user.setId(1L);
        Post post = new Post();
        CommentForm form = new CommentForm();
        form.setContent("New Comment");

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // when
        Comment comment = commentService.addComment(postId, form, user);

        // then
        assertThat(comment.getContent()).isEqualTo("New Comment");
        assertThat(comment.getUser()).isEqualTo(user);
        assertThat(post.getComments()).contains(comment);
    }

    @Test
    @DisplayName("존재하지 않는 포스트에 댓글 추가 시 예외가 발생한다")
    void addComment_PostNotFound() {
        // given
        Long postId = 1L;
        CommentForm form = new CommentForm();
        form.setContent("New Comment");
        User user = new User();

        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.addComment(postId, form, user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post Not Found");
    }

    @Test
    @DisplayName("댓글을 삭제한다")
    void deleteComment() {
        // given
        Long postId = 1L;
        Long commentId = 1L;
        Post post = new Post();
        Comment comment = new Comment();
        comment.setId(commentId);
        post.addComment(comment);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        // when
        commentService.deleteComment(postId, commentId);

        // then
        assertThat(post.getComments()).isEmpty();
    }

    @Test
    @DisplayName("댓글 삭제 시 포스트가 없으면 예외가 발생한다")
    void deleteComment_PostNotFound() {
        // given
        Long postId = 1L;
        Long commentId = 1L;
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(postId, commentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Post Not Found");
    }

    @Test
    @DisplayName("댓글 삭제 시 댓글이 없으면 예외가 발생한다")
    void deleteComment_CommentNotFound() {
        // given
        Long postId = 1L;
        Long commentId = 1L;
        Post post = new Post();
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(postId, commentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Comment Not Found");
    }
}
