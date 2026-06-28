package myex.shopping.dto.userdto;

import myex.shopping.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PrincipalDetails implements UserDetails {

    private final User user;

    public PrincipalDetails(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // User 엔티티의 Role을 사용하여 권한 부여 (ex: "ROLE_USER" 또는 "ROLE_ADMIN")
        String roleName = "ROLE_" + user.getRole().name();
        return Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // UserDetails의 getUsername()은 식별자(이 경우 이메일)을 반환.
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정이 만료되지 않았음.
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // 계정이 잠기지 않았음.
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 자격 증명(비밀번호)이 만료되지 않았음.
    }

    @Override
    public boolean isEnabled() {
        // User 엔티티의 active 필드를 사용하여 계정 활성화 상태를 반환
        return user.isActive();
    }
}
