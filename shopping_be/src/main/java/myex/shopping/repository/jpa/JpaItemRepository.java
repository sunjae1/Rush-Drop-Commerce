package myex.shopping.repository.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import myex.shopping.domain.Item;
import myex.shopping.repository.ItemRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Primary
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaItemRepository implements ItemRepository {

    private final EntityManager em;

    @Override
    @Transactional(readOnly = false)
    public Item save(Item item) {
        //persist : "새로운 엔티티" 라고 생각. 이때, id == null 이면 INSERT 쿼리,
        // id 가 값이 있으면, "준영속 객체인데 persist 하려 한다." -> PersistentObjectException 예외 발생.
        //@GeneratedValue 없이, setId() 하는데 중복 되면, PK 중복 ConstraintViolationException 발생.

        item.setDeleted(false); //명시적으로 deleted 상태를 false로 설정
        if (item.getId() == null) {
            em.persist(item);
        }
        else {
            em.merge(item);
        }
        return item;
    }

    @Override
    public Optional<Item> findById(Long id) {
        List<Item> result = em.createQuery("select i from Item i left join fetch i.category where i.id = :id and i.deleted = false", Item.class)
                .setParameter("id", id)
                .getResultList();
        return result.stream().findAny(); //결과가 없거나(0개), 1개이므로 findAny() 사용.
    }

    @Override
    public Optional<Item> findByIdForUpdate(Long id) {
        List<Item> result = em.createQuery("select i from Item i left join fetch i.category where i.id = :id and i.deleted = false", Item.class)
                .setParameter("id", id)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
        return result.stream().findAny();
    }

    @Override
    public List<Item> findAll() {
        return findAll(false);
    }

    @Override
    public List<Item> findAll(boolean deleted) {
        return em.createQuery("select i from Item i left join fetch i.category where i.deleted = :deleted", Item.class)
                .setParameter("deleted", deleted)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = false)
    public void update(Long itemId, Item updateParam) {
        Item item = em.find(Item.class, itemId);
        if (item == null) {
            return;
        }
        item.setItemName(updateParam.getItemName());
        item.setPrice(updateParam.getPrice());
        item.setQuantity(updateParam.getQuantity());
        item.setImageUrl(updateParam.getImageUrl());
        item.changeCategory(updateParam.getCategory());
        item.configureDropSale(
                updateParam.isDropProduct(),
                updateParam.getDropStartsAt(),
                updateParam.getDropEndsAt(),
                updateParam.getDropPurchaseLimit()
        );
    }


    @Override
    @Transactional(readOnly = false)
    //소프트 삭제로 변경.
    public void deleteItem(Long itemId) {
        //em.remove는 영속 객체를 매개변수로 받음.
        Item item = em.find(Item.class, itemId);
        if (item !=null) {
            item.setDeleted(true); //소프트 삭제 : deleted 필드를 true로 설정
            em.merge(item);
        }
    }
    @Override
    @Transactional(readOnly = true)
    public List<Item> searchByName(String name) {
        return searchByName(name, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> searchByName(String name, boolean deleted) {
        return em.createQuery("SELECT i FROM Item i left join fetch i.category where i.deleted = :deleted and i.itemName like :name", Item.class)
                .setParameter("deleted", deleted)
                .setParameter("name", "%" + name + "%")    //중요 : 검색어 앞뒤에 %를 직접 붙여줘야 함
                .getResultList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Item> findByCategory(Long categoryId) {
        return findByCategory(categoryId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Item> findByCategory(Long categoryId, boolean deleted) {
        return em.createQuery("SELECT i FROM Item i left join fetch i.category where i.deleted = :deleted and i.category.id = :categoryId", Item.class)
                .setParameter("deleted", deleted)
                .setParameter("categoryId", categoryId)
                .getResultList();
    }

    @Override
    public List<Item> findByCategoryAndName(Long categoryId, String keyword) {
        return findByCategoryAndName(categoryId, keyword, false);
    }

    @Override
    public List<Item> findByCategoryAndName(Long categoryId, String keyword, boolean deleted) {
        return em.createQuery("SELECT i FROM Item i left join fetch i.category WHERE i.deleted = :deleted AND i.category.id = :categoryId AND i.itemName LIKE :keyword", Item.class)
                .setParameter("deleted", deleted)
                .setParameter("categoryId", categoryId)
                .setParameter("keyword", "%" + keyword + "%")
                .getResultList();
    }
}
