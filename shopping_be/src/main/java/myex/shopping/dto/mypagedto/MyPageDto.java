package myex.shopping.dto.mypagedto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import myex.shopping.domain.Cart;
import myex.shopping.domain.User;
import myex.shopping.dto.postdto.PostDto;
import myex.shopping.dto.itemdto.ItemDto;
import myex.shopping.dto.orderdto.OrderDto;
import myex.shopping.dto.userdto.UserDto;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Schema(description = "MyPage 보낼 데이터들(User, Order, Post, CartItem)DTO")
public class MyPageDto {
    @Schema(description = "사용자 정보",example = "user")
    private UserDto user;
    @Schema(description = "주문 정보",example = "[Order1, Order2]")
    private List<OrderDto> orders;
    @Schema(description = "등록한 게시물들",example = "[Post1, Post2]")
    private List<PostDto> posts;
    @Schema(description = "장바구니 상품들",example = "[cartItem1, cartItem2]")
    private List<ItemDto> cartItems;

    public MyPageDto(User user, List<OrderDto> orders, List<PostDto> posts, Cart cart) {
        this.user = new UserDto(user);
        this.orders = orders;
        this.posts = posts;
        if (cart.getCartItems() != null) {
            this.cartItems = cart.getCartItems().stream()
                    .map(oi -> new ItemDto(oi.getItem()))
                    .collect(Collectors.toList());
        }
    }
}
