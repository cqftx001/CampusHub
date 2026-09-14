package com.campushub.marketplace.repository;

import com.campushub.marketplace.domain.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    Optional<Brand> findBySlug(String slug);

    List<Brand> findAllByActiveTrueOrderByDisplayOrderAscDisplayNameAsc();
}
