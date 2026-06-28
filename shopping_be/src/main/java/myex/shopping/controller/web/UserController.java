package myex.shopping.controller.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.*;
import myex.shopping.dto.itemdto.ItemDto;
import myex.shopping.dto.mypagedto.MyPageOrderDto;
import myex.shopping.dto.mypagedto.MyPagePostDBDto;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.dto.userdto.UserDto;
import myex.shopping.form.LoginForm;
import myex.shopping.form.RegisterForm;
import myex.shopping.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;
    private final OrderService orderService;
    private final PostService postService;
    private final CartService cartService;
    private final ItemService itemService;
    private final CategoryService categoryService;

    // 로그인 페이지로 보내기.
    @GetMapping("/login")
    public String start(Model model) {
        model.addAttribute("form", new LoginForm());
        return "login";
    }

    // 회원가입 폼 조회.
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    // 회원가입 등록.
    @PostMapping("/register")
    public String addUser(@Valid @ModelAttribute("form") RegisterForm form,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            log.info("회원 가입 검증 실패 - {}", bindingResult);
            return "register";
        }
        User user = new User(form.getEmail(), form.getName(), form.getPassword());
        userService.save(user);
        return "redirect:/login";
    }

    // 전체 회원 목록 조회.
    @GetMapping("/allUser")
    @PreAuthorize("hasRole('ADMIN')")
    public String allUser(Model model) {
        List<User> users = userService.allUser();
        model.addAttribute("users", users);
        return "allUser";
    }

    // 메인 페이지 요청 : Item, User (+검색 추가)
    @GetMapping("/")
    public String mainPage(Model model,
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {
        if (principalDetails != null) {
            User loginUser = principalDetails.getUser();
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
        }
        List<ItemDto> items = itemService.findItems(keyword, categoryId);
        model.addAttribute("items", items);
        model.addAttribute("categories", categoryService.findAll());
        return "main";
    }

    // 마이페이지 보내는거 : user, orders, posts, cart
    @GetMapping("/mypage")
    public String myPage(@AuthenticationPrincipal PrincipalDetails principalDetails,
            Model model) {
        User loginUser = principalDetails.getUser();
        UserDto userDto = new UserDto(loginUser);
        model.addAttribute("user", userDto);

        Cart cart = cartService.findOrCreateCartForUser(loginUser);
        List<MyPageOrderDto> orderDtos = orderService.changeToOrderDtoList(loginUser);
        List<MyPagePostDBDto> postDtos = postService.changeToDtoList(loginUser);

        model.addAttribute("orders", orderDtos);
        model.addAttribute("posts", postDtos);
        model.addAttribute("cart", cart);

        return "mypage/view";
    }
}
