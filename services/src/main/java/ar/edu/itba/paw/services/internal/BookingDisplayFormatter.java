package ar.edu.itba.paw.services.internal;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.context.i18n.LocaleContextHolder;

public final class BookingDisplayFormatter {

    private BookingDisplayFormatter() {}

    public static String formatDateLabel(final OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        final Locale locale = LocaleContextHolder.getLocale();
        return dateTime.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale));
    }

    public static String formatTimeRangeLabel(final OffsetDateTime startTime, final OffsetDateTime endTime) {
        if (startTime == null || endTime == null) {
            return "";
        }
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return startTime.format(formatter) + " hs - " + endTime.format(formatter) + " hs";
    }

    public static String formatTotalPriceLabel(
            final OffsetDateTime startTime, final OffsetDateTime endTime, final Integer pricePerHour) {
        if (startTime == null || endTime == null || pricePerHour == null || pricePerHour < 0) {
            return "";
        }
        final long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) {
            return "";
        }
        final BigDecimal totalPrice = BigDecimal.valueOf(pricePerHour.longValue())
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR"));
        numberFormat.setMinimumFractionDigits(0);
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat.format(totalPrice);
    }

    public static String statusMessageCode(final BookingState state) {
        if (state == null) {
            return "profile.sentBookings.status.unknown";
        }
        return switch (state) {
            case BOOKING_PENDING -> "profile.sentBookings.status.pending";
            case BOOKING_CONFIRMED -> "profile.sentBookings.status.confirmed";
            case BOOKING_REJECTED -> "profile.sentBookings.status.rejected";
            case BOOKING_CANCELLED -> "profile.sentBookings.status.cancelled";
            case BOOKING_COMPLETED -> "profile.sentBookings.status.completed";
            case BOOKING_PAYMENT_SUBMITTED -> "profile.sentBookings.status.paymentSubmitted";
            case BOOKING_PAID -> "profile.sentBookings.status.paid";
            case BOOKING_PAYMENT_REFUSED -> "profile.sentBookings.status.paymentRefused";
        };
    }

    public static String resolvePaymentAlias(final User user) {
        if (user == null) {
            return "";
        }
        if (user.getPaymentAlias() != null && !user.getPaymentAlias().isBlank()) {
            return user.getPaymentAlias();
        }
        return user.getEmail() == null ? "" : user.getEmail();
    }

    public static boolean shouldExposePaymentAliasToGuest(final BookingState state) {
        return state == BookingState.BOOKING_CONFIRMED || state == BookingState.BOOKING_PAYMENT_REFUSED;
    }

    public static boolean shouldRetainBookingForDeletion(final BookingState state) {
        return state != BookingState.BOOKING_REJECTED && state != BookingState.BOOKING_CANCELLED;
    }

    public static boolean matchesAnyStatusFilter(final String statusMessageCode, final java.util.List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        for (final String filter : filters) {
            if (matchesExactStatusFilter(statusMessageCode, filter)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesExactStatusFilter(final String statusMessageCode, final String filter) {
        return switch (filter) {
            case "pending" -> "profile.sentBookings.status.pending".equals(statusMessageCode);
            case "confirmed" -> "profile.sentBookings.status.confirmed".equals(statusMessageCode);
            case "paymentSubmitted" -> "profile.sentBookings.status.paymentSubmitted".equals(statusMessageCode);
            case "paid" -> "profile.sentBookings.status.paid".equals(statusMessageCode);
            case "paymentRefused" -> "profile.sentBookings.status.paymentRefused".equals(statusMessageCode);
            case "completed" -> "profile.sentBookings.status.completed".equals(statusMessageCode);
            case "rejected" -> "profile.sentBookings.status.rejected".equals(statusMessageCode);
            case "cancelled" -> "profile.sentBookings.status.cancelled".equals(statusMessageCode);
            default -> false;
        };
    }

    public static boolean matchesBoatNameSearch(final String itemTitle, final String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        if (itemTitle == null) {
            return false;
        }
        return itemTitle.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }
}
