package myex.shopping.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "MEMBER") // USER는 H2 예약어. 충돌 남.
// 실제 DB 테이블명 : Member
// 엔티티 클래스명 : User, JPQL 쿼리는 User로.
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @NotEmpty(message = "이메일을 입력해주세요")
    @Column(nullable = false, unique = true)
    private String email;
    // @NotEmpty(message = "사용자 이름을 입력해주세요")
    private String name;
    // @NotEmpty(message = "비밀번호를 입력해주세요")
    // @Size(min = 3, max = 15, message = "패스워드는 3자 이상 15자 이하 입니다.")
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cart> carts = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Comment> comments = new ArrayList<>();

    // 소프트 삭제
    private boolean active;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    // JPA 전용 기본 생성자 - 없을 시 InstantiationException,
    // JPA 구현체 Hibernate 는 데베에서 조회한 데이터로 엔티티 객체 만들 때, 먼저 기본 생성자를 호출하여 텅 빈 객체 생성 후,
    // 객체 필드에 데이터 값을 채워 넣는 방식으로 동작.

    public User() {
    }

    public User(String email, String name, String password) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.role = Role.USER;
    }

    @Override
    public String toString() {
        return "User{" + "email='" + email + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        User user = (User) o;
        return Objects.equals(getId(), user.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    // 연관관계 편의 메소드
    public void addCart(Cart cart) {
        carts.add(cart);
        cart.setUser(this);
    }

    public void deleteCart(Cart cart) {
        carts.remove(cart);
        cart.setUser(null);
    }

}
