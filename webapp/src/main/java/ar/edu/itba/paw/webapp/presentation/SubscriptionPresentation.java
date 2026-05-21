package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.services.SubscriptionService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class SubscriptionPresentation {

    private final SubscriptionService subscriptionService;

    public ModelAndView subscribe(
            final BotecitoUserDetails principal,
            final int subscribedToId,
            final String returnPath,
            final RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }
        if (!subscriptionService.subscribe(principal.getId(), subscribedToId)) {
            ToastSupport.error(redirectAttributes, "subscription.self.error");
            return redirect(returnPath);
        }
        ToastSupport.success(redirectAttributes, "subscription.created");
        return redirect(returnPath);
    }

    public ModelAndView unsubscribe(
            final BotecitoUserDetails principal,
            final int subscribedToId,
            final String returnPath,
            final RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return new ModelAndView("redirect:/login");
        }
        if (!subscriptionService.unsubscribe(principal.getId(), subscribedToId)) {
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
            return "/profile";
        }
        final String trimmed = returnPath.trim();
        if (!trimmed.startsWith("/") || trimmed.startsWith("//") || trimmed.contains("://")) {
            return "/profile";
        }
        if (trimmed.contains("\r") || trimmed.contains("\n")) {
            return "/profile";
        }
        return trimmed;
    }
}
