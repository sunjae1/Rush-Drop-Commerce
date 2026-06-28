package myex.shopping.controller.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.Item;
import myex.shopping.domain.User;
import myex.shopping.dto.itemdto.ItemDto;
import myex.shopping.dto.itemdto.ItemDtoDetail;
import myex.shopping.dto.itemdto.ItemEditDto;
import myex.shopping.dto.userdto.UserDto;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.form.ItemAddForm;
import myex.shopping.form.ItemEditForm;
import myex.shopping.repository.ItemRepository;
import myex.shopping.service.CategoryService;
import myex.shopping.service.ImageService;
import myex.shopping.service.ItemService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
@Slf4j
public class ItemController {

    private final ItemRepository itemRepository; // 생성자 주입.
    private final ItemService itemService;
    private final CategoryService categoryService;
    private final ImageService imageService;

    // 전체 아이템 조회 (+검색 추가)
    @GetMapping
    public String items(Model model,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(required = false) Long categoryId,
                        HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
        }
        List<ItemDto> items = itemService.findItems(keyword, categoryId);
        model.addAttribute("items", items);
        model.addAttribute("categories", categoryService.findAll());
        return "items/items";
    }

    // 개별 아이템 조회
    @GetMapping("/{itemId}")
    public String item(@PathVariable long itemId, Model model,
                       HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
        }

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("item not found"));
        // DTO로 변환.
        ItemDtoDetail dto = new ItemDtoDetail(item);
        imageService.resolveImageUrl(dto);
        model.addAttribute("item", dto);
        return "items/item";
    }

    // 아이템 추가 폼.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/add")
    public String addForm(Model model,
                          HttpSession session) {
        addUserIfPresent(model, session);
        addCategoryOptions(model);
        model.addAttribute("item", new ItemAddForm());
        return "items/addForm";
    }

    // 아이템 추가 요청
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public String addItem(@Valid @ModelAttribute("item") ItemAddForm form,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes,
                          Model model,
                          HttpSession session) throws IOException {
        // 업로드 시 UploadFolder 에 있는 사진 업로드 시도 하면, "같은 경로 + 같은 파일명" 이라 같다고 판단해 move
        // 불가능.(오류 발생. -> UUID로 바꿀시, "같은 경로 + 다른 파일명" 이라 다른 파일이라 판단하고 업로드 가능.
        // "다른 경로 + 같은 파일명" : 덮어쓰기.
        validateImageFile(form, bindingResult);
        if (bindingResult.hasErrors()) {
            log.info("상품 폼 검증 실패 : {}", bindingResult);
            addUserIfPresent(model, session);
            addCategoryOptions(model);
            return "items/addForm";
        }
        Long savedItemId = itemService.createItem(form);
        redirectAttributes.addAttribute("itemId", savedItemId);
        return "redirect:/items/{itemId}";
    }

    private void validateImageFile(ItemAddForm form, BindingResult bindingResult) {
        if ((form.getImageFile() == null || form.getImageFile().isEmpty())
                && !bindingResult.hasFieldErrors("imageFile")) {
            bindingResult.rejectValue("imageFile", "required", "상품 이미지를 선택해주세요.");
        }
    }

    // 아이템 수정 폼.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{itemId}/edit")
    public String editForm(@PathVariable("itemId") Long itemId,
                           Model model,
                           HttpSession session) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("item not found"));
        addUserIfPresent(model, session);
        addCategoryOptions(model);

        model.addAttribute("item", imageService.resolveImageUrl(new ItemEditDto(item)));
        return "items/editForm";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{itemId}/edit")
    public String edit(@PathVariable Long itemId,
                       @Valid @ModelAttribute("item") ItemEditForm form,
                       BindingResult bindingResult,
                       Model model,
                       HttpSession session) throws IOException {
        log.info("아이템 수정 요청 컨트롤러 진입");
        log.info("form 정보 : {}", form);
        if (bindingResult.hasErrors()) {
            log.info("검증 실패 : {}", bindingResult);
            form.setId(itemId);
            addUserIfPresent(model, session);
            addCategoryOptions(model);
            return "items/editForm";
        }
        itemService.editItemWithUUID(form, itemId);
        return "redirect:/items/{itemId}";
        // {} 치환 순위.
        /*
         * 1. RedirectAttributes.addAttribute("itemId",...)
         * 2. @PathVariable, @RequestParam 같은 요청
         * 메서드 파라미터 이름 Long itemId 랑 매칭 가능.
         */
    }

    // 아이템 삭제하기.
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{itemId}/delete")
    public String deleteItem(@PathVariable Long itemId) {
        itemService.deleteItem(itemId);
        return "redirect:/items";
    }

    private void addUserIfPresent(Model model, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
        }
    }

    private void addCategoryOptions(Model model) {
        model.addAttribute("categories", categoryService.findAll());
    }
}
