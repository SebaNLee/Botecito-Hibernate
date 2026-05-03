package ar.edu.itba.paw.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Marketplace listing sort, aligned with the {@code sort} request parameter and UI values.
 */
@Getter
@RequiredArgsConstructor
public enum ItemSearchSort {
    NEWEST("newest"),
    OLDEST("oldest"),
    PRICE_ASC("priceAsc"),
    PRICE_DESC("priceDesc");

    private final String requestValue;

    public static ItemSearchSort fromRequestParam(final String sort) {
        if (sort == null || sort.isBlank()) {
            return NEWEST;
        }
        final String trimmed = sort.trim();
        for (final ItemSearchSort value : values()) {
            if (value.getRequestValue().equals(trimmed)) {
                return value;
            }
        }
        return NEWEST;
    }
}
