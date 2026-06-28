package myex.shopping.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.dto.itemdto.ItemDto;
import myex.shopping.form.ItemAddForm;
import myex.shopping.form.ItemEditForm;
import myex.shopping.service.ItemService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/*
Put, Patch는 클라이언트가 이미 성공값을 가지고 요청을 한거기 때문에, 서버는 "수정 완료" 만 보내면 된다.
(성공했고, 반환값이 없다) No Content 204 응답 사용 가능.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/items")
@Tag(name = "Item", description = "상품 관련 API")
@Validated
public class ApiItemController {

    private final ItemService itemService;

    @Operation(summary = "전체 상품 조회", description = "모든 상품을 조회합니다.", responses = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "삭제된 상품 조회 시 관리자 권한 필요")
    })
    // 전체 아이템 조회
    @PreAuthorize("#deleted == false or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<ItemDto>> items(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Long categoryId,
                                               @RequestParam(required = false, defaultValue = "false") boolean deleted) {
        return ResponseEntity.ok(itemService.findItems(keyword, categoryId, deleted));
    }

    @Operation(summary = "개별 아이템 조회", description = "개별 아이템을 조회합니다.", responses = {
            @ApiResponse(responseCode = "200", description = "개별 아이템 조회 성공"),
            @ApiResponse(responseCode = "404", description = "아이템이 없음")
    })
    // 개별 아이템 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> item(@PathVariable @Positive(message = "양수만 입력 가능합니다.") Long id) {
        ItemDto itemDto = itemService.findByIdToDto(id);
        return ResponseEntity.ok(itemDto);
    }

    @Operation(summary = "상품 추가 등록", description = "상품 정보를 새로 등록합니다.", responses = {
            @ApiResponse(responseCode = "201", description = "등록 성공")
    })
    // 아이템 추가 로직
    // AddForm, 프론트에서 imageFile, itemName, price, quantity 넘어옴.
    // JSON + File => form-data(multipart/form-data)
    // 원래 Form은 url에 key=value로 전송.
    // multipart/form-data는 각 input 파트로 나눠서 전송.(텍스트+파일 가능)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ItemDto> addItem(@Valid @ModelAttribute ItemAddForm form,
                                           BindingResult bindingResult) throws IOException, BindException {
        log.info("ItemController AddItem : PostMapping");
        validateImageFile(form, bindingResult);
        validateDropSaleFields(
                form.getDropProduct(),
                form.getDropStartsAt(),
                form.getDropEndsAt(),
                form.getDropPurchaseLimit(),
                bindingResult
        );
        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }
        Long savedItemId = itemService.createItem(form);
        ItemDto savedItem = itemService.findByIdToDto(savedItemId);
        return ResponseEntity.created(URI.create("/api/items/" + savedItemId)).body(savedItem);
    }

    private void validateImageFile(ItemAddForm form, BindingResult bindingResult) {
        if ((form.getImageFile() == null || form.getImageFile().isEmpty())
                && !bindingResult.hasFieldErrors("imageFile")) {
            bindingResult.rejectValue("imageFile", "required", "상품 이미지를 선택해주세요.");
        }
    }

    // 아이템 수정 :상세 + 수정화면 공용
    // 수정 화면이 필요하면 GET /items/{itemId} 에서 현재 상태를 가져와서 수정 폼에 채움.
    /*
     * 실무 설계.
     * GET /items -> 전체 목록 조회
     * GET /items/{itemId} -> 단일 아이템 조회
     * PUT /items/{itemId} -> 수정 요청
     */

    @Operation(summary = "한 상품 수정", description = "상품 하나를 수정합니다.", responses = {
            @ApiResponse(responseCode = "200", description = "수정 성공")
    })
    // 한개만 수정 : PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ItemDto> editItem(@PathVariable @Positive(message = "양수만 입력 가능합니다") Long id,
                                            @Valid @ModelAttribute ItemEditForm form,
                                            BindingResult bindingResult) throws IOException, BindException {
        validateDropSaleFields(
                form.getDropProduct(),
                form.getDropStartsAt(),
                form.getDropEndsAt(),
                form.getDropPurchaseLimit(),
                bindingResult
        );
        if (bindingResult.hasErrors()) {
            throw new BindException(bindingResult);
        }
        Long updateItemId = itemService.editItemWithUUID(form, id);
        ItemDto updatedItem = itemService.findByIdToDto(updateItemId);
        return ResponseEntity.ok(updatedItem);
    }

    @Operation(summary = "상품 삭제", description = "상품 정보를 삭제합니다.", responses = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "아이템 찾지 못함")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable @Positive(message = "양수만 입력 가능합니다.") Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build(); // 204
    }

    private void validateDropSaleFields(Boolean dropProduct,
                                        java.time.LocalDateTime dropStartsAt,
                                        java.time.LocalDateTime dropEndsAt,
                                        Integer dropPurchaseLimit,
                                        BindingResult bindingResult) {
        if (!Boolean.TRUE.equals(dropProduct)) {
            return;
        }

        if (dropStartsAt == null) {
            bindingResult.rejectValue("dropStartsAt", "required", "드롭 판매 시작 시간을 입력해주세요.");
        }

        if (dropEndsAt == null) {
            bindingResult.rejectValue("dropEndsAt", "required", "드롭 판매 종료 시간을 입력해주세요.");
        }

        if (dropStartsAt != null && dropEndsAt != null && !dropEndsAt.isAfter(dropStartsAt)) {
            bindingResult.rejectValue("dropEndsAt", "invalid", "드롭 판매 종료 시간은 시작 시간 이후여야 합니다.");
        }

        if (dropPurchaseLimit == null || dropPurchaseLimit < 1) {
            bindingResult.rejectValue("dropPurchaseLimit", "invalid", "1인 구매 제한 수량은 1개 이상이어야 합니다.");
        }
    }

}
