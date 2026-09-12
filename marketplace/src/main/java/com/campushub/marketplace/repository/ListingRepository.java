package com.campushub.marketplace.repository;

import com.campushub.marketplace.domain.Listing;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID> {

    @EntityGraph(
            attributePaths = {
                    "labels",
                    "labels.parent"
            }
    )
    @Query("""
            SELECT DISTINCT listing
            FROM Listing listing
            WHERE listing.id = :listingId
            """)
    Optional<Listing> findByIdWithLabels(
            @Param("listingId") UUID listingId
    );

}
