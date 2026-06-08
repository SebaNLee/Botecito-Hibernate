package ar.edu.itba.paw.webapp.presentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

public final class ToastPresentation {

    private ToastPresentation() {}

    public static List<Map<String, String>> validationToasts(
            final BindingResult errors, final String messagePrefix, final MessageSource messageSource) {
        final Locale locale = LocaleContextHolder.getLocale();
        final List<Map<String, String>> toasts = new ArrayList<>();
        for (final ObjectError error : errors.getAllErrors()) {
            final Map<String, String> entry = new HashMap<>();
            entry.put("type", "error");
            entry.put("text", validationToastText(error, messagePrefix, locale, messageSource));
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

    public static List<Map<String, String>> codeToasts(final String type, final String messageCode) {
        return List.of(Map.of("type", type, "code", messageCode));
    }

    public static List<Map<String, String>> errorCodeToasts(final String messageCode) {
        return codeToasts("error", messageCode);
    }

    private static String validationToastText(
            final ObjectError error,
            final String messagePrefix,
            final Locale locale,
            final MessageSource messageSource) {
        final String fieldLabel = fieldLabel(error, messagePrefix, locale, messageSource);
        final String reason = resolveValidationReason(error, messagePrefix, locale, messageSource);
        final String formatKey = messagePrefix + ".validation.toastFormat";
        try {
            return messageSource.getMessage(formatKey, new Object[] {fieldLabel, reason}, locale);
        } catch (final NoSuchMessageException ignored) {
            return fieldLabel + ": " + reason;
        }
    }

    private static String fieldLabel(
            final ObjectError error,
            final String messagePrefix,
            final Locale locale,
            final MessageSource messageSource) {
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

    private static String resolveValidationReason(
            final ObjectError error,
            final String messagePrefix,
            final Locale locale,
            final MessageSource messageSource) {
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
