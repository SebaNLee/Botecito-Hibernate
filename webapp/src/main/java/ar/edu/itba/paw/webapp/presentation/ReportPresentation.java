package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.webapp.util.ToastSupport;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class ReportPresentation {

    private static final String MESSAGE_PREFIX = "report";

    public ModelAndView submitReportResult(
            final int itemId, final HttpServletRequest request, final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, MESSAGE_PREFIX + ".success");
        return detailRedirect(itemId, request);
    }

    private static ModelAndView detailRedirect(final int itemId, final HttpServletRequest request) {
        final String query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return new ModelAndView("redirect:/item/" + itemId);
        }
        return new ModelAndView("redirect:/item/" + itemId + "?" + query);
    }
}
