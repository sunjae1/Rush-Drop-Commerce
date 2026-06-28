package myex.shopping.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.Category;
import myex.shopping.domain.Item;
import myex.shopping.dto.itemdto.ItemDto;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.form.ItemAddForm;
import myex.shopping.form.ItemEditForm;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.jpa.JpaCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final JpaCategoryRepository categoryRepository;
    private final ImageService imageService;
    private final EntityManager em;

    @Transactional(readOnly = false)
    public Item update(Long itemId, Item updateParam) {
        // 영속성 컨텍스트가 관리. (Dirty Checking)
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품을 찾을 수 없습니다."));
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

        return item;
    }

    // id itemName, price, quantity (url 없음)
    @Transactional(readOnly = true)
    public List<ItemDto> findAllToDto() {
        List<ItemDto> dtos = itemRepository.findAll()
                .stream()
                .map(ItemDto::new)
                .collect(Collectors.toList());
        return imageService.resolveImageUrls(dtos);
    }

    @Transactional(readOnly = true)
    public ItemDto findByIdToDto(Long id) {
        ItemDto dto = itemRepository.findById(id)
                .map(item -> new ItemDto(item, true))
                .orElseThrow(() -> new ResourceNotFoundException("item not found"));
        return imageService.resolveImageUrl(dto);
    }

    @Transactional(readOnly = false)
    public Long createItem(ItemAddForm form) throws IOException {
        String imageUrl = imageService.storeFile(form.getImageFile());
        Item item = new Item();
        // 기본 필드 및 이미지 URL 저장.
        item.setItemName(form.getItemName());
        item.setPrice(form.getPrice());
        item.setQuantity(form.getQuantity());
        item.setImageUrl(imageUrl);
        item.changeCategory(resolveCategory(form.getCategoryId()));
        applyDropSale(item, form.getDropProduct(), form.getDropStartsAt(), form.getDropEndsAt(), form.getDropPurchaseLimit());

        Item savedItem = itemRepository.save(item);
        return savedItem.getId();
    }

    @Transactional(readOnly = false)
    public Long editItemWithUUID(ItemEditForm form, Long itemId) throws IOException {
        String imageUrl = imageService.storeFile(form.getImageFile());
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("item not found"));
        item.setItemName(form.getItemName());
        item.setPrice(form.getPrice());
        item.setQuantity(form.getQuantity());
        item.changeCategory(resolveCategory(form.getCategoryId()));
        applyDropSale(item, form.getDropProduct(), form.getDropStartsAt(), form.getDropEndsAt(), form.getDropPurchaseLimit());
        if (imageUrl != null) {
            // 새 이미지가 업로드되면 기존 S3 이미지 삭제
            imageService.deleteFile(item.getImageUrl());
            item.setImageUrl(imageUrl);
        }
        itemRepository.save(item);
        em.flush();
        return item.getId();
    }

    @Transactional
    public void deleteItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("item not found"));
        // 아이템 삭제 시 S3 이미지도 삭제
        imageService.deleteFile(item.getImageUrl());
        itemRepository.deleteItem(itemId);
    }

    @Transactional(readOnly = true)
    public List<ItemDto> findSearchByNameDto(String keyword) {
        List<ItemDto> dtos = itemRepository.searchByName(keyword)
                .stream()
                .map(ItemDto::new)
                .collect(Collectors.toList());
        return imageService.resolveImageUrls(dtos);
    }

    @Transactional(readOnly = true)
    public List<ItemDto> findByCategory(Long categoryId) {
        List<ItemDto> dtos = itemRepository.findByCategory(categoryId)
                .stream()
                .map(ItemDto::new)
                .collect(Collectors.toList());
        return imageService.resolveImageUrls(dtos);
    }

    // 키워드 검색 && 카테고리 로 아이템 검색
    @Transactional(readOnly = true)
    public List<ItemDto> findItems(String keyword, Long categoryId) {
        return findItems(keyword, categoryId, false);
    }

    @Transactional(readOnly = true)
    public List<ItemDto> findItems(String keyword, Long categoryId, boolean deleted) {
        List<ItemDto> dtos;
        // 카테고리 && 검색어 둘 다 있는경우
        if (StringUtils.hasText(keyword) && categoryId != null) {
            dtos = itemRepository.findByCategoryAndName(categoryId, keyword, deleted)
                    .stream()
                    .map(ItemDto::new)
                    .collect(Collectors.toList());
        }
        // 검색어만 있는 경우
        else if (StringUtils.hasText(keyword)) {
            dtos = itemRepository.searchByName(keyword, deleted)
                    .stream()
                    .map(ItemDto::new)
                    .collect(Collectors.toList());
        }
        // 카테고리만 있는 경우
        else if (categoryId != null) {
            dtos = itemRepository.findByCategory(categoryId, deleted)
                    .stream()
                    .map(ItemDto::new)
                    .collect(Collectors.toList());
        }
        // 둘 다 없는 경우.
        else {
            dtos = itemRepository.findAll(deleted)
                    .stream()
                    .map(ItemDto::new)
                    .collect(Collectors.toList());
        }
        return imageService.resolveImageUrls(dtos);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }

        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("category not found"));
    }

    private void applyDropSale(Item item,
                               Boolean dropProduct,
                               java.time.LocalDateTime dropStartsAt,
                               java.time.LocalDateTime dropEndsAt,
                               Integer dropPurchaseLimit) {
        item.configureDropSale(
                Boolean.TRUE.equals(dropProduct),
                dropStartsAt,
                dropEndsAt,
                dropPurchaseLimit
        );
    }
}
