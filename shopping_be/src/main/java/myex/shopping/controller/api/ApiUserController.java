package myex.shopping.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import myex.shopping.dto.userdto.PrincipalDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.Cart;
import myex.shopping.domain.User;
import myex.shopping.dto.mypagedto.MyPageDto;
import myex.shopping.dto.orderdto.OrderDto;
import myex.shopping.dto.postdto.PostDto;
import myex.shopping.dto.userdto.UserDto;
import myex.shopping.dto.userdto.UserEditDto;
import myex.shopping.dto.userdto.UserResponse;
import myex.shopping.form.RegisterForm;
import myex.shopping.service.CartService;
import myex.shopping.service.ImageService;
import myex.shopping.service.ItemService;
import myex.shopping.service.OrderService;
import myex.shopping.service.PostService;
import myex.shopping.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//POST/PUT/PATCH/DELETE(등록, 수정, 삭제) : 성공/실패 여부 따라
//201 CREATED, 204 NO CONTENT, 400 BAD REQUEST, 404 NOT FOUND 등
//다양한 상태 코드 필요. --> ResponseEntity<DTO> 반환.

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "User", description = "사용자 관련 API") // Open API Spec 설명.
@Validated // 단일 값 @RequestParam, @PathVariable 검증 시 필요.
public class ApiUserController {

        // 프론트엔드에서 뷰를 뿌리고 Api 에서는, 프론트에서 오는 정보를 서버에 거쳐 처리하고 다시 JSON 으로 반환. (뷰가 필요없어서
        // GetMapping 거의 다 제거.)

        // 생성자 주입.
        private final UserService userService;
        private final ItemService itemService;
        private final OrderService orderService;
        private final PostService postService;
        private final CartService cartService;
        private final ImageService imageService;

        // 로그인
        // Spring Security 대신 처리.
        @Operation(summary = "전체 사용자 조회", description = "관리자가 전체 사용자를 조회합니다.", responses = {
                        @ApiResponse(responseCode = "200", description = "조회 성공") })
        // 전체 사용자 조회
        @GetMapping("/users")
        public ResponseEntity<List<UserDto>> allUser() {
                return ResponseEntity.ok(
                                userService.allUser().stream()
                                                .map(UserDto::new)
                                                .toList());
        }

        // 전체 활성화 사용자 조회
        @GetMapping("/users/active")
        public ResponseEntity<List<UserDto>> allUserByActive() {
                return ResponseEntity.ok(
                                userService.allUserByActive().stream()
                                                .map(UserDto::new)
                                                .toList());
        }

        @Operation(summary = "메인페이지(쇼핑몰), 전체 상품 조회", description = "메인페이지로 들어가서 전체 상품을 조회합니다.", responses = {
                        @ApiResponse(responseCode = "200", description = "전체 상품 조회 성공"),
                        @ApiResponse(responseCode = "401", description = "로그인 실패(계정 정보 없음(세션))")
        })
        // 메인 페이지, 상품 전체 조회. (record 확인 한번 하기)
        @GetMapping
        public ResponseEntity<UserResponse> mainPage(@AuthenticationPrincipal PrincipalDetails principalDetails) {
                if (principalDetails == null) {
                        log.info("로그인 실패");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                User loginUser = principalDetails.getUser();
                UserDto userDto = new UserDto(loginUser);
                return ResponseEntity.ok(new UserResponse(userDto, itemService.findAllToDto()));
        }

        @Operation(summary = "마이 페이지 요청", description = "사용자의 Cart, Order, Post 를 조회합니다.", responses = {
                        @ApiResponse(responseCode = "401", description = "로그인 실패"),
                        @ApiResponse(responseCode = "200", description = "조회 성공")
        })
        // 마이페이지 보내는거 : user, orders, posts, cart
        @GetMapping("/myPage")
        public ResponseEntity<MyPageDto> myPage(@AuthenticationPrincipal PrincipalDetails principalDetails) {
                if (principalDetails == null) {
                        log.info("로그인 실패");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                User loginUser = principalDetails.getUser();
                List<OrderDto> orders = orderService.findOrderDtosByUser(loginUser);
                List<PostDto> posts = postService.findPostDtosByUser(loginUser);
                Cart cart = cartService.findOrCreateCartForUser(loginUser);
                MyPageDto dto = new MyPageDto(loginUser, orders, posts, cart);
                imageService.resolveImageUrls(dto.getCartItems());
                return ResponseEntity.ok(dto);
        }

        @Operation(summary = "회원가입", description = "회원가입 등록", responses = {
                        @ApiResponse(responseCode = "201", description = "회원가입 등록 성공")
        })
        // 회원가입
        @PostMapping("/register")
        public ResponseEntity<UserDto> addUser(@Valid @RequestBody RegisterForm form) {
                User user = new User(form.getEmail(), form.getName(), form.getPassword());
                userService.save(user);
                return ResponseEntity.status(HttpStatus.CREATED).body(new UserDto(user));
        }

        @Operation(summary = "회원 정보 수정", description = "로그인된 사용자의 정보를 수정합니다.", responses = {
                        @ApiResponse(responseCode = "200", description = "회원 정보 수정 성공"),
                        @ApiResponse(responseCode = "401", description = "로그인 필요")
        })
        // 회원 정보 수정
        @PutMapping("/users")
        public ResponseEntity<UserDto> updateUser(@Valid @RequestBody UserEditDto updateDTO,
                        @AuthenticationPrincipal PrincipalDetails principalDetails) {
                if (principalDetails == null) {
                        log.info("수정 실패 - 로그인 필요");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                User loginUser = principalDetails.getUser();
                User updatedUser = userService.updateUser(loginUser.getId(), updateDTO);
                log.info("사용자 정보 수정 완료: {}", updatedUser);
                return ResponseEntity.ok(new UserDto(updatedUser));
        }

        // 회원 정보 삭제
        @DeleteMapping("/users")
        public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal PrincipalDetails principalDetails) {
                if (principalDetails == null) {
                        log.info("탈퇴 실패 - 로그인 필요");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                User loginUser = principalDetails.getUser();
                userService.deleteUser(loginUser.getId());
                log.info("사용자 탈퇴 완료");
                return ResponseEntity.noContent().build(); // 204
        }
}
