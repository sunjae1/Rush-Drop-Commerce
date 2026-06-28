package myex.shopping.repository.jpa;

import myex.shopping.domain.Comment;
import myex.shopping.domain.Post;
import myex.shopping.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaCommentRepository.class)
class JpaCommentRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JpaCommentRepository commentRepository;

    private User user;
    private Post post;

    @BeforeEach
    void setUp() {
        user = new User("commentuser@example.com", "commentuser", "password");
        em.persist(user);

        post = new Post("Test Post", "This is a test post.");
        post.setUser(user);
        em.persist(post);
        
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("새로운 댓글 저장 테스트")
    void save() {
        // given
        User managedUser = em.find(User.class, this.user.getId());
        Post managedPost = em.find(Post.class, this.post.getId());
        
        Comment comment = new Comment();
        comment.setContent("This is a test comment.");
        comment.setUser(managedUser);
        comment.setPost(managedPost);

        // when
        Comment savedComment = commentRepository.save(comment);
        em.flush();
        em.clear();

        // then
        assertThat(savedComment.getId()).isNotNull();
        Comment foundComment = em.find(Comment.class, savedComment.getId());
        assertThat(foundComment).isNotNull();
        assertThat(foundComment.getContent()).isEqualTo("This is a test comment.");
        assertThat(foundComment.getUser().getId()).isEqualTo(managedUser.getId());
        assertThat(foundComment.getPost().getId()).isEqualTo(managedPost.getId());
    }

    @Test
    @DisplayName("ID로 댓글 조회")
    void findById() {
        // given
        Comment comment = new Comment();
        comment.setContent("Find me");
        comment.setUser(em.find(User.class, user.getId()));
        comment.setPost(em.find(Post.class, post.getId()));
        em.persistAndFlush(comment);
        Long commentId = comment.getId();
        em.clear();
        
        // when
        Optional<Comment> foundCommentOpt = commentRepository.findById(commentId);

        // then
        assertThat(foundCommentOpt).isPresent();
        assertThat(foundCommentOpt.get().getId()).isEqualTo(commentId);
        assertThat(foundCommentOpt.get().getContent()).isEqualTo("Find me");
    }

    @Test
    @DisplayName("모든 댓글 조회")
    void findAll() {
        // given
        Comment comment1 = new Comment();
        comment1.setContent("Comment 1");
        comment1.setUser(em.find(User.class, user.getId()));
        comment1.setPost(em.find(Post.class, post.getId()));
        em.persist(comment1);

        Comment comment2 = new Comment();
        comment2.setContent("Comment 2");
        comment2.setUser(em.find(User.class, user.getId()));
        comment2.setPost(em.find(Post.class, post.getId()));
        em.persist(comment2);

        em.flush();
        em.clear();
        
        // when
        List<Comment> allComments = commentRepository.findAll();

        // then
        assertThat(allComments).hasSize(2);
    }

    @Test
    @DisplayName("댓글 삭제 테스트")
    void delete() {
        // given
        Comment comment = new Comment();
        comment.setContent("To be deleted");
        comment.setUser(em.find(User.class, user.getId()));
        comment.setPost(em.find(Post.class, post.getId()));
        em.persistAndFlush(comment);
        Long commentId = comment.getId();
        em.clear();
        
        // when
        commentRepository.delete(commentId);
        em.flush();
        em.clear();
        
        // then
        Comment deletedComment = em.find(Comment.class, commentId);
        assertThat(deletedComment).isNull();
    }
}
