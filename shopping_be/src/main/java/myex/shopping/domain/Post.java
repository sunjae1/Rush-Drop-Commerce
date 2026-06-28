package myex.shopping.domain;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@Entity
public class Post {

    //게시판 : DB id, 제목, 내용, 글쓴이

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty(message = "제목을 입력해주세요.")
    private String title;
    @NotEmpty(message = "내용을 입력해주세요")
    private String content;

    private String author;

    //User로 바꾸고, @ManyToOne?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    private LocalDateTime createdDate;

    //cascade는 em 엔티티 기준 같이 쿼리 나감. orphanRemoval은 객체 기준, 객체 삭제시 자동으로 자식 객체 고아 삭제.
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();
    public Post() {
    }

    public Post(String title, String content) {
        this.title = title;
        this.content = content;
    }

    //연관관계 편의 메소드(Comment)
    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setPost(this);
    }

    public void deleteComment(Comment comment)
    {
        comments.remove(comment);
        comment.setPost(null);
    }

    //연관관계 편의 메소드 (Many에 생성)
    public void addUser(User user) {
        //추가할려는데 이미 user가 있다면 user.posts List 에서 현재 Post 제거.
        if (this.user !=null) {
            this.user.getPosts().remove(this);
        }
        //나 에게 부모를 설정(Post - User) (FK 설정)
        this.user = user;
        //부모(User)의 컬렉션에 나(Post)를 추가 (객체 그래프 일관성 유지)

        //detached된 준영속 상태 User loginUser 에서 OneToMany 이기 때문에 기본 LAZY 라서, user(준영속).getPosts() 를 하면
        // (지연 로딩으로 설정된 연관 데이터를 가져오려고 시도하면 LazyInitializationException 이 발생.)
        user.getPosts().add(this);
    }

    public void deleteUser() {
        //기존 부모(User)의 자식 리스트에서 나(Post)를 제거.
        if (this.user !=null) {
            this.user.getPosts().remove(this);
        }
        this.user = null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return Objects.equals(getId(), post.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
