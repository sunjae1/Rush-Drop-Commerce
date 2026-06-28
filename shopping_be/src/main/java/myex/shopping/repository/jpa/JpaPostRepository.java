package myex.shopping.repository.jpa;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.Post;
import myex.shopping.domain.User;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.repository.PostRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Primary
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaPostRepository implements PostRepository {

    private final EntityManager em;

    @Transactional(readOnly = false)
    @Override
    public Post save(Post post) {
        em.persist(post);
        return post;
    }

    @Override
    public Optional<Post> findById(Long id) {
        List<Post> result = em.createQuery("select p from Post p " +
                        "join fetch p.user " +
                        "left join fetch p.comments c " +
                        "left join fetch c.user cu " +
                        "where p.id = :id", Post.class)
                .setParameter("id", id)
                .getResultList();
        return result.stream().findFirst();
    }

    //fetch join 으로 수정.
    //User, Comments 둘 다 한 번의 쿼리로 가져옴.
    //Comments는 없을 수도 있어서 left join 쓰는게 안전.
    //Post - Comments : OneToMany 컬렉션 (List)를 조인하면 데이터가 뻥튀기 될 수 있어 Post 중복 조회 방지, distinct 추가.
    @Override
    public List<Post> findAll() {
        return em.createQuery("select p from Post p " +
                        "join fetch p.user", Post.class)
                .getResultList();
    }

    @Override
    public List<Post> findAllByCreatedDateAsc() {
        return em.createQuery("select p from Post p " +
                        "join fetch p.user " +
                        "order by p.createdDate asc", Post.class)
                .getResultList();
    }

    @Override
    public List<Post> findAllByCreatedDateDesc() {
        return em.createQuery("select p from Post p " +
                        "join fetch p.user " +
                        "order by p.createdDate desc", Post.class)
                .getResultList();
    }

    //JPQL :파라미터명(:와 파라미터명은 무조건 붙여야 됨.)
    //=: 파라미터명 (띄어쓰기 오류)
    @Override
    public List<Post> findByUser(User user) {
        return em.createQuery("select distinct p from Post p " +
                        "left join fetch p.comments c " +
                        "left join fetch c.user " +
                        "where p.user = :user", Post.class)
                .setParameter("user", user)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = false)
    public void deleteById(Long id) {
        Post post = em.find(Post.class, id);
        if (post != null) {
            log.info("Post Delete");
            em.remove(post);
        }
        else {
            throw new ResourceNotFoundException("Post not found");
        }
    }
}
