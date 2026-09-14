package com.campushub.marketplace.repository;

import com.campushub.marketplace.domain.Category;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    @EntityGraph(attributePaths = "parent")
    Optional<Category> findBySlug(String slug);

}
