package com.edusistem.core.shared.domain.vo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record PageResult<T>(List<T> items, int page, int size, long totalElements, int totalPages) {

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(items.stream().map(mapper).toList(), page, size, totalElements, totalPages);
    }

    /** Recorre todas las páginas (de {@link PageQuery#MAX_SIZE} elementos) y devuelve los elementos en una lista mutable. */
    public static <T> List<T> collectAll(Function<PageQuery, PageResult<T>> fetch) {
        List<T> list = new ArrayList<>();
        PageResult<T> page;
        int n = 0;
        do {
            page = fetch.apply(new PageQuery(n++, PageQuery.MAX_SIZE));
            list.addAll(page.items());
        } while (n < page.totalPages());
        return list;
    }
}
