package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.webapp.util.ToastSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class SubscriptionPresentation {

    public ModelAndView subscribeResult(
            final boolean success, final String returnPath, final RedirectAttributes redirectAttributes) {
        if (!success) {
            ToastSupport.error(redirectAttributes, "subscription.self.error");
            return redirect(returnPath);
        }
        ToastSupport.success(redirectAttributes, "subscription.created");
        return redirect(returnPath);
    }

    public ModelAndView unsubscribeResult(
            final boolean success, final String returnPath, final RedirectAttributes redirectAttributes) {
        if (!success) {
            ToastSupport.error(redirectAttributes, "subscription.self.error");
            return redirect(returnPath);
        }
        ToastSupport.success(redirectAttributes, "subscription.removed");
        return redirect(returnPath);
    }

    private static ModelAndView redirect(final String returnPath) {
        return new ModelAndView("redirect:" + safeRelativePath(returnPath));
    }

    private static String safeRelativePath(final String returnPath) {
        if (returnPath == null || returnPath.isBlank()) {
            return "/settings";
        }
        final String trimmed = returnPath.trim();
        if (!trimmed.startsWith("/") || trimmed.startsWith("//") || trimmed.contains("://")) {
            return "/settings";
        }
        if (trimmed.contains("\r") || trimmed.contains("\n")) {
            return "/settings";
        }
        return trimmed;
    }
}
