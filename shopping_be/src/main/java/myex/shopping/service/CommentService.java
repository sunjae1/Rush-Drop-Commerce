package myex.shopping.service;

import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import myex.shopping.domain.Comment;
import myex.shopping.domain.Post;
import myex.shopping.domain.User;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.form.CommentForm;
import myex.shopping.repository.CommentRepository;
import myex.shopping.repository.PostRepository;
import myex.shopping.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

        private final PostRepository postRepository;
        private final CommentRepository commentRepository;
        private final UserRepository userRepository;
        private final EntityManager em;

        // 댓글 추가
        public Comment addComment(Long postId, CommentForm form, User loginUser) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ResourceNotFoundException("Post Not Found"));
                Comment comment = new Comment();
                comment.setUser(loginUser);
                comment.setContent(form.getContent());
                post.addComment(comment); // 연관관계 편의 메소드
                // em.persist(comment);
                return comment;
        }

        // 댓글 수정
        public Comment updateComment(Long commentId, CommentForm form, Long userId) {
                Comment comment = commentRepository.findById(commentId)
                                .orElseThrow(() -> new ResourceNotFoundException("Comment Not Found"));
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User Not Found"));

                comment.setContent(form.getContent());
                // 더티체킹
                return comment;
        }

        // 댓글 삭제
        public void deleteComment(Long postId, Long commentId) {
                // 영속성이 관리.
                // 이미 컨트롤러에서 검증해서 2차 검증.
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ResourceNotFoundException("Post Not Found"));
                Comment comment = commentRepository.findById(commentId)
                                .orElseThrow(() -> new ResourceNotFoundException("Comment Not Found"));

                // orphanRemoval 안했을 때(기본값 : false), 연관관계 주인(One) 쪽에서 없애는 코드 있으면 그때 영속성이 관리한다면
                // UPDATE 쿼리 나감.(post_id만 null 처리)
                // + Transactional 없으면 준영속으로 UPDATE 쿼리 안나감.
                post.deleteComment(comment); // 외래키만 제거 : UPDATE 문. -> post_id를 null 처리만 함.

                // commentRepository.delete(commentId); //comment 레코드 자체를 제거. -> post_id 외래 키도
                // 함께 사라짐.
                /*
                 * 최적화 : update, delete 문 둘 다 같이 나가지 않음. update 외래키 null 예약하고, delete comment_id
                 * 있으면 delete만 보내서 하는 식으로
                 * JPA 가 최적화 실행.
                 */
        }

        // 댓글 작성자 본인인지 확인 메소드
        public boolean isCommentOwner(Long id, User loginUser) {
                if (loginUser == null) {
                        return false;
                }
                Comment comment = commentRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Comment Not Found"));
                // equals boolean 반환.
                return comment.getUser().getId().equals(loginUser.getId());

        }

}
