package myex.shopping.controller.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.Category;
import myex.shopping.domain.User;
import myex.shopping.dto.categorydto.CategoryCreateDTO;
import myex.shopping.dto.categorydto.CategoryDTO;
import myex.shopping.dto.categorydto.CategoryEditDTO;
import myex.shopping.dto.userdto.UserDto;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.repository.jpa.JpaCategoryRepository;
import myex.shopping.service.CategoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;
    private final JpaCategoryRepository categoryRepository;

    @GetMapping
    public String list(Model model,
                       HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user",userDto);
        }
        List<CategoryDTO> categories = categoryService.findAll();
        model.addAttribute("categories", categories);
        return "categories/list";
    }

    @GetMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String addForm(Model model,
                          HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user",userDto);
        }
        model.addAttribute("category", new CategoryCreateDTO());
        return "categories/addForm";
    }

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public String addCategory(@Valid @ModelAttribute("category") CategoryCreateDTO categoryCreateDTO,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes,
                              HttpSession session,
                              Model model) {

        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user",userDto);
        }
        if (bindingResult.hasErrors()) {
            log.info("카테고리 생성 폼 검증 실패: {}", bindingResult);
            return "categories/addForm";
        }
        Category savedCategory = categoryService.createCategory(categoryCreateDTO);
        redirectAttributes.addFlashAttribute("message", "카테고리가 성공적으로 생성되었습니다.");
        return "redirect:/categories";
    }

    @GetMapping("/{categoryId}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editForm(@PathVariable Long categoryId,
                           Model model,
                           HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user",userDto);
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        model.addAttribute("category", new CategoryDTO(category));
        return "categories/editForm";
    }

    @PostMapping("/{categoryId}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editCategory(@PathVariable Long categoryId,
                               @Valid @ModelAttribute("category") CategoryEditDTO categoryEditDTO,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model,
                               HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user",userDto);
        }
        if (bindingResult.hasErrors()) {
            log.info("카테고리 수정 폼 검증 실패: {}", bindingResult);
            return "categories/editForm";
        }
        categoryService.updateCategory(categoryId, categoryEditDTO);
        redirectAttributes.addFlashAttribute("message", "카테고리가 성공적으로 수정되었습니다.");
        return "redirect:/categories";
    }

    @PostMapping("/{categoryId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteCategory(@PathVariable Long categoryId, RedirectAttributes redirectAttributes) {
        categoryService.deleteCategory(categoryId);
        redirectAttributes.addFlashAttribute("message", "카테고리가 성공적으로 삭제되었습니다.");
        return "redirect:/categories";
    }
}
