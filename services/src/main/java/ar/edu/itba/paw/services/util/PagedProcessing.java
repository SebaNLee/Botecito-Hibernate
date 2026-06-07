package ar.edu.itba.paw.services.util;

import ar.edu.itba.paw.models.dto.PageModel;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class PagedProcessing {

    /**
     * Uses {@code searcher} to obtain pages of type {@code PageModel<T>}, and runs {@code action} on each of the elements inside.
     * The searcher function must be of the form: {@code PageModel<T> searcher(Integer page, Integer pageSize)}
     */
    public static <T> void batchAction(
            final int batchSize,
            final long totalCount,
            final BiFunction<Integer, Integer, PageModel<T>> searcher,
            final Consumer<T> action) {
        int page = 1;
        final long totalBatches = (long) Math.ceil((double) totalCount / batchSize);
        List<T> batch = null;

        while (page <= totalBatches) {
            var search = searcher.apply(page, batchSize);
            batch = search.getContent();
            for (T element : batch) {
                action.accept(element);
            }
            page++;
        }
    }
}
