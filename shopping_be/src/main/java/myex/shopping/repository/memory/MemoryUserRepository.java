package myex.shopping.repository.memory;

import myex.shopping.domain.User;
import myex.shopping.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

//@Bean 등록이랑 DB 접근 예외를 스프링 예외로 변환하는 기능 가짐.
@Repository
public class MemoryUserRepository implements UserRepository {

    //@Repository 이런 어노테이션에 다 싱글톤 보장 : 해당 클래스의 객체가 딱 하나만 만들어지도록. / 근데 예제에서 명시적으로 private static 처럼 싱글톤 흉내내기 위해.

/*
    이처럼 static을 사용하면, MemoryUserRepository를 사용하는 모든 테스트 클래스에서 @AfterEach로
    clearStore()를 호출해줘야 한다는 암묵적인 규칙이 생깁니다.
    이 규칙을 단 한 곳이라도 잊어버리면, 전체 테스트는 실행 순서에 따라 통과하거나 실패하는, 잡기 매우 어려운 버그의 원인이 됩니다.
     반면에 static을 제거하면, 상태는 MemoryUserRepository '인스턴스'에 귀속됩니다. Spring 테스트 컨텍스트는 기본적으로 테스트 클래스 단위로 캐싱되므로, UserServiceTest의 UserRepository 인스턴스와
    ItemServiceTest의 UserRepository 인스턴스가 사용하는 map은 (설정에 따라) 분리될 가능성이 높습니다.
    훨씬 더 안전한 구조입니다.  ==> static을 제거해라.
*/

    private HashMap<Long, User> map = new HashMap<>();
    private Long sequence = 0L;



    @Override
    public User save(User user) {
        user.setId(++sequence);
        map.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(map.get(id));

    }

    @Override
    public Optional<User> findByEmail(String email) {

        Collection<User> values = map.values();

        //두개 합쳐서 향상된 for문 가능.(이것도 iterator를 쓰기 때문에 구현 되있어야함).
        Iterator<User> iterator = values.iterator();
        while(iterator.hasNext())
        {
            User next = iterator.next();
            if (next.getEmail().equals(email)) {
                return Optional.of(next);
            }
        }
        return Optional.empty();

    /*    Collection<User> value2 = map.values();
        for (User user : value2) {
            if (user.getEmail().equals(email)) {
                return user;
            }
        }*/



    }

    @Override
    public List<User> findByName(String name) {
        return map.values().stream()
                .filter(user -> user.getName().equals(name))
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(map.values());
    }

    @Override
    public List<User> findAllByActiveTrue() {
        return List.of();
    }

    //보통 테스트에서 사용.
    //JPA에선 @Transactional 안에 @Test 작성시 자동으로 Rollback 해줌.(clearStore는 Memory 에서만 필요)
    public void clearStore() {
        map.clear();
    }

}
