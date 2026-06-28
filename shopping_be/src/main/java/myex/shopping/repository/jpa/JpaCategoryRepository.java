package myex.shopping.repository.jpa;

import myex.shopping.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

//Spring Data JPA 사용.
public interface JpaCategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
            select c.id as id, c.name as name, count(i.id) as itemCount
            from Category c
            left join c.items i on i.deleted = false
            group by c.id, c.name
            order by c.id
            """)
    List<CategoryCardSummary> findCategoryCardSummaries();

    @Query("""
            select c.id as id, c.name as name, count(i.id) as itemCount
            from Category c
            left join c.items i on i.deleted = false
            where c.id = :id
            group by c.id, c.name
            """)
    Optional<CategoryCardSummary> findCategoryCardSummaryById(@Param("id") Long id);

    @Query("""
            select i.category.id as categoryId, i.imageUrl as imageUrl
            from Item i
            where i.deleted = false
              and i.category is not null
              and i.id = (
                  select min(i2.id)
                  from Item i2
                  where i2.deleted = false
                    and i2.category = i.category
              )
            """)
    List<CategoryRepresentativeImage> findRepresentativeImages();

    @Query("""
            select i.category.id as categoryId, i.imageUrl as imageUrl
            from Item i
            where i.deleted = false
              and i.category.id = :categoryId
              and i.id = (
                  select min(i2.id)
                  from Item i2
                  where i2.deleted = false
                    and i2.category = i.category
              )
            """)
    Optional<CategoryRepresentativeImage> findRepresentativeImageByCategoryId(@Param("categoryId") Long categoryId);

    interface CategoryCardSummary {
        Long getId();
        String getName();
        long getItemCount();
    }

    interface CategoryRepresentativeImage {
        Long getCategoryId();
        String getImageUrl();
    }
}
