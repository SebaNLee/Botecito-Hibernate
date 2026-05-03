package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.ItemAvailability;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.services.dto.OwnerAvailabilityView;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

public final class OwnerAvailabilityViewBuilder {

    private static final int PICKER_MONTHS_AROUND_TODAY = 2;
    private static final int SLOT_STEP_MINUTES = 30;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private OwnerAvailabilityViewBuilder() {}

    public static OwnerAvailabilityView build(
            final List<ItemAvailability> availabilities,
            final List<ItemBooking> bookings,
            final String requestedDate,
            final int ownerId) {
        final List<ItemBooking> personalBlocks = listPersonalBlocks(bookings, ownerId);

        final LocalDate startDate = LocalDate.now();
        final LocalDate endDate =
                LocalDate.now().plusMonths(PICKER_MONTHS_AROUND_TODAY).with(TemporalAdjusters.lastDayOfMonth());

        final Map<String, TreeSet<String>> scheduledTimesByDate =
                buildScheduledTimesByDate(availabilities, startDate, endDate);
        final Map<String, TreeSet<String>> guestBookedTimesByDate =
                buildGuestBookedTimesByDate(bookings, ownerId, startDate, endDate);

        final List<String> offeredDates = new ArrayList<>(scheduledTimesByDate.keySet());
        final String selectedDate = resolveSelectedDate(requestedDate, offeredDates);

        final List<OwnerAvailabilityView.Slot> slots = selectedDate == null
                ? List.of()
                : buildSlots(
                        scheduledTimesByDate.getOrDefault(selectedDate, new TreeSet<>()),
                        guestBookedTimesByDate.getOrDefault(selectedDate, new TreeSet<>()),
                        indexOwnerPersonalBlockStartsBySlot(personalBlocks, selectedDate));

        final List<String> blockedDates = new ArrayList<>();
        for (final ItemBooking block : personalBlocks) {
            addBookingDatesInRange(block, startDate, endDate, blockedDates);
        }

        return new OwnerAvailabilityView(
                toJsonArray(offeredDates),
                toJsonArray(blockedDates),
                selectedDate,
                slots,
                slotsToJson(slots),
                personalBlocks,
                toPersonalBlockRows(personalBlocks));
    }

    private static List<ItemBooking> listPersonalBlocks(final List<ItemBooking> bookings, final int ownerId) {
        final List<ItemBooking> out = new ArrayList<>();
        for (final ItemBooking booking : bookings) {
            if (booking.getGuestId() != null
                    && Objects.equals(booking.getGuestId(), ownerId)
                    && booking.getState() == BookingState.BOOKING_CONFIRMED) {
                out.add(booking);
            }
        }
        return out;
    }

