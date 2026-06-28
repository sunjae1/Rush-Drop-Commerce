package myex.shopping.dto.userdto;

import io.swagger.v3.oas.annotations.media.Schema;
import myex.shopping.dto.itemdto.ItemDto;

import java.util.List;

@Schema(description = "사용자 및 상품 통합 DTO")
public record UserResponse(UserDto userDto, List<ItemDto> itemDto)  {
    /*
    불변 객체.
    public final class UserResponse
        private final UserDto userDto;
        private final List<ItemDto> item

        //편의 메소드
        1. 생성자
        2. 접근자 getXxx() 이 아닌 이름이 같은 userDto() 로 만들어짐.
        3. equals()와 hashCode() : 모든 구성 요소 필드 값이 동일한지 비교할 수 있게 만들어짐.
        4. toString() : 사람이 읽기 쉬운 형식 문자열 반환.
     */
}
