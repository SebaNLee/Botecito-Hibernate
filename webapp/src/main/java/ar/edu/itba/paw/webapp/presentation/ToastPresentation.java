package ar.edu.itba.paw.webapp.presentation;

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

@Component
@RequiredArgsConstructor
public class ToastPresentation {

    private final MessageSource messageSource;

    /**
     * Builds error toasts from a {@link BindingResult}, using {@code messagePrefix} for i18n keys
     * (e.g. {@code marketplaceSearch}, {@code detail}): {@code {prefix}.validation.toastFormat},
     * {@code {prefix}.field.*}, {@code {prefix}.validation.bannerTitle}.
     */
    public List<Map<String, String>> validationToasts(final BindingResult errors, final String messagePrefix) {
        final Locale locale = LocaleContextHolder.getLocale();
        final List<Map<String, String>> toasts = new ArrayList<>();
        for (final ObjectError error : errors.getAllErrors()) {
            final Map<String, String> entry = new HashMap<>();
            entry.put("type", "error");
            entry.put("text", validationToastText(error, messagePrefix, locale));
            toasts.add(entry);
        }
        if (toasts.isEmpty()) {
            final Map<String, String> entry = new HashMap<>();
            entry.put("type", "error");
            entry.put("code", messagePrefix + ".validation.bannerTitle");
            toasts.add(entry);
        }
        return toasts;
    }

    /** Single toast resolved from a message {@code code} (see {@code paw:toastNotifier}). */
    public List<Map<String, String>> codeToasts(final String type, final String messageCode) {
        return List.of(Map.of("type", type, "code", messageCode));
    }

    public List<Map<String, String>> errorCodeToasts(final String messageCode) {
        return codeToasts("error", messageCode);
    }

    private String validationToastText(final ObjectError error, final String messagePrefix, final Locale locale) {
        final String fieldLabel = fieldLabel(error, messagePrefix, locale);
        final String reason = resolveValidationReason(error, messagePrefix, locale);
        final String formatKey = messagePrefix + ".validation.toastFormat";
        try {
            return messageSource.getMessage(formatKey, new Object[] {fieldLabel, reason}, locale);
        } catch (final NoSuchMessageException ignored) {
            return fieldLabel + ": " + reason;
        }
    }

    private String fieldLabel(final ObjectError error, final String messagePrefix, final Locale locale) {
        String field = null;
        if (error instanceof FieldError fe) {
            field = fe.getField();
        }
        final String formFieldKey = messagePrefix + ".field.form";
        if (field == null) {
            try {
                return messageSource.getMessage(formFieldKey, null, locale);
            } catch (final NoSuchMessageException e) {
                return "Form";
            }
        }
        if ("pageSizeValid".equals(field)) {
            field = "pageSize";
        }
        try {
            return messageSource.getMessage(messagePrefix + ".field." + field, null, locale);
        } catch (final NoSuchMessageException e) {
            return field;
        }
    }

    private String resolveValidationReason(final ObjectError error, final String messagePrefix, final Locale locale) {
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
            final String bannerKey = messagePrefix + ".validation.bannerTitle";
            try {
                return messageSource.getMessage(bannerKey, null, locale);
            } catch (final NoSuchMessageException e) {
                return "Invalid value";
            }
        }
    }
}
