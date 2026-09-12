package com.campushub.marketplace.repository;

import com.campushub.marketplace.domain.Label;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LabelRepository extends JpaRepository<Label, UUID> {

    @EntityGraph(attributePaths = "parent")
    List<Label> findAllByActiveTrueOrderByDisplayOrderAscSlugAsc();

    @EntityGraph(attributePaths = "parent")
    List<Label> findAllBySlugInAndActiveTrue(Collection<String> slugs);
}
