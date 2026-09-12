package com.campushub.marketplace.service;

import com.campushub.marketplace.domain.Label;
import com.campushub.marketplace.error.MarketplaceErrorCode;
import com.campushub.marketplace.error.MarketplaceException;
import com.campushub.marketplace.repository.LabelRepository;
import com.campushub.marketplace.utils.LabelSlugNormalizer;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class LabelSelectionResolver {

    private static final int MAXIMUM_CHILD_LABELS = 3;

    private final LabelRepository labelRepository;

    public LabelSelectionResolver(
            LabelRepository labelRepository
    ) {
        this.labelRepository = labelRepository;
    }

    public ResolvedLabelSelection resolve(
            String requestedCategory,
            Collection<String> requestedChildLabels
    ) {
        String categorySlug = LabelSlugNormalizer.normalize(
                requestedCategory
        );

        List<String> childSlugs = normalizeChildSlugs(
                requestedChildLabels
        );

        validateChildLabelCount(childSlugs);

        LinkedHashSet<String> uniqueChildSlugs =
                new LinkedHashSet<>(childSlugs);

        if (uniqueChildSlugs.size() != childSlugs.size()) {
            throw invalidSelection();
        }

        LinkedHashSet<String> requestedSlugs =
                new LinkedHashSet<>();

        requestedSlugs.add(categorySlug);
        requestedSlugs.addAll(uniqueChildSlugs);

        Map<String, Label> resolvedLabels =
                labelRepository
                        .findAllBySlugInAndActiveTrue(
                                requestedSlugs
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                Label::getSlug,
                                Function.identity(),
                                (first, ignored) -> first,
                                LinkedHashMap::new
                        ));

        if (resolvedLabels.size() != requestedSlugs.size()) {
            throw invalidSelection();
        }

        Label category = resolvedLabels.get(categorySlug);

        if (category == null || !category.isCategory()) {
            throw invalidSelection();
        }

        LinkedHashSet<Label> childLabels =
                uniqueChildSlugs.stream()
                        .map(resolvedLabels::get)
                        .collect(Collectors.toCollection(
                                LinkedHashSet::new
                        ));

        boolean invalidChild = childLabels.stream()
                .anyMatch(label -> !label.isDirectChildOf(category));

        if (invalidChild) {
            throw invalidSelection();
        }

        return new ResolvedLabelSelection(category, childLabels);
    }

    private List<String> normalizeChildSlugs(
            Collection<String> requestedChildLabels
    ) {
        if (requestedChildLabels == null) {
            throw invalidSelection();
        }

        return requestedChildLabels.stream()
                .map(LabelSlugNormalizer::normalize)
                .toList();
    }

    private void validateChildLabelCount(
            List<String> childSlugs
    ) {
        if (childSlugs.isEmpty()
                || childSlugs.size()
                > MAXIMUM_CHILD_LABELS) {
            throw invalidSelection();
        }
    }

    private MarketplaceException invalidSelection() {
        return new MarketplaceException(MarketplaceErrorCode.INVALID_LABEL_SELECTION);
    }
}