    private static List<OwnerAvailabilityView.PersonalBlockRow> toPersonalBlockRows(
            final List<ItemBooking> personalBlocks) {
        final List<ItemBooking> sorted = new ArrayList<>(personalBlocks);
        sorted.sort(Comparator.comparing(ItemBooking::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())));
        final List<OwnerAvailabilityView.PersonalBlockRow> rows = new ArrayList<>();
        for (final ItemBooking block : sorted) {
            if (block.getId() == null || block.getStartTime() == null || block.getEndTime() == null) {
                continue;
            }
            rows.add(new OwnerAvailabilityView.PersonalBlockRow(
                    block.getId(),
                    block.getStartTime().toLocalDate().format(DATE_FORMAT),
                    block.getStartTime().toLocalTime().format(TIME_FORMAT),
                    block.getEndTime().toLocalTime().format(TIME_FORMAT)));
        }
        return rows;
    }

    private static void addBookingDatesInRange(
            final ItemBooking booking,
            final LocalDate rangeStart,
            final LocalDate rangeEnd,
            final List<String> outIsoDates) {
        if (booking.getStartTime() == null || booking.getEndTime() == null) {
            return;
        }
        LocalDate d = booking.getStartTime().toLocalDate();
        final LocalDate last = booking.getEndTime().toLocalDate();
        while (!d.isAfter(last)) {
            if (!d.isBefore(rangeStart) && !d.isAfter(rangeEnd)) {
                final String iso = d.format(DATE_FORMAT);
                if (!outIsoDates.contains(iso)) {
                    outIsoDates.add(iso);
                }
            }
            d = d.plusDays(1);
        }
    }

    private static Map<String, TreeSet<String>> buildScheduledTimesByDate(
            final List<ItemAvailability> availabilities, final LocalDate startDate, final LocalDate endDate) {
        final Map<String, TreeSet<String>> collected = new TreeMap<>();
        for (final ItemAvailability availability : availabilities) {
            final DayOfWeek weekday = availability.getWeekday();
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                if (date.getDayOfWeek() != weekday) {
                    continue;
                }
                addTimeRange(
                        collected, date.format(DATE_FORMAT), availability.getStartTime(), availability.getEndTime());
            }
        }
        return collected;
    }

    private static Map<String, TreeSet<String>> buildGuestBookedTimesByDate(
            final List<ItemBooking> bookings, final int ownerId, final LocalDate startDate, final LocalDate endDate) {
        final Map<String, TreeSet<String>> collected = new TreeMap<>();
        for (final ItemBooking booking : bookings) {
            if (!isBlockingBooking(booking)) {
                continue;
            }
            if (booking.getGuestId() != null && Objects.equals(booking.getGuestId(), ownerId)) {
                continue;
            }
            OffsetDateTime cursor = booking.getStartTime();
            final OffsetDateTime end = booking.getEndTime();
            while (cursor.isBefore(end)) {
                final LocalDate date = cursor.toLocalDate();
                if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                    collected
                            .computeIfAbsent(date.format(DATE_FORMAT), ignored -> new TreeSet<>())
                            .add(cursor.toLocalTime().format(TIME_FORMAT));
                }
                cursor = cursor.plusMinutes(SLOT_STEP_MINUTES);
            }
        }
        return collected;
    }

    private static boolean isBlockingBooking(final ItemBooking booking) {
        return booking.getState() == null
                || booking.getState() == BookingState.BOOKING_PENDING
                || booking.getState() == BookingState.BOOKING_CONFIRMED
                || booking.getState() == BookingState.BOOKING_PAYMENT_SUBMITTED
                || booking.getState() == BookingState.BOOKING_PAID;
    }

    private static Map<String, Integer> indexOwnerPersonalBlockStartsBySlot(
            final List<ItemBooking> personalBlocks, final String selectedDateIso) {
        final Map<String, Integer> index = new TreeMap<>();
        final LocalDate parsedSelected;
        try {
            parsedSelected = LocalDate.parse(selectedDateIso);
        } catch (final DateTimeParseException exception) {
            return index;
        }
        for (final ItemBooking block : personalBlocks) {
            if (block.getId() == null || block.getStartTime() == null || block.getEndTime() == null) {
                continue;
            }
            OffsetDateTime cursor = block.getStartTime();
            final OffsetDateTime end = block.getEndTime();
            while (cursor.isBefore(end)) {
                if (parsedSelected.equals(cursor.toLocalDate())) {
                    index.put(cursor.toLocalTime().format(TIME_FORMAT), block.getId());
                }
                cursor = cursor.plusMinutes(SLOT_STEP_MINUTES);
            }
        }
        return index;
    }

    private static List<OwnerAvailabilityView.Slot> buildSlots(
            final TreeSet<String> scheduledTimes,
            final TreeSet<String> guestBookedTimes,
            final Map<String, Integer> ownerBlockStartToBookingId) {
        final List<OwnerAvailabilityView.Slot> slots = new ArrayList<>();
        for (final String time : scheduledTimes) {
            final LocalTime startTime = LocalTime.parse(time);
            final String endTime = startTime.plusMinutes(SLOT_STEP_MINUTES).format(TIME_FORMAT);
            final boolean guestBooked = guestBookedTimes.contains(time);
            final Integer blockBookingId = ownerBlockStartToBookingId.get(time);
            final String state = blockBookingId != null ? "BLOCKED" : (guestBooked ? "BOOKED" : "AVAILABLE");
            slots.add(new OwnerAvailabilityView.Slot(time, endTime, state, blockBookingId));
        }
        return slots;
    }

    private static void addTimeRange(
            final Map<String, TreeSet<String>> collected,
            final String date,
            final LocalTime startTime,
            final LocalTime endTime) {
        final int startMinute = startTime.toSecondOfDay() / 60;
        final int endMinute = endTime.toSecondOfDay() / 60;
        for (int minute = startMinute; minute < endMinute; minute += SLOT_STEP_MINUTES) {
            collected
                    .computeIfAbsent(date, ignored -> new TreeSet<>())
                    .add(LocalTime.ofSecondOfDay((long) minute * 60).format(TIME_FORMAT));
        }
    }

    private static String resolveSelectedDate(final String requestedDate, final List<String> offeredDates) {
        if (requestedDate != null && offeredDates.contains(requestedDate)) {
            return requestedDate;
        }
        return offeredDates.isEmpty() ? null : offeredDates.get(0);
    }

    private static String slotsToJson(final List<OwnerAvailabilityView.Slot> slots) {
        final StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < slots.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            final OwnerAvailabilityView.Slot slot = slots.get(i);
            json.append("{\"start\":\"")
                    .append(jsonEscape(slot.getStartTime()))
                    .append("\",\"end\":\"")
                    .append(jsonEscape(slot.getEndTime()))
                    .append("\",\"state\":\"")
                    .append(jsonEscape(slot.getState()))
                    .append("\"}");
        }
        return json.append(']').toString();
    }

    private static String jsonEscape(final String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String toJsonArray(final List<String> values) {
        final StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append('"').append(values.get(i).replace("\"", "\\\"")).append('"');
        }
        return json.append(']').toString();
    }
}
