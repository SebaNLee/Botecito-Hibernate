package ar.edu.itba.paw.services.dto;

import ar.edu.itba.paw.models.BookingState;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public final class BookingDecisionBatch {
    private final List<Decision> decisions;

    public BookingDecisionBatch(final List<Decision> decisions) {
        this.decisions = decisions == null ? List.of() : List.copyOf(decisions);
    }

    @Getter
    @AllArgsConstructor
    public static final class Decision {
        private final String token;
        private final BookingState newState;
    }
}
