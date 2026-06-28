package myex.shopping.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import myex.shopping.domain.Category;
import myex.shopping.dto.categorydto.CategoryCreateDTO;
import myex.shopping.dto.categorydto.CategoryDTO;
import myex.shopping.dto.categorydto.CategoryEditDTO;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.repository.jpa.JpaCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final JpaCategoryRepository categoryRepository;
    private final ImageService imageService;

    // 전체 조회
    public List<CategoryDTO> findAll() {
        Map<Long, String> representativeImagesByCategoryId = categoryRepository.findRepresentativeImages().stream()
                .collect(Collectors.toMap(
                        JpaCategoryRepository.CategoryRepresentativeImage::getCategoryId,
                        image -> resolveDisplayImageUrl(image.getImageUrl())
                ));

        return categoryRepository.findCategoryCardSummaries().stream()
                .map(summary -> toCategoryDto(summary, representativeImagesByCategoryId))
                .collect(Collectors.toList());
    }

    private CategoryDTO toCategoryDto(JpaCategoryRepository.CategoryCardSummary summary,
                                      Map<Long, String> representativeImagesByCategoryId) {
        return toCategoryDto(summary, representativeImagesByCategoryId::get);
    }

    private CategoryDTO toCategoryDto(JpaCategoryRepository.CategoryCardSummary summary,
                                      Function<Long, String> representativeImageResolver) {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setId(summary.getId());
        categoryDTO.setName(summary.getName());
        categoryDTO.setItemCount(Math.toIntExact(summary.getItemCount()));
        categoryDTO.setRepresentativeImageUrl(representativeImageResolver.apply(summary.getId()));
        return categoryDTO;
    }

    // 단일 조회
    public CategoryDTO findById(Long id) {
        JpaCategoryRepository.CategoryCardSummary summary = categoryRepository.findCategoryCardSummaryById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        return toCategoryDto(summary, categoryId -> categoryRepository.findRepresentativeImageByCategoryId(categoryId)
                .stream()
                .map(JpaCategoryRepository.CategoryRepresentativeImage::getImageUrl)
                .map(this::resolveDisplayImageUrl)
                .findFirst()
                .orElse(null));
    }

    // 등록
    @Transactional
    public Category createCategory(@Valid CategoryCreateDTO createDTO) {
        Category category = new Category();
        category.setName(createDTO.getName());
        return categoryRepository.save(category);
    }

    // 수정
    @Transactional
    public Category updateCategory(Long id, CategoryEditDTO updateDTO) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        // PATCH -> null :기존 값 유지, notNull -> 수정.
        if (updateDTO.getName() != null) {
            category.setName(updateDTO.getName());
        }

        // em.merge
        return categoryRepository.save(category);
    }

    // 삭제
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("category not found"));
        categoryRepository.delete(category);
    }

    private String resolveDisplayImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "/image/shopping.png";
        }
        if (imageUrl.startsWith("/") || imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        return imageService.generatePresignedUrl(imageUrl);
    }
}
