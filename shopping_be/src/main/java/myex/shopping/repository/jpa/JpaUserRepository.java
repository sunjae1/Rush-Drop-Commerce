package myex.shopping.repository.jpa;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.User;
import myex.shopping.repository.UserRepository;
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
public class JpaUserRepository implements UserRepository {

    private final EntityManager em;

    @Override
    @Transactional(readOnly = false)
    public User save(User user) {
        if (user.getId() == null) {
            em.persist(user); //GenerationType.IDENTITY -> INSERT 문 바로 실행.
        }
        else {
            em.merge(user);
        }
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(em.find(User.class, id));
    }

    @Override
    public Optional<User> findByEmail(String email) {

        //항상 DB 컬럼명이 아닌, 엔티티 기준으로 작성.

        return em.createQuery("select u from User u where u.email =:email", User.class)
                .setParameter("email", email)
                .getResultStream()
                .findFirst();
        //getSingleResult()는 NoResultException 으로 따로 잡아야 함.
        //getResultStream().findFirst() 결과 없으면 자동으로 Optional.empty()반환.
    }

    @Override
    public List<User> findByName(String name) {
        return em.createQuery("select u from User u " +
                        "where u.name =:name", User.class)
                .setParameter("name", name)
                .getResultList();
        //getSingleResult() :조회가 하나일때 사용하는 메소드
        //결과가 0건 NoResultException 발생.
        //결과가 2건 이상 NonUniqueResultException 발생.
    }

    @Override
    public List<User> findAll() {
        return em.createQuery("select u from User u", User.class)
                .getResultList();
    }

    @Override
    public List<User> findAllByActiveTrue() {
        return em.createQuery("select u from User u " +
                        "where u.active = true", User.class)
                .getResultList();
    }

}
