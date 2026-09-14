CREATE SCHEMA IF NOT EXISTS marketplace;

CREATE TABLE marketplace.categories
(
    id            UUID PRIMARY KEY,

    slug          VARCHAR(64)              NOT NULL,
    display_name  VARCHAR(100)             NOT NULL,

    parent_id     UUID,

    active        BOOLEAN                  NOT NULL,
    display_order INTEGER                  NOT NULL,

    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    version       BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT uk_marketplace_categories_slug
        UNIQUE (slug),

    CONSTRAINT fk_marketplace_categories_parent
        FOREIGN KEY (parent_id)
            REFERENCES marketplace.categories (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_marketplace_categories_parent
        CHECK (
            parent_id IS NULL
                OR parent_id <> id
            ),

    CONSTRAINT ck_marketplace_categories_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX idx_marketplace_categories_parent
    ON marketplace.categories (parent_id);


CREATE TABLE marketplace.brands
(
    id            UUID PRIMARY KEY,

    slug          VARCHAR(64)              NOT NULL,
    display_name  VARCHAR(100)             NOT NULL,

    active        BOOLEAN                  NOT NULL,
    display_order INTEGER                  NOT NULL,

    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    version       BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT uk_marketplace_brands_slug
        UNIQUE (slug),

    CONSTRAINT ck_marketplace_brands_display_order
        CHECK (display_order >= 0)
);


CREATE TABLE marketplace.listings
(
    id                 UUID PRIMARY KEY,

    seller_account_id  UUID                     NOT NULL,

    category_id        UUID                     NOT NULL,
    brand_id           UUID,

    title              VARCHAR(160)             NOT NULL,
    description        VARCHAR(5000)            NOT NULL,

    manufacture_year   INTEGER,

    condition          VARCHAR(32)              NOT NULL,

    price              NUMERIC(12, 2)           NOT NULL,
    currency           VARCHAR(3)               NOT NULL,

    location           VARCHAR(160)             NOT NULL,
    delivery_method    VARCHAR(32)              NOT NULL,

    primary_image_url  VARCHAR(2048)            NOT NULL,

    status             VARCHAR(32)              NOT NULL,

    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    version            BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT fk_marketplace_listings_category
        FOREIGN KEY (category_id)
            REFERENCES marketplace.categories (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_marketplace_listings_brand
        FOREIGN KEY (brand_id)
            REFERENCES marketplace.brands (id)
            ON DELETE RESTRICT,

    CONSTRAINT ck_marketplace_listings_manufacture_year
        CHECK (
            manufacture_year IS NULL
                OR manufacture_year BETWEEN 1800 AND 2100
            ),

    CONSTRAINT ck_marketplace_listings_condition
        CHECK (
            condition IN (
                          'NEW',
                          'OPEN_BOX',
                          'LIKE_NEW',
                          'GOOD',
                          'FAIR',
                          'FOR_PARTS_OR_NOT_WORKING'
                )
            ),

    CONSTRAINT ck_marketplace_listings_price
        CHECK (price > 0),

    CONSTRAINT ck_marketplace_listings_currency
        CHECK (currency = 'USD'),

    CONSTRAINT ck_marketplace_listings_delivery_method
        CHECK (
            delivery_method IN (
                                'LOCAL_PICKUP',
                                'SHIPPING',
                                'PICKUP_OR_SHIPPING'
                )
            ),

    CONSTRAINT ck_marketplace_listings_status
        CHECK (
            status IN (
                       'ACTIVE',
                       'SOLD',
                       'WITHDRAWN'
                )
            )
);

CREATE INDEX idx_marketplace_listings_status_created
    ON marketplace.listings (
                             status,
                             created_at DESC,
                             id DESC
        );

CREATE INDEX idx_marketplace_listings_status_category_created
    ON marketplace.listings (
                             status,
                             category_id,
                             created_at DESC,
                             id DESC
        );

CREATE INDEX idx_marketplace_listings_status_brand_created
    ON marketplace.listings (
                             status,
                             brand_id,
                             created_at DESC,
                             id DESC
        );


CREATE TABLE marketplace.listing_additional_images
(
    listing_id   UUID          NOT NULL,
    display_order INTEGER       NOT NULL,
    image_url    VARCHAR(2048) NOT NULL,

    CONSTRAINT pk_marketplace_listing_additional_images
        PRIMARY KEY (
                     listing_id,
                     display_order
            ),

    CONSTRAINT fk_marketplace_additional_images_listing
        FOREIGN KEY (listing_id)
            REFERENCES marketplace.listings (id)
            ON DELETE CASCADE,

    CONSTRAINT uk_marketplace_listing_additional_image_url
        UNIQUE (
                listing_id,
                image_url
            ),

    CONSTRAINT ck_marketplace_listing_image_display_order
        CHECK (
            display_order BETWEEN 0 AND 6
            )
);