package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.SelfBookingData;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.services.BookingService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.SaveSelfBlocksForm;
import ar.edu.itba.paw.webapp.util.DetailAvailabilityPicker;
import ar.edu.itba.paw.webapp.util.JsonForHtml;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import javax.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Component
@RequiredArgsConstructor
public class AvailabilityPresentation {

    private static final int MAX_RETURN_PATH_LENGTH = 512;
    private static final String DEFAULT_AVAILABILITY_BACK_PATH = "/my-boats";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final BookingService bookingInterface;
    private final ToastPresentation toastPresentation;

    public ModelAndView manageAvailabilityPage(
            final BotecitoUserDetails principal,
            final int itemId,
            final String requestedDate,
            final String returnParam,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        final String backPath = resolveBackPath(request, returnParam);
        final SelfBookingData model =
                bookingInterface.getSelfBlocks(itemId, principal.getId(), parseRequestedDate(requestedDate));
        return buildManageAvailabilityView(model, backPath);
    }

    public ModelAndView saveSelfBlocks(
            final BotecitoUserDetails principal,
            final int itemId,
            final String returnParam,
            final SaveSelfBlocksForm form,
            final BindingResult errors,
            final RedirectAttributes redirectAttributes) {
        final String backPath = sanitizeReturnPath(returnParam);
        if (errors.hasErrors()) {
            final SelfBookingData model = bookingInterface.getSelfBlocks(itemId, principal.getId(), form.getDate());
            final ModelAndView mav = buildManageAvailabilityView(model, backPath);
            mav.addObject("toasts", toastPresentation.validationToasts(errors, "saveSelfBlocks"));
            return mav;
        }
        bookingInterface.saveSelfBlockChanges(
                itemId, principal.getId(), form.getDate(), form.deletedBlockIds(), form.updates(), form.creates());
        ToastSupport.success(redirectAttributes, "manageAvailability.msg.saved");
        return availabilityRedirect(itemId, form.getDate().toString(), backPath);
    }

    private ModelAndView availabilityRedirect(
            final int itemId, final String dateStr, final String sanitizedReturnPath) {
        final StringBuilder url =
                new StringBuilder("redirect:/my-boats/").append(itemId).append("/availability");
        if (dateStr != null && !dateStr.isBlank()) {
            url.append("?date=").append(dateStr);
        }
        return new ModelAndView(appendReturnQuery(url.toString(), sanitizedReturnPath));
    }

    private ModelAndView buildManageAvailabilityView(final SelfBookingData model, final String backPath) {
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
        mav.addObject("manageAvailabilityReturnPath", backPath);
        mav.addObject("manageAvailabilityBackPath", backPath != null ? backPath : DEFAULT_AVAILABILITY_BACK_PATH);
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

    static String resolveBackPath(final HttpServletRequest request, final String returnParam) {
        final String safeReturn = sanitizeReturnPath(returnParam);
        if (safeReturn != null) {
            return safeReturn;
        }
        final String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            try {
                final URI uri = URI.create(referer.trim());
                final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
                String path = uri.getPath() == null ? "" : uri.getPath();
                if (!contextPath.isEmpty() && path.startsWith(contextPath)) {
                    path = path.substring(contextPath.length());
                }
                if (path.isEmpty()) {
                    path = "/";
                }
                final String query = uri.getQuery();
                final String candidate = query == null || query.isBlank() ? path : path + "?" + query;
                final String fromReferer = sanitizeReturnPath(candidate);
                if (fromReferer != null) {
                    return fromReferer;
                }
            } catch (final IllegalArgumentException ignored) {
                // ignore malformed referer
            }
        }
        return DEFAULT_AVAILABILITY_BACK_PATH;
    }

    static String sanitizeReturnPath(final String raw) {
        if (raw == null) {
            return null;
        }
        String candidate = raw.trim();
        if (candidate.isEmpty()) {
            return null;
        }
        try {
            candidate = URLDecoder.decode(candidate, StandardCharsets.UTF_8);
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
        candidate = candidate.trim();
        if (candidate.length() > MAX_RETURN_PATH_LENGTH) {
            return null;
        }
        if (!candidate.startsWith("/") || candidate.startsWith("//")) {
            return null;
        }
        if (candidate.contains("://") || candidate.contains("\\") || candidate.indexOf('\n') >= 0) {
            return null;
        }
        if (candidate.indexOf('\r') >= 0 || candidate.indexOf('\0') >= 0) {
            return null;
        }
        return candidate;
    }

    private static LocalDate parseRequestedDate(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (final Exception ignored) {
            return null;
        }
    }

    private static String appendReturnQuery(final String redirectUrl, final String sanitizedReturnPath) {
        if (sanitizedReturnPath == null) {
            return redirectUrl;
        }
        final int q = redirectUrl.indexOf('?');
        final String sep = q >= 0 ? "&" : "?";
        return redirectUrl + sep + "return=" + URLEncoder.encode(sanitizedReturnPath, StandardCharsets.UTF_8);
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
