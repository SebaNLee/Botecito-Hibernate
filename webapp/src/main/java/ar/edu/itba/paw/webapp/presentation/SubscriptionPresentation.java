package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.webapp.form.ItemDetailViewForm;
import ar.edu.itba.paw.webapp.form.ProfileViewForm;
import ar.edu.itba.paw.webapp.form.SettingsViewForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class SubscriptionPresentation {

    public ModelAndView subscribeFromProfileResult(
            final ProfileViewForm view,
            final int profileUserId,
            final boolean success,
            final RedirectAttributes redirectAttributes) {
        if (!success) {
            ToastSupport.error(redirectAttributes, "subscription.self.error");
        } else {
            ToastSupport.success(redirectAttributes, "subscription.created");
        }
        addProfileViewParams(redirectAttributes, view);
        return new ModelAndView("redirect:/profiles/" + profileUserId);
    }

    public ModelAndView unsubscribeFromProfileResult(
            final ProfileViewForm view,
            final int profileUserId,
            final boolean success,
            final RedirectAttributes redirectAttributes) {
        if (!success) {
            ToastSupport.error(redirectAttributes, "subscription.self.error");
        } else {
            ToastSupport.info(redirectAttributes, "subscription.removed");
        }
        addProfileViewParams(redirectAttributes, view);
        return new ModelAndView("redirect:/profiles/" + profileUserId);
    }

    public ModelAndView subscribeFromItemDetailResult(
            final ItemDetailViewForm view, final boolean success, final RedirectAttributes redirectAttributes) {
        if (!success) {
            ToastSupport.error(redirectAttributes, "subscription.self.error");
        } else {
            ToastSupport.success(redirectAttributes, "subscription.created");
        }
        return itemDetailRedirect(view, redirectAttributes);
    }

    public ModelAndView unsubscribeFromItemDetailResult(
            final ItemDetailViewForm view, final boolean success, final RedirectAttributes redirectAttributes) {
        if (!success) {
            ToastSupport.error(redirectAttributes, "subscription.self.error");
        } else {
            ToastSupport.info(redirectAttributes, "subscription.removed");
        }
        return itemDetailRedirect(view, redirectAttributes);
    }

    public ModelAndView unsubscribeFromSettingsResult(
            final SettingsViewForm view, final boolean success, final RedirectAttributes redirectAttributes) {
        if (!success) {
            ToastSupport.error(redirectAttributes, "subscription.self.error");
        } else {
            ToastSupport.info(redirectAttributes, "subscription.removed");
        }
        addSettingsViewParams(redirectAttributes, view);
        return new ModelAndView("redirect:/settings");
    }

    private static ModelAndView itemDetailRedirect(
            final ItemDetailViewForm view, final RedirectAttributes redirectAttributes) {
        if (view == null || view.getItemId() == null) {
            return new ModelAndView("redirect:/");
        }
        addItemDetailViewParams(redirectAttributes, view);
        return new ModelAndView("redirect:/item/" + view.getItemId());
    }

    private static void addItemDetailViewParams(
            final RedirectAttributes redirectAttributes, final ItemDetailViewForm view) {
        if (view.getReviewPage() != null && view.getReviewPage() > 1) {
            redirectAttributes.addAttribute("reviewPage", view.getReviewPage());
        }
    }

    private static void addProfileViewParams(final RedirectAttributes redirectAttributes, final ProfileViewForm view) {
        if (view == null) {
            return;
        }
        final String tab = StringUtils.hasText(view.getTab()) ? view.getTab() : "listings";
        if (!"listings".equals(tab)) {
            redirectAttributes.addAttribute("tab", tab);
        }
        final int listingsPage = view.getListingsPage() == null ? 1 : view.getListingsPage();
        final int listingsPageSize = view.getListingsPageSize() == null ? 6 : view.getListingsPageSize();
        final int reviewsPage = view.getReviewsPage() == null ? 1 : view.getReviewsPage();
        final int reviewsPageSize = view.getReviewsPageSize() == null ? 5 : view.getReviewsPageSize();

        if ("reviews".equals(tab)) {
            if (reviewsPage > 1) {
                redirectAttributes.addAttribute("reviewsPage", reviewsPage);
            }
            if (reviewsPageSize != 5) {
                redirectAttributes.addAttribute("reviewsPageSize", reviewsPageSize);
            }
        } else {
            if (listingsPage > 1) {
                redirectAttributes.addAttribute("listingsPage", listingsPage);
            }
            if (listingsPageSize != 6) {
                redirectAttributes.addAttribute("listingsPageSize", listingsPageSize);
            }
        }
    }

    private static void addSettingsViewParams(
            final RedirectAttributes redirectAttributes, final SettingsViewForm view) {
        if (view == null) {
            return;
        }
        final int page = view.getSubscriptionsPage() == null ? 1 : view.getSubscriptionsPage();
        final int pageSize = view.getSubscriptionsPageSize() == null ? 6 : view.getSubscriptionsPageSize();
        if (page > 1) {
            redirectAttributes.addAttribute("subscriptionsPage", page);
        }
        if (pageSize != 6) {
            redirectAttributes.addAttribute("subscriptionsPageSize", pageSize);
        }
        if (Boolean.TRUE.equals(view.getEdit())) {
            redirectAttributes.addAttribute("edit", true);
        }
    }
}
