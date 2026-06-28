package myex.shopping.service;

import myex.shopping.domain.User;
import myex.shopping.dto.userdto.UserEditDto;
import myex.shopping.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("사용자를 저장한다")
    void save() {
        // given
        User user = new User(" Test@Test.com ", "Tester", "password");
        user.setId(1L);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());

        // when
        Long savedId = userService.save(user);

        // then
        assertThat(savedId).isEqualTo(1L);
        assertThat(user.getEmail()).isEqualTo("test@test.com");
    }

    @Test
    @DisplayName("모든 사용자를 조회한다")
    void allUser() {
        // given
        User user1 = new User("test1@test.com", "Tester1", "pw1");
        User user2 = new User("test2@test.com", "Tester2", "pw2");
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        // when
        List<User> users = userService.allUser();

        // then
        assertThat(users).hasSize(2);
        assertThat(users).contains(user1, user2);
    }

    @Test
    @DisplayName("사용자 정보를 수정한다")
    void updateUser() {
        // given
        Long userId = 1L;
        User existingUser = new User("old@test.com", "OldName", "password");
        UserEditDto updateDto = new UserEditDto();
        updateDto.setName("NewName");
        updateDto.setEmail(" New@Test.com ");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());

        // when
        User updatedUser = userService.updateUser(userId, updateDto);

        // then
        assertThat(updatedUser.getName()).isEqualTo("NewName");
        assertThat(updatedUser.getEmail()).isEqualTo("new@test.com");
    }

    @Test
    @DisplayName("사용자 탈퇴 시 이메일을 익명화하고 비활성화한다")
    void deleteUser() {
        Long userId = 1L;
        User existingUser = new User("old@test.com", "OldName", "password");
        existingUser.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        userService.deleteUser(userId);

        assertThat(existingUser.isActive()).isFalse();
        assertThat(existingUser.getEmail()).startsWith("deleted__1__");
        assertThat(existingUser.getEmail()).endsWith("@deleted.local");
    }

    @Test
    @DisplayName("로그인용 이메일 조회는 소문자 정규화를 적용한다")
    void findByEmail() {
        User existingUser = new User("normalized@test.com", "Tester", "password");
        when(userRepository.findByEmail("normalized@test.com")).thenReturn(Optional.of(existingUser));

        Optional<User> foundUser = userService.findByEmail(" Normalized@Test.com ");

        assertThat(foundUser).contains(existingUser);
    }
}
