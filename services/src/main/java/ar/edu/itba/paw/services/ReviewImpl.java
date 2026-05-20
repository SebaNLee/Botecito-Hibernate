package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.TargetEnum;
import ar.edu.itba.paw.persistence.BookingDao;
import ar.edu.itba.paw.persistence.ReviewDao;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public final class ReviewImpl implements ReviewService {

    private final BookingDao bookingDao;
    private final ReviewDao reviewDao;

    @Override
    @Transactional
    public Optional<Review> createReviewForBooking(
            final int bookingId, final int reviewerUserId, final int rating, final String comment) {
        return createReviewForBooking(bookingId, reviewerUserId, rating, comment, null);
    }

    @Override
    @Transactional
    public Optional<Review> createReviewForBooking(
            final int bookingId,
            final int reviewerUserId,
            final int rating,
            final String comment,
            final TargetEnum targetType) {
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

        final TargetEnum resolved = resolveTarget(booking.get(), ownerId.get(), reviewerUserId, targetType);
        if (resolved == null) {
            return Optional.empty();
        }

        if (reviewDao
                .findReviewByBookingSenderAndTargetType(bookingId, reviewerUserId, resolved)
                .isPresent()) {
            return Optional.empty();
        }

        return reviewDao.createReview(bookingId, reviewerUserId, resolved, rating, normalizeComment(comment));
    }

    private static TargetEnum resolveTarget(
            final Booking booking, final int ownerId, final int reviewerUserId, final TargetEnum explicit) {
        final int guestId = booking.getGuest() != null ? booking.getGuest().getId() : 0;
        if (explicit != null) {
            if (explicit == TargetEnum.ITEM && guestId == reviewerUserId) {
                return explicit;
            }
            if (explicit == TargetEnum.USER && (guestId == reviewerUserId || ownerId == reviewerUserId)) {
                return explicit;
            }
            return null;
        }
        if (guestId == reviewerUserId && ownerId != reviewerUserId) {
            return TargetEnum.ITEM;
        }
        if (ownerId == reviewerUserId && guestId != reviewerUserId) {
            return TargetEnum.USER;
        }
        return null;
    }

    private static String normalizeComment(final String comment) {
        if (comment == null) {
            return null;
        }
        final String trimmed = comment.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, List<Review>> findReviewsByBookingIds(final int reviewerUserId) {
        return reviewDao.findReviewsBySender(reviewerUserId).stream()
                .collect(Collectors.groupingBy(r -> r.getBooking().getId()));
    }

    private static boolean isReviewWindowOpen(final Booking booking) {
        if (booking == null || booking.getStatus() == null || booking.getEnd() == null) {
            return false;
        }
        return isBookingEligibleForPostStayReview(booking.getStatus())
                && booking.getEnd().isBefore(LocalDateTime.now(ZoneOffset.UTC));
    }

    private static boolean isBookingEligibleForPostStayReview(final BookingStatusEnum status) {
        return status == BookingStatusEnum.FINISHED;
    }
}
