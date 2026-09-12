package com.campushub.marketplace.domain;

import com.campushub.shared.base.BaseEntity;

import java.math.BigDecimal;

public class ListingAttributeValue extends BaseEntity {

    private Listing listing;

    private AttributeDefinition attributeDefinition;

    private String textValue;

    private BigDecimal numberValue;

    private Boolean booleanValue;

}
