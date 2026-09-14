package com.campushub.marketplace.vo;

import java.util.List;

public record CategoryGroupView(
        String slug,
        String displayName,
        List<CategoryOptionView> children
) {

    public CategoryGroupView {
        children = List.copyOf(children);
    }
}
