package myex.shopping.repository.jpa;

import myex.shopping.domain.User;
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
@Import(JpaUserRepository.class)
class JpaUserRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JpaUserRepository userRepository;

    @Test
    @DisplayName("새로운 사용자 저장 테스트")
    void save() {
        // given
        User user = new User("test@example.com", "tester", "password123");

        // when
        User savedUser = userRepository.save(user);

        // then
        assertThat(savedUser.getId()).isNotNull();
        User foundUser = em.find(User.class, savedUser.getId());
        assertThat(foundUser.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("ID로 사용자 조회")
    void findById() {
        // given
        User user = new User("findbyid@example.com", "finduser", "password");
        em.persistAndFlush(user);

        // when
        Optional<User> foundUserOpt = userRepository.findById(user.getId());

        // then
        assertThat(foundUserOpt).isPresent();
        assertThat(foundUserOpt.get().getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("존재하지 않는 ID로 사용자 조회")
    void findById_NotFound() {
        // when
        Optional<User> foundUserOpt = userRepository.findById(999L);

        // then
        assertThat(foundUserOpt).isNotPresent();
    }


    @Test
    @DisplayName("이메일로 사용자 조회")
    void findByEmail() {
        // given
        User user = new User("findbyemail@example.com", "emailuser", "password");
        em.persistAndFlush(user);

        // when
        Optional<User> foundUserOpt = userRepository.findByEmail("findbyemail@example.com");

        // then
        assertThat(foundUserOpt).isPresent();
        assertThat(foundUserOpt.get().getEmail()).isEqualTo("findbyemail@example.com");
    }
    
    @Test
    @DisplayName("존재하지 않는 이메일로 사용자 조회")
    void findByEmail_NotFound() {
        // when
        Optional<User> foundUserOpt = userRepository.findByEmail("nonexistent@example.com");

        // then
        assertThat(foundUserOpt).isNotPresent();
    }

    @Test
    @DisplayName("이름으로 사용자 조회")
    void findByName() {
        // given
        User user1 = new User("user1@example.com", "tester", "pw1");
        User user2 = new User("user2@example.com", "tester", "pw2");
        em.persist(user1);
        em.persistAndFlush(user2);

        // when
        List<User> foundUsers = userRepository.findByName("tester");

        // then
        assertThat(foundUsers).hasSize(2);
        assertThat(foundUsers).extracting(User::getName).containsOnly("tester");
    }

    @Test
    @DisplayName("모든 사용자 조회")
    void findAll() {
        // given
        User user1 = new User("user1@findAll.com", "user1", "pw1");
        User user2 = new User("user2@findAll.com", "user2", "pw2");
        em.persist(user1);
        em.persistAndFlush(user2);

        // when
        List<User> allUsers = userRepository.findAll();

        // then
        assertThat(allUsers).hasSize(2);
    }
}
