package com.campushub.marketplace.repository;

import com.campushub.marketplace.domain.Listing;
import com.campushub.marketplace.domain.ListingCondition;
import com.campushub.marketplace.domain.ListingStatus;
import com.campushub.marketplace.repository.projection.ListingSummaryRow;
import com.campushub.marketplace.repository.projection.SellerListingSummaryRow;
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
                        :keywordPattern is null
                        or lower(listing.title)
                            like :keywordPattern escape '!'
                        or lower(listing.description)
                            like :keywordPattern escape '!'
                        or lower(category.displayName)
                            like :keywordPattern escape '!'
                        or category.slug
                            like :keywordPattern escape '!'
                        or lower(parent.displayName)
                            like :keywordPattern escape '!'
                        or parent.slug
                            like :keywordPattern escape '!'
                        or lower(brand.displayName)
                            like :keywordPattern escape '!'
                        or brand.slug
                            like :keywordPattern escape '!'
                  )
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
                order by
                    case
                        when :keywordPattern is not null
                         and lower(listing.title)
                             like :keywordPattern escape '!'
                        then 0
                        else 1
                    end,
                    listing.createdAt desc,
                    listing.id desc
                """,
            countQuery = """
                select count(listing.id)
                from Listing listing
                join listing.category category
                join category.parent parent
                left join listing.brand brand
                where listing.status = :status
                  and (
                        :keywordPattern is null
                        or lower(listing.title)
                            like :keywordPattern escape '!'
                        or lower(listing.description)
                            like :keywordPattern escape '!'
                        or lower(category.displayName)
                            like :keywordPattern escape '!'
                        or category.slug
                            like :keywordPattern escape '!'
                        or lower(parent.displayName)
                            like :keywordPattern escape '!'
                        or parent.slug
                            like :keywordPattern escape '!'
                        or lower(brand.displayName)
                            like :keywordPattern escape '!'
                        or brand.slug
                            like :keywordPattern escape '!'
                  )
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
            @Param("keywordPattern") String keywordPattern,
            @Param("categorySlug") String categorySlug,
            @Param("brandSlug") String brandSlug,
            @Param("condition") ListingCondition condition,
            @Param("minimumPrice") BigDecimal minimumPrice,
            @Param("maximumPrice") BigDecimal maximumPrice,
            Pageable pageable
    );

    @Query(
            value = """
            select new com.campushub.marketplace.repository.projection.SellerListingSummaryRow(
                listing.id,
                listing.title,
                listing.price,
                listing.currency,
                listing.primaryImageUrl,
                listing.status,
                listing.version,
                listing.createdAt,
                listing.updatedAt
            )
            from Listing listing
            where listing.sellerAccountId = :sellerAccountId
              and (
                    :status is null
                    or listing.status = :status
              )
            order by
                listing.createdAt desc,
                listing.id desc
            """,
            countQuery = """
            select count(listing.id)
            from Listing listing
            where listing.sellerAccountId = :sellerAccountId
              and (
                    :status is null
                    or listing.status = :status
              )
            """
    )
    Page<SellerListingSummaryRow> findSellerListingSummaries(
            @Param("sellerAccountId") UUID sellerAccountId,
            @Param("status") ListingStatus status,
            Pageable pageable
    );

    @Override
    @EntityGraph(
            attributePaths = {
                    "category",
                    "category.parent",
                    "brand",
                    "additionalImageUrls"
            }
    )
    Optional<Listing> findById(UUID id);
}
