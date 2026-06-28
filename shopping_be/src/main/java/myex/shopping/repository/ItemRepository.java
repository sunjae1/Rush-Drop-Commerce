package myex.shopping.repository;

import myex.shopping.domain.Item;
import myex.shopping.dto.itemdto.ItemDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;


public interface ItemRepository {

    Item save(Item item);
    Optional<Item> findById(Long id);
    Optional<Item> findByIdForUpdate(Long id);
    List<Item> findAll();
    List<Item> findAll(boolean deleted);
    void update(Long itemId, Item updateParam);
    void deleteItem(Long itemId);

    List<Item> searchByName(String keyword);
    List<Item> searchByName(String keyword, boolean deleted);

    List<Item> findByCategory(Long categoryId);
    List<Item> findByCategory(Long categoryId, boolean deleted);

    List<Item> findByCategoryAndName(Long categoryId, String keyword);
    List<Item> findByCategoryAndName(Long categoryId, String keyword, boolean deleted);
}
