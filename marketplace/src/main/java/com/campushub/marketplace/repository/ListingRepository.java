package com.campushub.marketplace.repository;

import com.campushub.marketplace.domain.Listing;
import com.campushub.marketplace.domain.ListingCondition;
import com.campushub.marketplace.domain.ListingStatus;
import com.campushub.marketplace.repository.projection.ListingSummaryRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface ListingRepository
        extends JpaRepository<Listing, UUID>, JpaSpecificationExecutor<Listing> {


    @EntityGraph(
            attributePaths = {
                    "category",
                    "category.parent",
                    "brand",
                    "additionalImageUrls"
            }
    )
    Optional<Listing> findByIdAndStatus(UUID id, ListingStatus status);

    @Query(
            value = """
                    select new com.campushub.marketplace.repository.projection.ListingSummaryRow(
                        listing.id,
                        category.slug,
                        category.displayName,
                        parent.slug,
                        parent.displayName,
                        brand.slug,
                        brand.displayName,
                        listing.title,
                        listing.condition,
                        listing.price,
                        listing.currency,
                        listing.location,
                        listing.deliveryMethod,
                        listing.primaryImageUrl,
                        listing.createdAt
                    )
                    from Listing listing
                    join listing.category category
                    join category.parent parent
                    left join listing.brand brand
                    where listing.status = :status
                      and (
                            :categorySlug is null
                            or category.slug = :categorySlug
                            or parent.slug = :categorySlug
                      )
                      and (
                            :brandSlug is null
                            or brand.slug = :brandSlug
                      )
                      and (
                            :condition is null
                            or listing.condition = :condition
                      )
                      and (
                            :minimumPrice is null
                            or listing.price >= :minimumPrice
                      )
                      and (
                            :maximumPrice is null
                            or listing.price <= :maximumPrice
                      )
                    order by listing.createdAt desc, listing.id desc
                    """,
            countQuery = """
                    select count(listing.id)
                    from Listing listing
                    join listing.category category
                    join category.parent parent
                    left join listing.brand brand
                    where listing.status = :status
                      and (
                            :categorySlug is null
                            or category.slug = :categorySlug
                            or parent.slug = :categorySlug
                      )
                      and (
                            :brandSlug is null
                            or brand.slug = :brandSlug
                      )
                      and (
                            :condition is null
                            or listing.condition = :condition
                      )
                      and (
                            :minimumPrice is null
                            or listing.price >= :minimumPrice
                      )
                      and (
                            :maximumPrice is null
                            or listing.price <= :maximumPrice
                      )
                    """
    )
    Page<ListingSummaryRow> searchActiveListingSummaries(
            @Param("status") ListingStatus status,
            @Param("categorySlug") String categorySlug,
            @Param("brandSlug") String brandSlug,
            @Param("condition") ListingCondition condition,
            @Param("minimumPrice") BigDecimal minimumPrice,
            @Param("maximumPrice") BigDecimal maximumPrice,
            Pageable pageable
    );

}
