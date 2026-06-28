package myex.shopping.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import myex.shopping.domain.Category;
import myex.shopping.repository.jpa.JpaCategoryRepository;
import myex.shopping.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaCategoryRepository categoryRepository;

    @MockitoBean
    private ImageService imageService;

    private Category existingCategory;

    @BeforeEach
    void setUp() {
        when(imageService.generatePresignedUrl(any())).thenReturn("https://cdn.test/category.jpg");

        existingCategory = new Category();
        existingCategory.setName("기존 카테고리");
        categoryRepository.save(existingCategory);
    }

    @Test
    @WithMockUser
    @DisplayName("카테고리 목록 조회는 일반 로그인 사용자도 가능하다")
    void getCategories_shouldAllowAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("카테고리 생성은 관리자만 가능하다")
    void createCategory_shouldForbidNonAdmin() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "신규 카테고리"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("관리자는 카테고리를 생성할 수 있다")
    void createCategory_shouldCreateForAdmin() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "신규 카테고리"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("신규 카테고리"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("관리자는 카테고리를 수정할 수 있다")
    void updateCategory_shouldUpdateForAdmin() throws Exception {
        mockMvc.perform(put("/api/categories/{id}", existingCategory.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "수정된 카테고리"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("수정된 카테고리"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("관리자는 카테고리를 삭제할 수 있다")
    void deleteCategory_shouldDeleteForAdmin() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", existingCategory.getId()))
                .andExpect(status().isNoContent());

        assertThat(categoryRepository.findById(existingCategory.getId())).isEmpty();
    }
}
