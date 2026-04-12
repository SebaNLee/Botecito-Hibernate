package ar.edu.itba.paw.services;

import java.util.List;

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

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
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
}
