package com.campushub.infrastructure.Utils;

import com.campushub.shared.enums.CommonErrorCode;
import com.campushub.shared.exception.BadRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class PageableUtils {

    private PageableUtils(){}

    public static Pageable sanitize(Pageable pageable, Set<String> allowedSorts){
        if(pageable.getSort().isUnsorted()){
            return pageable;
        }

        Set<String> invalidFields = StreamSupport.stream(pageable.getSort().spliterator(), false)
                .map(Sort.Order::getProperty)
                .filter(field -> !allowedSorts.contains(field))
                .collect(Collectors.toSet());

        if(!invalidFields.isEmpty()) {
            throw new BadRequestException(CommonErrorCode.BAD_REQUEST,
                    "Invalid sort field(s): " + invalidFields
                            + ". Allowed fields are: " + allowedSorts
            );
        }
        return pageable;
    }
}
