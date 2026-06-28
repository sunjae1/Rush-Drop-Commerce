package myex.shopping.service;

import jakarta.persistence.EntityManager;
import myex.shopping.domain.Category;
import myex.shopping.domain.Item;
import myex.shopping.dto.categorydto.CategoryDTO;
import myex.shopping.dto.categorydto.CategoryCreateDTO;
import myex.shopping.dto.categorydto.CategoryEditDTO;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.jpa.JpaCategoryRepository;
import myex.shopping.support.RedisBackedSpringBootTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class CategoryServiceIntegrationTest extends RedisBackedSpringBootTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private JpaCategoryRepository categoryRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SessionFactory sessionFactory;

    @MockitoBean
    private ImageService imageService;

    @Test
    @DisplayName("카테고리 쓰기 작업은 읽기 전용 트랜잭션에 묶이지 않는다")
    void categoryWriteOperationsShouldPersistChanges() {
        String uniqueSuffix = UUID.randomUUID().toString();
        String initialName = "category-" + uniqueSuffix;
        String updatedName = "category-updated-" + uniqueSuffix;

        CategoryCreateDTO createDTO = new CategoryCreateDTO();
        createDTO.setName(initialName);

        Long categoryId = null;
        try {
            Category createdCategory = categoryService.createCategory(createDTO);
            categoryId = createdCategory.getId();

            assertThat(categoryId).isNotNull();
            assertThat(categoryRepository.findById(categoryId))
                    .map(Category::getName)
                    .hasValue(initialName);

            CategoryEditDTO editDTO = new CategoryEditDTO();
            editDTO.setName(updatedName);

            Category updatedCategory = categoryService.updateCategory(categoryId, editDTO);

            assertThat(updatedCategory.getName()).isEqualTo(updatedName);
            assertThat(categoryRepository.findById(categoryId))
                    .map(Category::getName)
                    .hasValue(updatedName);

            categoryService.deleteCategory(categoryId);

            assertThat(categoryRepository.findById(categoryId)).isEmpty();
        } finally {
            if (categoryId != null) {
                Optional<Category> category = categoryRepository.findById(categoryId);
                category.ifPresent(categoryRepository::delete);
            }
        }
    }

    @Test
    @DisplayName("카테고리 목록 조회는 두 번의 벌크 쿼리로 대표 이미지와 상품 수를 구성한다")
    void findAll_shouldBuildCategoryCardsWithTwoQueries() {
        when(imageService.generatePresignedUrl(any())).thenAnswer(invocation -> "https://cdn.test/" + invocation.getArgument(0, String.class));

        Category featuredCategory = new Category();
        featuredCategory.setName("featured");
        categoryRepository.save(featuredCategory);

        Category emptyCategory = new Category();
        emptyCategory.setName("empty");
        categoryRepository.save(emptyCategory);

        Item visibleItem = new Item("visible-item", 1000, 3, "images/featured.jpg");
        visibleItem.changeCategory(featuredCategory);
        itemRepository.save(visibleItem);

        Item deletedItem = new Item("deleted-item", 2000, 4, "images/deleted.jpg");
        deletedItem.changeCategory(featuredCategory);
        itemRepository.save(deletedItem);
        itemRepository.deleteItem(deletedItem.getId());

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();

        List<CategoryDTO> categories = categoryService.findAll();

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);

        CategoryDTO featuredDto = categories.stream()
                .filter(category -> category.getId().equals(featuredCategory.getId()))
                .findFirst()
                .orElseThrow();
        CategoryDTO emptyDto = categories.stream()
                .filter(category -> category.getId().equals(emptyCategory.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(featuredDto.getItemCount()).isEqualTo(1);
        assertThat(featuredDto.getRepresentativeImageUrl()).isEqualTo("https://cdn.test/images/featured.jpg");
        assertThat(emptyDto.getItemCount()).isZero();
        assertThat(emptyDto.getRepresentativeImageUrl()).isNull();
    }
}
