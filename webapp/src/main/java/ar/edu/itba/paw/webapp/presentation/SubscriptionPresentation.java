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
        if (view.getPage() != null && view.getPage() > 1) {
            redirectAttributes.addAttribute("page", view.getPage());
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
        final int page = view.getPage() == null ? 1 : view.getPage();
        final int pageSize = view.getPageSize() == null ? 12 : view.getPageSize();

        if ("reviews".equals(tab)) {
            redirectAttributes.addAttribute("tab", "reviews");
            if (page > 1) {
                redirectAttributes.addAttribute("page", page);
            }
        } else {
            if (page > 1) {
                redirectAttributes.addAttribute("page", page);
            }
            if (StringUtils.hasText(view.getSortBy()) && !"newest".equals(view.getSortBy())) {
                redirectAttributes.addAttribute("sortBy", view.getSortBy());
            }
            if (pageSize != 12) {
                redirectAttributes.addAttribute("pageSize", pageSize);
            }
        }
    }

    private static void addSettingsViewParams(
            final RedirectAttributes redirectAttributes, final SettingsViewForm view) {
        if (view == null) {
            return;
        }
        final int page = view.getPage() == null ? 1 : view.getPage();
        final int pageSize = view.getPageSize() == null ? 6 : view.getPageSize();
        if (page > 1) {
            redirectAttributes.addAttribute("page", page);
        }
        if (pageSize != 6) {
            redirectAttributes.addAttribute("pageSize", pageSize);
        }
        if (Boolean.TRUE.equals(view.getEdit())) {
            redirectAttributes.addAttribute("edit", true);
        }
    }
}
