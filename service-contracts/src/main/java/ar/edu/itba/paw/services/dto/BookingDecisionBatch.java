package ar.edu.itba.paw.services.dto;

import ar.edu.itba.paw.models.BookingState;
import java.util.List;

public final class BookingDecisionBatch {
    private final List<Decision> decisions;

    public BookingDecisionBatch(final List<Decision> decisions) {
        this.decisions = decisions == null ? List.of() : List.copyOf(decisions);
    }

    public List<Decision> getDecisions() {
        return decisions;
    }

    public static final class Decision {
        private final String token;
        private final BookingState newState;

        public Decision(final String token, final BookingState newState) {
            this.token = token;
            this.newState = newState;
        }

        public String getToken() {
            return token;
        }

        public BookingState getNewState() {
            return newState;
        }
    }
}
