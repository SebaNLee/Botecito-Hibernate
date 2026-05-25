package ar.edu.itba.paw.persistence.utils;

import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Query;

public final class Paging {

    public static final int DEFAULT_PAGE = 1;

    private Paging() {}

    public static int resolvePage(final Integer page) {
        return resolvePage(page, DEFAULT_PAGE);
    }

    public static int resolvePage(final Integer page, final int defaultPage) {
        if (page == null || page < 1) {
            return defaultPage;
        }
        return page;
    }

    public static int resolvePage(final int page, final int defaultPage) {
        if (page < 1) {
            return defaultPage;
        }
        return page;
    }

    public static int resolvePageSize(final Integer pageSize, final int defaultPageSize, final int... allowed) {
        if (pageSize == null) {
            return defaultPageSize;
        }
        for (final int allowedSize : allowed) {
            if (pageSize == allowedSize) {
                return pageSize;
            }
        }
        return defaultPageSize;
    }

    public static int firstResult(final int page, final int pageSize) {
        return (page - 1) * pageSize;
    }

    public static void apply(final Query query, final int page, final int pageSize) {
        query.setFirstResult(firstResult(page, pageSize));
        query.setMaxResults(pageSize);
    }

    public static List<Integer> toIntegerIds(final List<?> rows) {
        return rows.stream().map(id -> ((Number) id).intValue()).collect(Collectors.toList());
    }
}
