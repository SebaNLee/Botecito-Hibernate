package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.BookingOrm;
import ar.edu.itba.paw.models.entity.BookingStatusEnumOrm;
import ar.edu.itba.paw.models.entity.ReviewOrm;
import ar.edu.itba.paw.models.entity.TargetEnumOrm;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.ReviewDao;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public final class ReviewImpl implements ReviewInterface {

    private final BookingDao bookingDao;
    private final ReviewDao reviewDao;

    @Override
    @Transactional
    public Optional<ReviewOrm> createReviewForBooking(
            final int bookingId, final int reviewerUserId, final int rating, final String comment) {
        if (rating < 1 || rating > 5) {
            return Optional.empty();
        }

        final Optional<BookingOrm> booking = bookingDao.findById(bookingId);
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
    @Transactional(readOnly = true)
    public boolean canCreateReview(final int bookingId, final int reviewerUserId) {
        final Optional<BookingOrm> booking = bookingDao.findById(bookingId);
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
            final BookingOrm booking, final int ownerId, final int reviewerUserId) {
        final int guestId = booking.getGuest() != null ? booking.getGuest().getId() : 0;
        if (guestId == reviewerUserId && ownerId != reviewerUserId) {
            return new ReviewDescriptor(TargetEnumOrm.ITEM);
        }
        if (ownerId == reviewerUserId && guestId != reviewerUserId) {
            return new ReviewDescriptor(TargetEnumOrm.USER);
        }
        return null;
    }

    private record ReviewDescriptor(TargetEnumOrm targetType) {}

    private static String normalizeComment(final String comment) {
        if (comment == null) {
            return null;
        }
        final String trimmed = comment.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isReviewWindowOpen(final BookingOrm booking) {
        if (booking == null || booking.getStatus() == null || booking.getEnd() == null) {
            return false;
        }
        return isBookingEligibleForPostStayReview(booking.getStatus())
                && booking.getEnd().isBefore(LocalDateTime.now());
    }

    private static boolean isBookingEligibleForPostStayReview(final BookingStatusEnumOrm status) {
        return switch (status) {
            case CONFIRMED, PAID, FINISHED -> true;
            default -> false;
        };
    }
}
