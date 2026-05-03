package ar.edu.itba.paw.services.dto;

import ar.edu.itba.paw.models.ItemBooking;
import java.util.List;
import java.util.Map;

public final class EditConflictView {
    private final List<ItemBooking> activeBookings;
    private final Map<Integer, String> guestNames;
    private final Map<Integer, String> startLabels;
    private final Map<Integer, String> friendlyDates;
    private final Map<Integer, String> friendlyTimeRanges;
    private final Map<Integer, String> friendlyPrices;
    private final Map<Integer, String> statusCodes;

    public EditConflictView(
            final List<ItemBooking> activeBookings,
            final Map<Integer, String> guestNames,
            final Map<Integer, String> startLabels,
            final Map<Integer, String> friendlyDates,
            final Map<Integer, String> friendlyTimeRanges,
            final Map<Integer, String> friendlyPrices,
            final Map<Integer, String> statusCodes) {
        this.activeBookings = activeBookings == null ? List.of() : List.copyOf(activeBookings);
        this.guestNames = guestNames == null ? Map.of() : Map.copyOf(guestNames);
        this.startLabels = startLabels == null ? Map.of() : Map.copyOf(startLabels);
        this.friendlyDates = friendlyDates == null ? Map.of() : Map.copyOf(friendlyDates);
        this.friendlyTimeRanges = friendlyTimeRanges == null ? Map.of() : Map.copyOf(friendlyTimeRanges);
        this.friendlyPrices = friendlyPrices == null ? Map.of() : Map.copyOf(friendlyPrices);
        this.statusCodes = statusCodes == null ? Map.of() : Map.copyOf(statusCodes);
    }

    public List<ItemBooking> getActiveBookings() {
        return activeBookings;
    }

    public Map<Integer, String> getGuestNames() {
        return guestNames;
    }

    public Map<Integer, String> getStartLabels() {
        return startLabels;
    }

    public Map<Integer, String> getFriendlyDates() {
        return friendlyDates;
    }

    public Map<Integer, String> getFriendlyTimeRanges() {
        return friendlyTimeRanges;
    }

    public Map<Integer, String> getFriendlyPrices() {
        return friendlyPrices;
    }

    public Map<Integer, String> getStatusCodes() {
        return statusCodes;
    }
}
