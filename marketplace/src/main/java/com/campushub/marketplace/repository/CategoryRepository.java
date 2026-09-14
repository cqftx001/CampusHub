package com.campushub.marketplace.repository;

import com.campushub.marketplace.domain.Category;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @EntityGraph(attributePaths = "parent")
    Optional<Category> findBySlug(String slug);

    @Query("""
            select category
            from Category category
            left join fetch category.parent parent
            where category.active = true
              and (
                    category.parent is null
                    or parent.active = true
              )
            order by
                category.displayOrder asc,
                category.displayName asc
            """)
    List<Category> findAvailableCatalogCategories();
}
