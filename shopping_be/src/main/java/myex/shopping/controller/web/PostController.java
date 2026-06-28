package myex.shopping.controller.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.User;
import myex.shopping.dto.postdto.PostDBDto;
import myex.shopping.dto.postdto.PostListDto;
import myex.shopping.dto.postdto.PostEditDto;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.dto.userdto.UserDto;
import myex.shopping.form.CommentForm;
import myex.shopping.form.PostForm;
import myex.shopping.repository.PostRepository;
import myex.shopping.service.PostService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/posts")
// @Validated -> BindingResult 전에 예외 터트려서 전역으로 처리
// (Valid + BindingResult 이전 처리)
// 클래스 레벨 사용.
// 메소드 레벨 선언시 객체만 검증 가능.(ModelAttribute, RequestBody)
// 메서드 레벨 선언시 단일 값 검증 RequestParam, PathVariable 검증 불가.
public class PostController {
    private final PostRepository postRepository;
    private final PostService postService;

    // 게시판 조회.
    @GetMapping
    public String list(Model model,
                       @AuthenticationPrincipal PrincipalDetails principalDetails) {
        User loginUser = principalDetails != null ? principalDetails.getUser() : null;
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
            model.addAttribute("loginUser", userDto);
        }
        List<PostListDto> posts = postService.findAllPostListDto();
        model.addAttribute("posts", posts);
        return "posts/list";
    }

    // 게시물 한개 상세 보기.
    @GetMapping("/{id}")
    public String view(@PathVariable Long id,
                       Model model,
                       @AuthenticationPrincipal PrincipalDetails principalDetails) {
        log.info("post.id = {}", id);
        User loginUser = principalDetails != null ? principalDetails.getUser() : null;
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("loginUser", userDto);
            model.addAttribute("user", userDto);
        }
        PostDBDto postDBDto = postService.changeToDto(id);
        model.addAttribute("post", postDBDto);
        model.addAttribute("commentForm", new CommentForm());
        return "posts/view";
    }

    // 게시물 등록 폼
    @GetMapping("/new")
    public String createForm(Model model,
                             @AuthenticationPrincipal PrincipalDetails principalDetails) {
        User loginUser = principalDetails != null ? principalDetails.getUser() : null;
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
        }
        model.addAttribute("post", new PostForm());
        return "posts/new";
    }

    // 게시물 등록
    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("post") PostForm form,
                         BindingResult bindingResult,
                         Model model,
                         @AuthenticationPrincipal PrincipalDetails principalDetails) {
        if (principalDetails == null) {
            return "redirect:/login";
        }
        User loginUser = principalDetails.getUser();
        UserDto userDto = new UserDto(loginUser);
        model.addAttribute("user", userDto);
        if (bindingResult.hasErrors()) {
            log.info("게시물 등록 폼 검증 실패 : {}", bindingResult);
            return "posts/new";
        }
        postService.createPost(form, loginUser);
        return "redirect:/posts";
    }

    // 게시물 수정 폼
    @GetMapping("/{id}/update")
    public String updateForm(@PathVariable Long id,
                             Model model,
                             @AuthenticationPrincipal PrincipalDetails principalDetails) {
        User loginUser = principalDetails != null ? principalDetails.getUser() : null;
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
        }
        PostDBDto postDBDto = postService.changeToDto(id);
        model.addAttribute("post", postDBDto);
        return "posts/edit";
    }

    // 게시물 수정
    @PostMapping("/{id}/update")
    public String updatePost(@PathVariable Long id,
                             @Valid @ModelAttribute("post") PostEditDto form,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal PrincipalDetails principalDetails,
                             Model model) {
        log.info("게시물 수정 요청 컨트롤러 진입");
        log.info("PostForm 정보 : {}", form);
        User loginUser = principalDetails != null ? principalDetails.getUser() : null;
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
        }
        if (bindingResult.hasErrors()) {
            log.info("검증 실패 : {}", bindingResult);
            return "posts/edit";
        }
        postService.updatePost(id, form);
        return "redirect:/posts/{id}";
        // @PostMapping("/{postId}/update") 경로 변수 {postId} 는 redirect:/{postId} 플레이스홀더 값이랑 매칭 됨.
    }

    // 게시물 삭제.
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) String redirectInfo) {
        log.info("redirectInfo 값 : {}", redirectInfo);
        postRepository.deleteById(id);
        if ("mypage".equals(redirectInfo)) {
            return "redirect:/mypage";
        }
        return "redirect:/posts";
    }

}
