package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.SelfBookingData;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public final class AvailabilityPresentation {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private AvailabilityPresentation() {}

    public static ModelAndView manageAvailabilityPage(final SelfBookingData model) {
        return buildManageAvailabilityView(model, null);
    }

    public static ModelAndView saveSelfBlocksErrors(
            final SelfBookingData model, final List<Map<String, String>> toasts) {
        return buildManageAvailabilityView(model, toasts);
    }

    public static ModelAndView saveSelfBlocksSuccess(
            final int itemId, final String dateStr, final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "manageAvailability.msg.saved");
        return availabilityRedirect(itemId, dateStr);
    }

    private static ModelAndView availabilityRedirect(final int itemId, final String dateStr) {
        final StringBuilder url =
                new StringBuilder("redirect:/my-boats/").append(itemId).append("/availability");
        if (dateStr != null && !dateStr.isBlank()) {
            url.append("?date=").append(dateStr);
        }
        return new ModelAndView(url.toString());
    }

    private static ModelAndView buildManageAvailabilityView(
            final SelfBookingData model, final List<Map<String, String>> toasts) {
        final ModelAndView mav = new ModelAndView("manage-availability");
        mav.addObject("item", model.getItem());
        mav.addObject("offeredDates", model.getOfferedDates());
        mav.addObject("selectedDate", model.getSelectedDate());
        mav.addObject(
                "manageAvailabilityTodayIso", model.getListingCalendarToday().format(ISO_DATE));
        mav.addObject(
                "manageAvailabilityMaxDateIso",
                model.getListingCalendarMaxInclusive().format(ISO_DATE));
        final var timeline = model.getDayTimeline();
        mav.addObject("timelineAvailableRanges", timeline.availableRanges());
        mav.addObject("timelineBookedRanges", timeline.bookedRanges());
        mav.addObject("timelineSelfBlocks", timeline.selfBlocks());
        mav.addObject("hasTimelineAvailability", model.isHasTimelineAvailability());
        if (toasts != null) {
            mav.addObject("toasts", toasts);
        }
        return mav;
    }
}
