package com.ssrpro.library.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class PageResult<T> {

    public static final int DEFAULT_SIZE = 10;

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalItems;
    private final int totalPages;
    /** 정확한 전체 건수를 모를 때 다음 페이지 존재 여부 */
    private final boolean nextPage;

    public static <T> PageResult<T> empty(int page, int size) {
        int safePage = Math.max(page, 1);
        return PageResult.<T>builder()
                .content(Collections.emptyList())
                .page(safePage)
                .size(size)
                .totalItems(0)
                .totalPages(0)
                .nextPage(false)
                .build();
    }

    /**
     * 전체 건수를 미리 세지 않는 페이지(검색 API 필터 등).
     * {@code hasNext=true}이면 {@code totalPages}는 {@code page + 1} 이상으로 잡힌다.
     */
    public static <T> PageResult<T> ofPage(List<T> content, int page, int size, boolean hasNext) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        List<T> items = content == null ? Collections.emptyList() : content;

        if (items.isEmpty() && !hasNext) {
            return empty(safePage, safeSize);
        }

        int totalPages = hasNext ? safePage + 1 : safePage;
        long totalItems = hasNext
                ? (long) safePage * safeSize + 1
                : (long) (safePage - 1) * safeSize + items.size();

        return PageResult.<T>builder()
                .content(items)
                .page(safePage)
                .size(safeSize)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .nextPage(hasNext)
                .build();
    }

    public static <T> PageResult<T> of(List<T> content, int page, int size, long totalItems) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int totalPages = totalItems <= 0 ? 0 : (int) Math.ceil((double) totalItems / safeSize);
        if (totalPages > 0 && safePage > totalPages) {
            safePage = totalPages;
        }
        return PageResult.<T>builder()
                .content(content == null ? Collections.emptyList() : content)
                .page(safePage)
                .size(safeSize)
                .totalItems(Math.max(totalItems, 0))
                .totalPages(totalPages)
                .nextPage(safePage < totalPages)
                .build();
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return nextPage || page < totalPages;
    }

    public int itemOffset() {
        return (page - 1) * size;
    }
}
