package ar.edu.itba.paw.services.dto;

import ar.edu.itba.paw.models.ItemBooking;
import java.util.List;

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

    public String getOfferedDatesJson() {
        return offeredDatesJson;
    }

    public String getBlockedDatesJson() {
        return blockedDatesJson;
    }

    public String getSelectedDate() {
        return selectedDate;
    }

    public List<Slot> getSlots() {
        return slots;
    }

    public String getSlotsStateJson() {
        return slotsStateJson;
    }

    public List<ItemBooking> getPersonalBlocks() {
        return personalBlocks;
    }

    public List<PersonalBlockRow> getPersonalBlockRows() {
        return personalBlockRows;
    }

    public static final class Slot {
        private final String startTime;
        private final String endTime;
        private final String state;
        private final Integer blockBookingId;

        public Slot(final String startTime, final String endTime, final String state, final Integer blockBookingId) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.state = state;
            this.blockBookingId = blockBookingId;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getState() {
            return state;
        }

        public Integer getBlockBookingId() {
            return blockBookingId;
        }

        public String getModalIdSuffix() {
            return startTime == null ? "" : startTime.replace(":", "");
        }
    }

    public static final class PersonalBlockRow {
        private final int bookingId;
        private final String dateIso;
        private final String startTime;
        private final String endTime;

        public PersonalBlockRow(
                final int bookingId, final String dateIso, final String startTime, final String endTime) {
            this.bookingId = bookingId;
            this.dateIso = dateIso;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public int getBookingId() {
            return bookingId;
        }

        public String getDateIso() {
            return dateIso;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }
    }
}
