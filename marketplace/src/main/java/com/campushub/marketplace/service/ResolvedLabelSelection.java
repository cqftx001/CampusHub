package com.campushub.marketplace.service;

import com.campushub.marketplace.domain.Label;

import java.util.LinkedHashSet;
import java.util.Set;

public record ResolvedLabelSelection(
        Label category,
        Set<Label> childLabels
) {

    public ResolvedLabelSelection {
        childLabels = Set.copyOf(childLabels);
    }
}
