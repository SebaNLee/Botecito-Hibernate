package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.ReviewModel;
import ar.edu.itba.paw.models.nuevo.enums.BookingStatus;
import ar.edu.itba.paw.models.nuevo.enums.TargetType;
import ar.edu.itba.paw.persistence.nuevo.BookingDao;
import ar.edu.itba.paw.persistence.nuevo.ReviewDao;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public final class ReviewImpl implements ReviewInterface {

    private final BookingDao bookingDao;
    private final ReviewDao reviewDao;

    @Override
    public Optional<ReviewModel> createReviewForBooking(
            final int bookingId, final int reviewerUserId, final int rating, final String comment) {
        if (rating < 1 || rating > 5) {
            return Optional.empty();
        }

        final Optional<Booking> booking = bookingDao.findById(bookingId);
        if (booking.isEmpty() || !isReviewWindowOpen(booking.get())) {
            return Optional.empty();
        }

        final Optional<Integer> ownerId = bookingDao.findOwnerIdForBookingId(bookingId);
        if (ownerId.isEmpty()) {
            return Optional.empty();
        }

        final ReviewDescriptor descriptor = resolveReviewDescriptor(booking.get(), ownerId.get(), reviewerUserId);
        if (descriptor == null) {
            return Optional.empty();
        }

        if (reviewDao
                .findReviewByBookingSenderAndTargetType(bookingId, reviewerUserId, descriptor.targetType)
                .isPresent()) {
            return Optional.empty();
        }

        return reviewDao.createReview(
                bookingId, reviewerUserId, descriptor.targetType, rating, normalizeComment(comment));
    }

    @Override
    public boolean canCreateReview(final int bookingId, final int reviewerUserId) {
        final Optional<Booking> booking = bookingDao.findById(bookingId);
        if (booking.isEmpty() || !isReviewWindowOpen(booking.get())) {
            return false;
        }
        final Optional<Integer> ownerId = bookingDao.findOwnerIdForBookingId(bookingId);
        if (ownerId.isEmpty()) {
            return false;
        }
        final ReviewDescriptor descriptor = resolveReviewDescriptor(booking.get(), ownerId.get(), reviewerUserId);
        if (descriptor == null) {
            return false;
        }
        return reviewDao
                .findReviewByBookingSenderAndTargetType(bookingId, reviewerUserId, descriptor.targetType)
                .isEmpty();
    }

    private static ReviewDescriptor resolveReviewDescriptor(
            final Booking booking, final int ownerId, final int reviewerUserId) {
        if (booking.getGuestId() == reviewerUserId && ownerId != reviewerUserId) {
            return new ReviewDescriptor(TargetType.ITEM);
        }
        if (ownerId == reviewerUserId && booking.getGuestId() != reviewerUserId) {
            return new ReviewDescriptor(TargetType.USER);
        }
        return null;
    }

    private record ReviewDescriptor(TargetType targetType) {}

    private static String normalizeComment(final String comment) {
        if (comment == null) {
            return null;
        }
        final String trimmed = comment.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isReviewWindowOpen(final Booking booking) {
        if (booking == null || booking.getStatus() == null || booking.getEnd() == null) {
            return false;
        }
        return isBookingEligibleForPostStayReview(booking.getStatus())
                && booking.getEnd().isBefore(LocalDateTime.now());
    }

    private static boolean isBookingEligibleForPostStayReview(final BookingStatus status) {
        return switch (status) {
            case CONFIRMED, PAID, FINISHED -> true;
            default -> false;
        };
    }
}
