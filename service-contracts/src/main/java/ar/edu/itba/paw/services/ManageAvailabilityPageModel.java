package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.Item;
import java.util.List;

/** Data assembled for the owner manage-availability screen. */
public final class ManageAvailabilityPageModel {

    private final Item item;
    private final List<String> offeredDates;
    private final List<String> blockedDates;
    private final String selectedDate;
    private final List<ManageAvailabilitySlotRow> slots;
    private final List<ManageAvailabilityPersonalBlockRow> personalBlockRows;

    public ManageAvailabilityPageModel(
            final Item item,
            final List<String> offeredDates,
            final List<String> blockedDates,
            final String selectedDate,
            final List<ManageAvailabilitySlotRow> slots,
            final List<ManageAvailabilityPersonalBlockRow> personalBlockRows) {
        this.item = item;
        this.offeredDates = List.copyOf(offeredDates);
        this.blockedDates = List.copyOf(blockedDates);
        this.selectedDate = selectedDate;
        this.slots = List.copyOf(slots);
        this.personalBlockRows = List.copyOf(personalBlockRows);
    }

    public Item getItem() {
        return item;
    }

    public List<String> getOfferedDates() {
        return offeredDates;
    }

    public List<String> getBlockedDates() {
        return blockedDates;
    }

    public String getSelectedDate() {
        return selectedDate;
    }

    public List<ManageAvailabilitySlotRow> getSlots() {
        return slots;
    }

    public List<ManageAvailabilityPersonalBlockRow> getPersonalBlockRows() {
        return personalBlockRows;
    }
}
