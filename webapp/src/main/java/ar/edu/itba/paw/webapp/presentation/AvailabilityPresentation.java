package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.SelfBookingData;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.webapp.util.DetailAvailabilityPicker;
import ar.edu.itba.paw.webapp.util.JsonForHtml;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class AvailabilityPresentation {

    private static final String MANAGE_AVAILABILITY_BACK_PATH = "/my-boats";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ToastPresentation toastPresentation;

    public ModelAndView manageAvailabilityPage(final SelfBookingData model) {
        return buildManageAvailabilityView(model);
    }

    public ModelAndView saveSelfBlocksErrors(final SelfBookingData model, final BindingResult errors) {
        final ModelAndView mav = buildManageAvailabilityView(model);
        mav.addObject("toasts", toastPresentation.validationToasts(errors, "saveSelfBlocks"));
        return mav;
    }

    public ModelAndView saveSelfBlocksSuccess(
            final int itemId, final String dateStr, final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "manageAvailability.msg.saved");
        return availabilityRedirect(itemId, dateStr);
    }

    private ModelAndView availabilityRedirect(final int itemId, final String dateStr) {
        final StringBuilder url =
                new StringBuilder("redirect:/my-boats/").append(itemId).append("/availability");
        if (dateStr != null && !dateStr.isBlank()) {
            url.append("?date=").append(dateStr);
        }
        return new ModelAndView(url.toString());
    }

    private ModelAndView buildManageAvailabilityView(final SelfBookingData model) {
        final ModelAndView mav = new ModelAndView("manage-availability");
        final String timezone = model.getTimezone();
        mav.addObject("item", model.getItem());
        mav.addObject("offeredDatesJson", JsonForHtml.serialize(model.getOfferedDates()));
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
        mav.addObject(
                "dayTimelineJson",
                buildDayTimelineJson(
                        model.getSelectedDate(),
                        availabilityWindows,
                        model.getActiveBookings(),
                        model.getOwnerSelfBlocks(),
                        selfBlockIds,
                        timezone));
        mav.addObject(
                "hasTimelineAvailability", hasAvailabilityWindowsForDate(model.getSelectedDate(), availabilityWindows));
        mav.addObject("manageAvailabilityBackPath", MANAGE_AVAILABILITY_BACK_PATH);
        return mav;
    }

    static boolean hasAvailabilityWindowsForDate(
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

    static String buildDayTimelineJson(
            final LocalDate selectedDate,
            final List<Availability> availabilities,
            final List<Booking> bookings,
            final List<Booking> ownerSelfBlocks,
            final Set<Integer> selfBlockBookingIds,
            final String timezone) {
        if (selectedDate == null) {
            return JsonForHtml.serialize(Map.of(
                    "availableRanges", List.of(),
                    "bookedRanges", List.of(),
                    "selfBlocks", List.of()));
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
        return toDayTimelineJson(availableRanges, bookedRanges, selfBlocks);
    }

    private static boolean isBlockingState(final BookingStatusEnum status) {
        return status == BookingStatusEnum.PENDING
                || status == BookingStatusEnum.ACCEPTED
                || status == BookingStatusEnum.PAID
                || status == BookingStatusEnum.CONFIRMED;
    }

    public static LocalDate parseRequestedDate(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static String toDayTimelineJson(
            final List<TimeRangeRow> availableRanges,
            final List<TimeRangeRow> bookedRanges,
            final List<SelfBlockRow> selfBlocks) {
        final Map<String, Object> timeline = new LinkedHashMap<>();
        timeline.put("availableRanges", toTimeRangeMaps(availableRanges));
        timeline.put("bookedRanges", toTimeRangeMaps(bookedRanges));
        final List<Map<String, Object>> selfBlockMaps = new ArrayList<>();
        for (final SelfBlockRow block : selfBlocks) {
            final Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", block.getId());
            row.put("startTime", block.getStartTime());
            row.put("endTime", block.getEndTime());
            selfBlockMaps.add(row);
        }
        timeline.put("selfBlocks", selfBlockMaps);
        return JsonForHtml.serialize(timeline);
    }

    private static List<Map<String, String>> toTimeRangeMaps(final List<TimeRangeRow> rows) {
        final List<Map<String, String>> maps = new ArrayList<>();
        for (final TimeRangeRow row : rows) {
            final Map<String, String> map = new LinkedHashMap<>();
            map.put("startTime", row.getStartTime());
            map.put("endTime", row.getEndTime());
            maps.add(map);
        }
        return maps;
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
