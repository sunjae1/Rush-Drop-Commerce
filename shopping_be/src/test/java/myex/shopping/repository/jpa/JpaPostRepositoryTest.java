package myex.shopping.repository.jpa;

import myex.shopping.domain.Comment;
import myex.shopping.domain.Post;
import myex.shopping.domain.User;
import org.hibernate.Hibernate;
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
@Import(JpaPostRepository.class)
class JpaPostRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JpaPostRepository postRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("postuser@example.com", "postuser", "password");
        em.persistAndFlush(user);
        em.clear();
    }

    @Test
    @DisplayName("새 게시물 저장")
    void save() {
        User managedUser = em.find(User.class, user.getId());
        Post post = new Post("New Post", "Content of new post");
        post.setUser(managedUser);

        Post savedPost = postRepository.save(post);
        em.flush();
        em.clear();

        assertThat(savedPost.getId()).isNotNull();
        Post foundPost = em.find(Post.class, savedPost.getId());
        assertThat(foundPost).isNotNull();
        assertThat(foundPost.getTitle()).isEqualTo("New Post");
        assertThat(foundPost.getUser().getId()).isEqualTo(managedUser.getId());
    }

    @Test
    @DisplayName("ID로 게시물 조회")
    void findById() {
        User managedUser = em.find(User.class, user.getId());
        Post post = new Post("Find Me Post", "Content");
        post.setUser(managedUser);
        em.persistAndFlush(post);
        Long postId = post.getId();
        em.clear();

        Optional<Post> foundPostOpt = postRepository.findById(postId);

        assertThat(foundPostOpt).isPresent();
        assertThat(foundPostOpt.get().getId()).isEqualTo(postId);
        assertThat(Hibernate.isInitialized(foundPostOpt.get().getComments())).isTrue();
    }

    @Test
    @DisplayName("사용자로 게시물 목록 조회")
    void findByUser() {
        User managedUser = em.find(User.class, user.getId());
        Post post1 = new Post("Post 1", "Content 1");
        post1.setUser(managedUser);
        em.persist(post1);

        Post post2 = new Post("Post 2", "Content 2");
        post2.setUser(managedUser);
        em.persist(post2);

        em.flush();
        em.clear();

        User queryUser = em.find(User.class, managedUser.getId());
        List<Post> posts = postRepository.findByUser(queryUser);

        assertThat(posts).hasSize(2);
    }

    @Test
    @DisplayName("모든 게시물 조회(목록용): user fetch join, comments lazy")
    void findAll() {
        User managedUser = em.find(User.class, user.getId());
        Post post1 = new Post("Post 1", "Content 1");
        post1.setUser(managedUser);

        Comment comment1 = new Comment();
        comment1.setContent("Comment for Post 1");
        comment1.setUser(managedUser);
        post1.addComment(comment1);
        em.persist(post1);

        User user2 = new User("user2@example.com", "user2", "password123");
        em.persist(user2);
        Post post2 = new Post("Post 2", "Content 2");
        post2.setUser(user2);
        em.persist(post2);

        em.flush();
        em.clear();

        List<Post> allPosts = postRepository.findAll();

        assertThat(allPosts).hasSize(2);
        assertThat(allPosts.get(0).getUser()).isNotNull();

        Post foundPost1 = allPosts.stream()
                .filter(p -> p.getId().equals(post1.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(Hibernate.isInitialized(foundPost1.getComments())).isFalse();
    }

    @Test
    @DisplayName("게시물 삭제")
    void delete() {
        User managedUser = em.find(User.class, user.getId());
        Post post = new Post("To be deleted", "Delete me");
        post.setUser(managedUser);
        em.persistAndFlush(post);
        Long postId = post.getId();
        em.clear();

        postRepository.deleteById(postId);
        em.flush();
        em.clear();

        Post deletedPost = em.find(Post.class, postId);
        assertThat(deletedPost).isNull();
    }
}