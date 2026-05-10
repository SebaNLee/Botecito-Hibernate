package ar.edu.itba.paw.services.nuevo;

/**
 * Outcome of attempting to create a pre-booking. Produced by the service from persistence return
 * codes.
 */
public sealed interface PreBookingCreateResult
        permits PreBookingCreateResult.Created,
                PreBookingCreateResult.OutsideAvailability,
                PreBookingCreateResult.Collision,
                PreBookingCreateResult.Unexpected {

    record Created(int bookingId) implements PreBookingCreateResult {
        public Created {
            if (bookingId <= 0) {
                throw new IllegalArgumentException("bookingId must be positive");
            }
        }
    }

    enum OutsideAvailability implements PreBookingCreateResult {
        INSTANCE
    }

    enum Collision implements PreBookingCreateResult {
        INSTANCE
    }

    enum Unexpected implements PreBookingCreateResult {
        INSTANCE
    }
}
