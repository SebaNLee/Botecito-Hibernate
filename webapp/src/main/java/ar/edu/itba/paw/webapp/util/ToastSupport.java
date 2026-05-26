package ar.edu.itba.paw.webapp.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public final class ToastSupport {

    public static final String FLASH_ATTRIBUTE = "toasts";

    private ToastSupport() {}

    public static void push(final RedirectAttributes redirectAttributes, final String type, final String code) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> toasts = (List<Map<String, String>>)
                redirectAttributes.getFlashAttributes().get(FLASH_ATTRIBUTE);
        if (toasts == null) {
            toasts = new ArrayList<>();
            redirectAttributes.addFlashAttribute(FLASH_ATTRIBUTE, toasts);
        }
        final Map<String, String> entry = new HashMap<>();
        entry.put("type", type);
        entry.put("code", code);
        toasts.add(entry);
    }

    public static void success(final RedirectAttributes redirectAttributes, final String code) {
        push(redirectAttributes, "success", code);
    }

    public static void error(final RedirectAttributes redirectAttributes, final String code) {
        push(redirectAttributes, "error", code);
    }

    public static void warning(final RedirectAttributes redirectAttributes, final String code) {
        push(redirectAttributes, "warning", code);
    }

    public static void info(final RedirectAttributes redirectAttributes, final String code) {
        push(redirectAttributes, "info", code);
    }
}
