package myex.shopping.controller.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.User;
import myex.shopping.dto.postdto.PostDBDto;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.dto.userdto.UserDto;
import myex.shopping.form.CommentForm;
import myex.shopping.service.CommentService;
import myex.shopping.service.PostService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/posts")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final PostService postService;

    // 댓글 추가
    @PostMapping("/{postId}/comments")
    public String addComment(@PathVariable Long postId,
                             @Valid @ModelAttribute CommentForm form,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal PrincipalDetails principalDetails,
                             Model model) {
        User loginUser = principalDetails != null ? principalDetails.getUser() : null;
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
            model.addAttribute("loginUser", userDto);
        }
        // ModelAttribute는 {postId} 경로변수도 객체 멤버변수에 있으면 바인딩 가능.
        log.info("로그인 유저 : {}", loginUser);
        log.info("CommentForm  정보 : {}", form);
        if (bindingResult.hasErrors()) {
            log.info("댓글 폼 검증 오류 : {}", bindingResult);
            PostDBDto postDBDto = postService.changeToDto(postId);
            model.addAttribute("post", postDBDto);
            return "posts/view";
        }
        commentService.addComment(postId, form, loginUser);
        return "redirect:/posts/{postId}";
    }

    // 댓글 수정
    @PostMapping("/{postId}/comments/{commentId}/update")
    public String updateComment(@PathVariable Long postId,
                                @PathVariable Long commentId,
                                @Valid @ModelAttribute("commentToUpdate") CommentForm form,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal PrincipalDetails principalDetails,
                                Model model) {
        // 폼 바인딩 확인
        log.info("CommentForm 정보: {}", form);
        // 로그인 확인
        User loginUser = principalDetails != null ? principalDetails.getUser() : null;
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
            model.addAttribute("loginUser", userDto);
        }
        // 검증 로직
        if (bindingResult.hasErrors()) {
            log.info("댓글 수정 폼 검증 오류 : {}", bindingResult);
            PostDBDto postDBDto = postService.changeToDto(postId);
            model.addAttribute("post", postDBDto);
            model.addAttribute("errorCommentId", commentId);
            // 'commentToUpdate' 와 그에 대한 BindingResult 추가.
            // 새 댓글 상자를 위한 깨끗한 form 객체만 추가.
            model.addAttribute("commentForm", new CommentForm());
            return "/posts/view";
        }
        // 작성자 본인만 수정 가능.
        if (loginUser != null && commentService.isCommentOwner(commentId, loginUser)) {
            commentService.updateComment(commentId, form, loginUser.getId());
            log.info("댓글 수정 메소드 후 commentService.updateComment 후");
        }
        return "redirect:/posts/{postId}";
    }

    // 댓글 삭제
    @PostMapping("/{postId}/comments/{commentId}")
    public String deleteComment(@PathVariable Long postId,
                                @PathVariable Long commentId,
                                @AuthenticationPrincipal PrincipalDetails principalDetails) {
        User loginUser = principalDetails != null ? principalDetails.getUser() : null;
        // 작성자 본인만 삭제 가능
        if (commentService.isCommentOwner(commentId, loginUser)) {
            commentService.deleteComment(postId, commentId);
        }
        // 삭제 성공&실패와 상관없이 다시 게시글 상세 페이지로
        return "redirect:/posts/{postId}";
    }
}
