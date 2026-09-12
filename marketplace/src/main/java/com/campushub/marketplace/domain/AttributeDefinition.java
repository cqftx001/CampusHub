package com.campushub.marketplace.domain;

import com.campushub.shared.base.BaseEntity;

import java.math.BigDecimal;
import java.util.Map;

public class AttributeDefinition extends BaseEntity {

    private Category category;

    private String slug;
    private String displayName;
    private AttributeValueType attributeValueType;

    private boolean required;
    private String unit;

    private BigDecimal minimumValue;
    private BigDecimal maximumValue;
    private Integer maximumTextLength;

    private Map<String, String> allowedOptions;

    private boolean active;
    private int displayOrder;

}
