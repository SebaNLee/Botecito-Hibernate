package ar.edu.itba.paw.models.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class PageModel<T> {

    private final List<T> content;
    private final int page;
    private final int pageSize;
    private final long totalItems;
    private final int totalPages;

    public PageModel(final List<T> content, final int page, final int pageSize, final long totalItems) {
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
}
