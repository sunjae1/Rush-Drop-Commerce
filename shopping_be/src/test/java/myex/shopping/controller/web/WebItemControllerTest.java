package myex.shopping.controller.web;

import myex.shopping.domain.Category;
import myex.shopping.domain.Item;
import myex.shopping.domain.Role;
import myex.shopping.domain.User;
import myex.shopping.dto.itemdto.ItemDto;
import myex.shopping.dto.itemdto.ItemDtoDetail;
import myex.shopping.dto.itemdto.ItemEditDto;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.UserRepository;
import myex.shopping.repository.jpa.JpaCategoryRepository;
import myex.shopping.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WebItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private JpaCategoryRepository categoryRepository;

    @MockitoBean
    private ImageService imageService;

    private PrincipalDetails adminDetails;
    private MockHttpSession session;
    private Category topCategory;
    private Category bottomCategory;
    private Category emptyCategory;
    private Item existingItem;
    private Item secondItem;

    @BeforeEach
    void setUp() throws Exception {
        User admin = new User("admin-item@example.com", "관리자", "password");
        admin.setActive(true);
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        adminDetails = new PrincipalDetails(admin);
        session = new MockHttpSession();
        session.setAttribute("loginUser", admin);

        topCategory = new Category();
        topCategory.setName("상의");
        categoryRepository.save(topCategory);

        bottomCategory = new Category();
        bottomCategory.setName("하의");
        categoryRepository.save(bottomCategory);

        emptyCategory = new Category();
        emptyCategory.setName("액세서리");
        categoryRepository.save(emptyCategory);

        existingItem = new Item("기존 상품", 10000, 5, "images/existing.jpg");
        existingItem.changeCategory(topCategory);
        itemRepository.save(existingItem);

        secondItem = new Item("다른 카테고리 상품", 20000, 8, "images/second.jpg");
        secondItem.changeCategory(bottomCategory);
        itemRepository.save(secondItem);

        when(imageService.storeFile(any())).thenReturn("images/uploaded.jpg");
        when(imageService.resolveImageUrls(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageService.resolveImageUrl(any(ItemDto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageService.resolveImageUrl(any(ItemEditDto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageService.resolveImageUrl(any(ItemDtoDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageService.generatePresignedUrl(any())).thenAnswer(invocation -> "https://cdn.test/" + invocation.getArgument(0, String.class));
    }

    @Test
    @DisplayName("카테고리 페이지는 비로그인 사용자도 접근 가능하고 대표 이미지와 필터 링크를 제공한다")
    void categoriesPage_shouldExposeFilteredLinksForAnonymousUser() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("categories/list"))
                .andExpect(content().string(containsString("https://cdn.test/images/existing.jpg")))
                .andExpect(content().string(containsString("https://cdn.test/images/second.jpg")))
                .andExpect(content().string(containsString(emptyCategory.getName())))
                .andExpect(content().string(containsString("카테고리에 상품을 추가해 주세요")))
                .andExpect(content().string(containsString("/?categoryId=" + topCategory.getId())))
                .andExpect(content().string(containsString("/?categoryId=" + bottomCategory.getId())))
                .andExpect(content().string(containsString("/?categoryId=" + emptyCategory.getId())));
    }

    @Test
    @DisplayName("메인 페이지는 검색 바와 같은 줄에 카테고리 필터를 렌더링하고 categoryId로 상품을 필터링한다")
    void mainPage_shouldFilterItemsByCategory() throws Exception {
        mockMvc.perform(get("/")
                        .param("categoryId", topCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(content().string(containsString("catalog-toolbar")))
                .andExpect(content().string(containsString("category-filter-chip")))
                .andExpect(content().string(containsString(topCategory.getName())))
                .andExpect(content().string(containsString(bottomCategory.getName())))
                .andExpect(content().string(containsString(existingItem.getItemName())))
                .andExpect(content().string(not(containsString(secondItem.getItemName()))))
                .andExpect(content().string(containsString("/?categoryId=" + topCategory.getId())));
    }

    @Test
    @DisplayName("메인 페이지 카테고리 필터 링크는 검색어를 유지한다")
    void mainPage_categoryFilterShouldKeepKeyword() throws Exception {
        String response = mockMvc.perform(get("/")
                        .param("keyword", "기존"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("keyword=%EA%B8%B0%EC%A1%B4");
        assertThat(response).contains("categoryId=" + topCategory.getId());
        assertThat(response).contains("categoryId=" + bottomCategory.getId());
    }

    @Test
    @DisplayName("상품 등록 폼은 category 선택 필드를 렌더링한다")
    void addForm_shouldRenderCategoryOptions() throws Exception {
        mockMvc.perform(get("/items/add")
                        .with(user(adminDetails))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("items/addForm"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(content().string(containsString("categoryId")))
                .andExpect(content().string(containsString(topCategory.getName())))
                .andExpect(content().string(containsString(bottomCategory.getName())));
    }

    @Test
    @DisplayName("상품 수정 폼은 현재 category와 선택 목록을 함께 렌더링한다")
    void editForm_shouldRenderCurrentCategoryAndOptions() throws Exception {
        mockMvc.perform(get("/items/{itemId}/edit", existingItem.getId())
                        .with(user(adminDetails))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("items/editForm"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(content().string(containsString("categoryId")))
                .andExpect(content().string(containsString(topCategory.getName())))
                .andExpect(content().string(containsString(bottomCategory.getName())));
    }

    @Test
    @DisplayName("SSR 상품 등록은 category를 함께 저장한다")
    void addItem_shouldPersistCategory() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "new-image.jpg", "image/jpeg", "image-content".getBytes());

        mockMvc.perform(multipart("/items/add")
                        .file(imageFile)
                        .with(user(adminDetails))
                        .with(csrf())
                        .session(session)
                        .param("itemName", "카테고리 포함 신규 상품")
                        .param("price", "15000")
                        .param("quantity", "7")
                        .param("categoryId", bottomCategory.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/items/*"));

        Item savedItem = itemRepository.findAll().stream()
                .filter(item -> "카테고리 포함 신규 상품".equals(item.getItemName()))
                .max(Comparator.comparing(Item::getId))
                .orElseThrow();

        assertThat(savedItem.getCategory()).isNotNull();
        assertThat(savedItem.getCategory().getId()).isEqualTo(bottomCategory.getId());
    }

    @Test
    @DisplayName("SSR 상품 수정은 category 변경을 저장한다")
    void editItem_shouldUpdateCategory() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile", "edited-image.jpg", "image/jpeg", "edited-content".getBytes());

        mockMvc.perform(multipart("/items/{itemId}/edit", existingItem.getId())
                        .file(imageFile)
                        .with(user(adminDetails))
                        .with(csrf())
                        .session(session)
                        .param("itemName", "수정된 상품")
                        .param("price", "18000")
                        .param("quantity", "3")
                        .param("categoryId", bottomCategory.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/items/*"));

        Item updatedItem = itemRepository.findById(existingItem.getId()).orElseThrow();
        assertThat(updatedItem.getItemName()).isEqualTo("수정된 상품");
        assertThat(updatedItem.getCategory()).isNotNull();
        assertThat(updatedItem.getCategory().getId()).isEqualTo(bottomCategory.getId());
    }

    @Test
    @DisplayName("관리자 상품 페이지는 categoryId 필터로 목록을 좁힌다")
    void itemsPage_shouldFilterItemsByCategory() throws Exception {
        mockMvc.perform(get("/items")
                        .with(user(adminDetails))
                        .session(session)
                        .param("categoryId", topCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("items/items"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(content().string(containsString(existingItem.getItemName())))
                .andExpect(content().string(not(containsString(secondItem.getItemName()))));
    }
}
