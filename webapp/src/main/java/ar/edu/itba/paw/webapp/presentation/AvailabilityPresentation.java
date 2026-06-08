package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.SelfBookingData;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.webapp.util.DetailAvailabilityPicker;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public final class AvailabilityPresentation {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
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
        final String timezone = model.getTimezone();
        mav.addObject("item", model.getItem());
        mav.addObject("offeredDates", model.getOfferedDates());
        mav.addObject("selectedDate", model.getSelectedDate());
        mav.addObject(
                "manageAvailabilityTodayIso",
                DetailAvailabilityPicker.listingCalendarToday(timezone).format(ISO_DATE));
        mav.addObject(
                "manageAvailabilityMaxDateIso",
                DetailAvailabilityPicker.listingCalendarMaxInclusive(timezone).format(ISO_DATE));
        final Version version = model.getItem().getLatestVersion();
        final List<Availability> availabilityWindows =
                version == null || version.getAvailabilities() == null ? List.of() : version.getAvailabilities();
        final Set<Integer> selfBlockIds =
                model.getOwnerSelfBlocks().stream().map(Booking::getId).collect(Collectors.toSet());
        final DayTimelineModel timeline = buildDayTimelineModel(
                model.getSelectedDate(),
                availabilityWindows,
                model.getActiveBookings(),
                model.getOwnerSelfBlocks(),
                selfBlockIds,
                timezone);
        mav.addObject("timelineAvailableRanges", timeline.availableRanges());
        mav.addObject("timelineBookedRanges", timeline.bookedRanges());
        mav.addObject("timelineSelfBlocks", timeline.selfBlocks());
        mav.addObject(
                "hasTimelineAvailability", hasAvailabilityWindowsForDate(model.getSelectedDate(), availabilityWindows));
        if (toasts != null) {
            mav.addObject("toasts", toasts);
        }
        return mav;
    }

    private static boolean hasAvailabilityWindowsForDate(
            final LocalDate selectedDate, final List<Availability> availabilities) {
        if (selectedDate == null || availabilities == null) {
            return false;
        }
        return availabilities.stream()
                .anyMatch(a -> a.getWeekday() != null
                        && a.getWeekday()
                                .name()
                                .equals(selectedDate.getDayOfWeek().name())
                        && a.getStartTime() != null
                        && a.getEndTime() != null
                        && a.getEndTime().isAfter(a.getStartTime()));
    }

    private static DayTimelineModel buildDayTimelineModel(
            final LocalDate selectedDate,
            final List<Availability> availabilities,
            final List<Booking> bookings,
            final List<Booking> ownerSelfBlocks,
            final Set<Integer> selfBlockBookingIds,
            final String timezone) {
        if (selectedDate == null) {
            return new DayTimelineModel(List.of(), List.of(), List.of());
        }
        final ZoneId zone = DetailAvailabilityPicker.listingZoneOrUtc(timezone);
        final List<TimeRangeRow> availableRanges = new ArrayList<>();
        for (final Availability availability : availabilities) {
            if (availability
                            .getWeekday()
                            .name()
                            .equals(selectedDate.getDayOfWeek().name())
                    && availability.getStartTime() != null
                    && availability.getEndTime() != null
                    && availability.getEndTime().isAfter(availability.getStartTime())) {
                availableRanges.add(new TimeRangeRow(
                        availability.getStartTime().format(TIME_FMT),
                        availability.getEndTime().format(TIME_FMT)));
            }
        }
        final List<TimeRangeRow> bookedRanges = new ArrayList<>();
        for (final Booking booking : bookings) {
            if (!isBlockingState(booking.getStatus())
                    || booking.getStart() == null
                    || booking.getEnd() == null
                    || booking.getId() != null && selfBlockBookingIds.contains(booking.getId())) {
                continue;
            }
            final LocalDate bookingDate = booking.getStart()
                    .atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(zone)
                    .toLocalDate();
            if (!selectedDate.equals(bookingDate)) {
                continue;
            }
            bookedRanges.add(new TimeRangeRow(
                    booking.getStart()
                            .atZone(ZoneOffset.UTC)
                            .withZoneSameInstant(zone)
                            .toLocalTime()
                            .format(TIME_FMT),
                    booking.getEnd()
                            .atZone(ZoneOffset.UTC)
                            .withZoneSameInstant(zone)
                            .toLocalTime()
                            .format(TIME_FMT)));
        }
        final List<SelfBlockRow> selfBlocks = new ArrayList<>();
        for (final Booking block : ownerSelfBlocks) {
            if (block.getId() == null || block.getStart() == null || block.getEnd() == null) {
                continue;
            }
            final LocalDate blockDate = block.getStart()
                    .atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(zone)
                    .toLocalDate();
            if (!selectedDate.equals(blockDate)) {
                continue;
            }
            selfBlocks.add(new SelfBlockRow(
                    block.getId(),
                    block.getStart()
                            .atZone(ZoneOffset.UTC)
                            .withZoneSameInstant(zone)
                            .toLocalTime()
                            .format(TIME_FMT),
                    block.getEnd()
                            .atZone(ZoneOffset.UTC)
                            .withZoneSameInstant(zone)
                            .toLocalTime()
                            .format(TIME_FMT)));
        }
        return new DayTimelineModel(availableRanges, bookedRanges, selfBlocks);
    }

    private static boolean isBlockingState(final BookingStatusEnum status) {
        return status == BookingStatusEnum.PENDING
                || status == BookingStatusEnum.ACCEPTED
                || status == BookingStatusEnum.PAID
                || status == BookingStatusEnum.CONFIRMED;
    }

    public record DayTimelineModel(
            List<TimeRangeRow> availableRanges, List<TimeRangeRow> bookedRanges, List<SelfBlockRow> selfBlocks) {

        public DayTimelineModel {
            availableRanges = List.copyOf(availableRanges);
            bookedRanges = List.copyOf(bookedRanges);
            selfBlocks = List.copyOf(selfBlocks);
        }
    }

    @Getter
    @RequiredArgsConstructor
    public static class TimeRangeRow {
        private final String startTime;
        private final String endTime;
    }

    @Getter
    @RequiredArgsConstructor
    public static class SelfBlockRow {
        private final int id;
        private final String startTime;
        private final String endTime;
    }
}
