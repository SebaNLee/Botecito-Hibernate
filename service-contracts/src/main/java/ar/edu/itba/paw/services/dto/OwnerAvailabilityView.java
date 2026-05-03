package ar.edu.itba.paw.services.dto;

import ar.edu.itba.paw.models.ItemBooking;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public final class OwnerAvailabilityView {
    private final String offeredDatesJson;
    private final String blockedDatesJson;
    private final String selectedDate;
    private final List<Slot> slots;
    private final String slotsStateJson;
    private final List<ItemBooking> personalBlocks;
    private final List<PersonalBlockRow> personalBlockRows;

    public OwnerAvailabilityView(
            final String offeredDatesJson,
            final String blockedDatesJson,
            final String selectedDate,
            final List<Slot> slots,
            final String slotsStateJson,
            final List<ItemBooking> personalBlocks,
            final List<PersonalBlockRow> personalBlockRows) {
        this.offeredDatesJson = offeredDatesJson == null ? "[]" : offeredDatesJson;
        this.blockedDatesJson = blockedDatesJson == null ? "[]" : blockedDatesJson;
        this.selectedDate = selectedDate == null ? "" : selectedDate;
        this.slots = slots == null ? List.of() : List.copyOf(slots);
        this.slotsStateJson = slotsStateJson == null ? "[]" : slotsStateJson;
        this.personalBlocks = personalBlocks == null ? List.of() : List.copyOf(personalBlocks);
        this.personalBlockRows = personalBlockRows == null ? List.of() : List.copyOf(personalBlockRows);
    }

    @Getter
    @AllArgsConstructor
    public static final class Slot {
        private final String startTime;
        private final String endTime;
        private final String state;
        private final Integer blockBookingId;

        public String getModalIdSuffix() {
            return startTime == null ? "" : startTime.replace(":", "");
        }
    }

    @Getter
    @AllArgsConstructor
    public static final class PersonalBlockRow {
        private final int bookingId;
        private final String dateIso;
        private final String startTime;
        private final String endTime;
    }
}
