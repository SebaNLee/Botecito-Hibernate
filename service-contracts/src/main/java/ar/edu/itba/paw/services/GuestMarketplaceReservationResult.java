package ar.edu.itba.paw.services;

public record GuestMarketplaceReservationResult(Outcome outcome, String ownerDisplayName) {

    public enum Outcome {
        SUCCESS,
        SELF_BOOKING,
        ERROR
    }

    public static GuestMarketplaceReservationResult success(final String ownerDisplayName) {
        return new GuestMarketplaceReservationResult(Outcome.SUCCESS, ownerDisplayName == null ? "" : ownerDisplayName);
    }

    public static GuestMarketplaceReservationResult selfBooking() {
        return new GuestMarketplaceReservationResult(Outcome.SELF_BOOKING, "");
    }

    public static GuestMarketplaceReservationResult error() {
        return new GuestMarketplaceReservationResult(Outcome.ERROR, "");
    }
}
