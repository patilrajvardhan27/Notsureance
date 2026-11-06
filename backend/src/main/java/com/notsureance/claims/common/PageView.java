package com.notsureance.claims.common;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PageView<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <E, T> PageView<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageView<>(page.map(mapper).getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
