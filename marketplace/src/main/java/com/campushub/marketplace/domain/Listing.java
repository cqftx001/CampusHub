package com.campushub.marketplace.domain;

import com.campushub.shared.base.BaseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Listing extends BaseEntity {

    private Category category;

    private Brand brand;

    private Set<Label> labels;

    private List<ListingAttributeValue> attributeValues = new ArrayList<>();
    
}

