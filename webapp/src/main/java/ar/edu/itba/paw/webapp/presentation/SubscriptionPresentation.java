package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.webapp.form.ItemDetailViewForm;
import ar.edu.itba.paw.webapp.form.ProfileListingsViewForm;
import ar.edu.itba.paw.webapp.form.ProfileReviewsViewForm;
import ar.edu.itba.paw.webapp.form.SettingsViewForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class SubscriptionPresentation {

    public ModelAndView subscribeFromProfileListingsResult(
            final ProfileListingsViewForm view,
            final int profileUserId,
            final boolean success,
            final RedirectAttributes redirectAttributes) {
        if (!success) {
            ToastSupport.error(redirectAttributes, "subscription.self.error");
        } else {
            ToastSupport.success(redirectAttributes, "subscription.created");
        }
        addProfileListingsViewParams(redirectAttributes, view);
        return new ModelAndView("redirect:/profiles/" + profileUserId + "/listings");
    }

    public ModelAndView unsubscribeFromProfileListingsResult(
            final ProfileListingsViewForm view,
            final int profileUserId,
            final boolean success,
            final RedirectAttributes redirectAttributes) {
        if (!success) {
            ToastSupport.error(redirectAttributes, "subscription.self.error");
        } else {
            ToastSupport.info(redirectAttributes, "subscription.removed");
        }
        addProfileListingsViewParams(redirectAttributes, view);
        return new ModelAndView("redirect:/profiles/" + profileUserId + "/listings");
    }

    public ModelAndView subscribeFromProfileReviewsResult(
            final ProfileReviewsViewForm view,
            final int profileUserId,
            final boolean success,
            final RedirectAttributes redirectAttributes) {
        if (!success) {
            ToastSupport.error(redirectAttributes, "subscription.self.error");
        } else {
            ToastSupport.success(redirectAttributes, "subscription.created");
        }
        addProfileReviewsViewParams(redirectAttributes, view);
        return new ModelAndView("redirect:/profiles/" + profileUserId + "/reviews");
    }

    public ModelAndView unsubscribeFromProfileReviewsResult(
            final ProfileReviewsViewForm view,
            final int profileUserId,
            final boolean success,
            final RedirectAttributes redirectAttributes) {
        if (!success) {
            ToastSupport.error(redirectAttributes, "subscription.self.error");
        } else {
            ToastSupport.info(redirectAttributes, "subscription.removed");
        }
        addProfileReviewsViewParams(redirectAttributes, view);
        return new ModelAndView("redirect:/profiles/" + profileUserId + "/reviews");
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
        return new ModelAndView("redirect:/settings#settings-subscriptions");
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
        if (view.getPage() != null && view.getPage() > 1) {
            redirectAttributes.addAttribute("page", view.getPage());
        }
    }

    private static void addProfileListingsViewParams(
            final RedirectAttributes redirectAttributes, final ProfileListingsViewForm view) {
        if (view.getPage() != null && view.getPage() > 1) {
            redirectAttributes.addAttribute("page", view.getPage());
        }
        if (!"newest".equals(view.getSortBy())) {
            redirectAttributes.addAttribute("sortBy", view.getSortBy());
        }
        if (view.getPageSize() != 12) {
            redirectAttributes.addAttribute("pageSize", view.getPageSize());
        }
    }

    private static void addProfileReviewsViewParams(
            final RedirectAttributes redirectAttributes, final ProfileReviewsViewForm view) {
        if (view.getPage() != null && view.getPage() > 1) {
            redirectAttributes.addAttribute("page", view.getPage());
        }
    }

    private static void addSettingsViewParams(
            final RedirectAttributes redirectAttributes, final SettingsViewForm view) {
        if (view.getPage() != null && view.getPage() > 1) {
            redirectAttributes.addAttribute("page", view.getPage());
        }
        if (view.getPageSize() != 6) {
            redirectAttributes.addAttribute("pageSize", view.getPageSize());
        }
        if (Boolean.TRUE.equals(view.getEdit())) {
            redirectAttributes.addAttribute("edit", true);
        }
    }
}
