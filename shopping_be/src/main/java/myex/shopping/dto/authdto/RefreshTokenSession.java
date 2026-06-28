package myex.shopping.dto.authdto;

import myex.shopping.domain.Role;
import myex.shopping.domain.User;

public record RefreshTokenSession(
        Long userId,
        String email,
        String name,
        String role,
        String sessionId
) {

    public User toUser() {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setName(name);
        user.setRole(Role.valueOf(role));
        user.setActive(true);
        return user;
    }
}
