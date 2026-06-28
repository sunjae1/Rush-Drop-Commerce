package myex.shopping.repository.memory;

import myex.shopping.domain.Item;
import myex.shopping.repository.ItemRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class MemoryItemRepository implements ItemRepository {

    //@Repository 하면 스프링이 클래스 단위로 싱글톤 보장 해줘서, 멤버변수에 static 할 필요 없음.
    private static Long sequence = 0L;
    private static final Map<Long, Item> store = new HashMap<>();

    //상품 저장
    @Override
    public Item save(Item item) {

        item.setId(++sequence);
        store.put(sequence, item);
        return item;
    }
    //상품 조회(id, key 로 조회)
    @Override
    public Optional<Item> findById(Long id) {
        return Optional.ofNullable(store.get(id))
                .filter(item -> !item.isDeleted());
    }

    @Override
    public Optional<Item> findByIdForUpdate(Long id) {
        return findById(id);
    }

    //전체 상품 조회
    @Override
    public List<Item> findAll() {
        return findAll(false);
    }

    @Override
    public List<Item> findAll(boolean deleted) {
        return store.values().stream()
                .filter(item -> item.isDeleted() == deleted)
                .toList();
    }

    //업데이트 처리(이름, 가격, 재고 수정)
    @Override
    public void update(Long itemId, Item updateParam) {
        Optional<Item> byId = findById(itemId);
        Item findItem = byId.get();
        findItem.setItemName(updateParam.getItemName());
        findItem.setPrice(updateParam.getPrice());
        findItem.setQuantity(updateParam.getQuantity());
        findItem.setImageUrl(updateParam.getImageUrl());
        findItem.changeCategory(updateParam.getCategory());
        findItem.configureDropSale(
                updateParam.isDropProduct(),
                updateParam.getDropStartsAt(),
                updateParam.getDropEndsAt(),
                updateParam.getDropPurchaseLimit()
        );

    }

    public void update_exceptImgUrl(Long itemId, Item updateParam){
        Optional<Item> byId = findById(itemId);
        Item findItem = byId.get();
        findItem.setItemName(updateParam.getItemName());
        findItem.setPrice(updateParam.getPrice());
        findItem.setQuantity(updateParam.getQuantity());
        findItem.configureDropSale(
                updateParam.isDropProduct(),
                updateParam.getDropStartsAt(),
                updateParam.getDropEndsAt(),
                updateParam.getDropPurchaseLimit()
        );
    }

    @Override
    public void deleteItem(Long itemId) {
        Item item = store.get(itemId);
        if (item != null) {
            item.setDeleted(true);
        }
    }

    @Override
    public List<Item> searchByName(String keyword) {
        return searchByName(keyword, false);
    }

    @Override
    public List<Item> searchByName(String keyword, boolean deleted) {
        String normalizedKeyword = keyword == null ? "" : keyword.toLowerCase();
        return store.values().stream()
                .filter(item -> item.isDeleted() == deleted)
                .filter(item -> item.getItemName() != null && item.getItemName().toLowerCase().contains(normalizedKeyword))
                .toList();
    }

    @Override
    public List<Item> findByCategory(Long categoryId) {
        return findByCategory(categoryId, false);
    }

    @Override
    public List<Item> findByCategory(Long categoryId, boolean deleted) {
        return store.values().stream()
                .filter(item -> item.isDeleted() == deleted)
                .filter(item -> item.getCategory() != null && Objects.equals(item.getCategory().getId(), categoryId))
                .toList();
    }

    @Override
    public List<Item> findByCategoryAndName(Long categoryId, String keyword) {
        return findByCategoryAndName(categoryId, keyword, false);
    }

    @Override
    public List<Item> findByCategoryAndName(Long categoryId, String keyword, boolean deleted) {
        String normalizedKeyword = keyword == null ? "" : keyword.toLowerCase();
        return store.values().stream()
                .filter(item -> item.isDeleted() == deleted)
                .filter(item -> item.getCategory() != null && Objects.equals(item.getCategory().getId(), categoryId))
                .filter(item -> item.getItemName() != null && item.getItemName().toLowerCase().contains(normalizedKeyword))
                .toList();
    }

    //item 저장소 전부 삭제.
    public void clearStore() {
        store.clear();
    }


}
