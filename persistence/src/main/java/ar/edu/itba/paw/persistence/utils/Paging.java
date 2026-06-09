package ar.edu.itba.paw.persistence.utils;

import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Query;

public final class Paging {

    private Paging() {}

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
