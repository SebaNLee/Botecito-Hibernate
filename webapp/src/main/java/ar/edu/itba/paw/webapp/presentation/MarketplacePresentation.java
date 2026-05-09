package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.ItemModel;
import ar.edu.itba.paw.models.nuevo.MarketplaceSearchModel;
import ar.edu.itba.paw.models.nuevo.MarketplaceSearchResult;
import ar.edu.itba.paw.services.Page;
import ar.edu.itba.paw.services.nuevo.MarketplaceInterface;
import ar.edu.itba.paw.webapp.form.nuevo.MarketplaceSearchForm;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class MarketplacePresentation {

    private final MarketplaceInterface marketplaceInterface;
    private final MessageSource messageSource;

    public ModelAndView marketplaceGet(final MarketplaceSearchForm search) {
        final MarketplaceSearchModel model = toModel(search);
        final MarketplaceSearchResult result = marketplaceInterface.searchMarketplace(model);
        final List<ItemModel> items = result.getItems();
        final ModelAndView mav = new ModelAndView("marketplace", "marketplaceSearch", search);
        mav.addObject("items", items);
        addListingModelObjects(mav, search, items, result.getTotalCount());
        return mav;
    }

    public ModelAndView marketplaceErrors(final MarketplaceSearchForm search, final BindingResult errors) {
        // Do not run search: invalid params often leave fields at @ModelAttribute
        // defaults (e.g. page=1),
        // which would still return results while BindingResult reports errors.
        final List<ItemModel> items = List.of();
        final ModelAndView mav = new ModelAndView("marketplace", "marketplaceSearch", search);
        mav.addAllObjects(errors.getModel());
        mav.addObject("items", items);
        addListingModelObjects(mav, search, items, 0L);
        mav.addObject("toasts", validationToasts(errors));
        return mav;
    }

    private void addListingModelObjects(
            final ModelAndView mav, final MarketplaceSearchForm search, final List<ItemModel> items, final long total) {
        final int page = search.getPage() == null ? 1 : search.getPage();
        final int pageSize = search.getPageSize() == null ? 12 : search.getPageSize();
        final int totalItems = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        mav.addObject("itemPage", new Page<>(items, page, pageSize, totalItems));
        mav.addObject("itemsCount", totalItems);
        mav.addObject("sort", sortRequestValue(search.getSortBy()));
    }

    private static Double parseminAvgRating(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(raw.trim());
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }

    private static String sortRequestValue(final String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "newest";
        }
        return switch (sortBy) {
            case "price_asc" -> "priceAsc";
            case "price_desc" -> "priceDesc";
            default -> sortBy;
        };
    }

    public MarketplaceSearchModel toModel(final MarketplaceSearchForm form) {
        final MarketplaceSearchModel model = new MarketplaceSearchModel();
        model.setSearchQuery(form.getSearchQuery());
        model.setDate(PresentationUtils.parseDate(form.getDate()));
        model.setStartTime(PresentationUtils.parseTime(form.getStartTime()));
        model.setEndTime(PresentationUtils.parseTime(form.getEndTime()));
        model.setCapacity(form.getCapacity());
        model.setWeight(form.getWeight());
        model.setDifficulty(form.getDifficultyLevel());
        model.setMinAvgRating(parseminAvgRating(form.getMinAvgRating()));
        model.setLocation(form.getLocation());
        model.setItemType(form.getItemType());
        model.setPage(form.getPage());
        model.setPageSize(form.getPageSize());
        model.setSortBy(form.getSortBy());
        return model;
    }

    private List<Map<String, String>> validationToasts(final BindingResult errors) {
        final Locale locale = LocaleContextHolder.getLocale();
        final List<Map<String, String>> toasts = new ArrayList<>();
        for (final ObjectError error : errors.getAllErrors()) {
            final Map<String, String> entry = new HashMap<>();
            entry.put("type", "error");
            entry.put("text", validationToastText(error, locale));
            toasts.add(entry);
        }
        if (toasts.isEmpty()) {
            final Map<String, String> entry = new HashMap<>();
            entry.put("type", "error");
            entry.put("code", "marketplaceSearch.validation.bannerTitle");
            toasts.add(entry);
        }
        return toasts;
    }

    private String validationToastText(final ObjectError error, final Locale locale) {
        final String fieldLabel = marketplaceSearchFieldLabel(error, locale);
        final String reason = resolveValidationReason(error, locale);
        try {
            return messageSource.getMessage(
                    "marketplaceSearch.validation.toastFormat", new Object[] {fieldLabel, reason}, locale);
        } catch (final NoSuchMessageException ignored) {
            return fieldLabel + ": " + reason;
        }
    }

    private String marketplaceSearchFieldLabel(final ObjectError error, final Locale locale) {
        String field = null;
        if (error instanceof FieldError fe) {
            field = fe.getField();
        }
        if (field == null) {
            try {
                return messageSource.getMessage("marketplaceSearch.field.form", null, locale);
            } catch (final NoSuchMessageException e) {
                return "Filters";
            }
        }
        if ("pageSizeValid".equals(field)) {
            field = "pageSize";
        }
        try {
            return messageSource.getMessage("marketplaceSearch.field." + field, null, locale);
        } catch (final NoSuchMessageException e) {
            return field;
        }
    }

    private String resolveValidationReason(final ObjectError error, final Locale locale) {
        try {
            return messageSource.getMessage(error, locale);
        } catch (final NoSuchMessageException ignored) {
            final String fallback = error.getDefaultMessage();
            if (fallback != null && !fallback.isBlank()) {
                return fallback;
            }
            final String[] codes = error.getCodes();
            if (codes != null && codes.length > 0) {
                final String firstCode = codes[0];
                if (firstCode != null) {
                    try {
                        return messageSource.getMessage(firstCode, null, locale);
                    } catch (final NoSuchMessageException e) {
                        return firstCode;
                    }
                }
            }
            try {
                return messageSource.getMessage("marketplaceSearch.validation.bannerTitle", null, locale);
            } catch (final NoSuchMessageException e) {
                return "Invalid value";
            }
        }
    }
}
