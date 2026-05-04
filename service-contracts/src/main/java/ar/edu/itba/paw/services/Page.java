package ar.edu.itba.paw.services;

import java.util.List;
import lombok.Getter;

@Getter
public class Page<T> {
    private final List<T> content;
    private final int page;
    private final int pageSize;
    private final int totalItems;
    private final int totalPages;

    public Page(final List<T> content, final int page, final int pageSize, final int totalItems) {
        this.content = List.copyOf(content);
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.totalPages = pageSize <= 0 ? 0 : (int) Math.ceil((double) totalItems / pageSize);
    }

    public boolean isHasPrevious() {
        return page > 1;
    }

    public boolean isHasNext() {
        return page < totalPages;
    }

    public int getPreviousPage() {
        return Math.max(1, page - 1);
    }

    public int getNextPage() {
        return totalPages == 0 ? 1 : Math.min(totalPages, page + 1);
    }

    public static <T> Page<T> slice(final List<T> items, final int page, final int pageSize) {
        final int totalItems = items == null ? 0 : items.size();
        final int ps = pageSize <= 0 ? 0 : pageSize;
        final int totalPages = ps <= 0 ? 0 : (int) Math.ceil((double) totalItems / ps);
        final int resolvedPage = totalPages == 0 ? 1 : Math.min(Math.max(1, page), totalPages);
        final int from = totalItems == 0 ? 0 : Math.min((resolvedPage - 1) * ps, totalItems);
        final int to = totalItems == 0 ? 0 : Math.min(from + ps, totalItems);
        return new Page<>(items == null ? List.of() : items.subList(from, to), resolvedPage, ps, totalItems);
    }
}
