package myex.shopping;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import myex.shopping.domain.*;
import myex.shopping.repository.*;
import myex.shopping.repository.jpa.JpaCategoryRepository;
import myex.shopping.repository.memory.MemoryCommentRepository;
import myex.shopping.repository.memory.MemoryItemRepository;
import myex.shopping.repository.memory.MemoryOrderRepository;
import myex.shopping.service.PostService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Profile("dev")
@Component //클래스 레벨에서 스프링 빈으로 등록
@RequiredArgsConstructor
public class TestDataInit {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;

    private final PostService postService;

    /**
     * 테스트용 데이터 추가
     */

    //@PostConstruct //스프링 빈이 생성되고, 의존성 주입이 끝난 뒤 호출되는 메서드 (빈이 다 준비되면 자동으로 메소드 실행
//    @Transactional
//    @EventListener(ApplicationReadyEvent.class)
    public void init() {

        Item itemA = new Item("남성 스투시 반팔 상의", 2000, 10,"/image/1.webp");
        itemRepository.save(itemA);
        itemRepository.save(new Item("남성 블루종 레더 자켓 블랙", 4000,20, "/image/2.webp"));

        itemRepository.save(new Item("여성 더블페이스 울 롱 코트", 100000,30, "/image/women_coat1.jpeg"));
        itemRepository.save(new Item("여성 머플러하프코트", 50000,50, "/image/women_coat2.webp"));
        itemRepository.save(new Item("남성 코듀로이 셔츠 브라운", 50000,50, "/image/man_shirt.webp"));


        User user = new User("test@na.com","테스터","test!");
        userRepository.save(user);

        Integer price = itemA.getPrice();
        int quantity = 3;

        //OrderItem, Order 테스트 데이터 생성.
        OrderItem orderItem = new OrderItem(itemA, price, quantity);
        Order order = new Order(user);
        order.addOrderItem(orderItem);
        orderRepository.save(order);
        order.confirmOrder();

        //게시글 등록
        Post post = new Post("첫 글 축하", "테스트용 게시글입니다. 게시글 입니다.\n게시글 게시글 게시글");
//        post.addUser(user);
        post.setAuthor(user.getName());
        post.setCreatedDate(LocalDateTime.now());
        postService.addUser(post, user.getId());

        //댓글 등록
        Comment comment = new Comment();
        comment.setUser(user);
        comment.setContent("테스트용 댓글 입력 중입니다. \n테스트 테스트  테스트 테스트 ");
        post.addComment(comment);
//        commentRepository.save(comment); Cascade 활용.
    }
}
