package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.HostReviewsPage;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.TargetEnum;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.paging.ReviewPaging;
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

    private final ReviewDao reviewDao;
    private final BookingService bookingService;

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

        final Booking booking = bookingService.findById(bookingId);
        if (!isReviewWindowOpen(booking)) {
            return Optional.empty();
        }

        final Users owner = booking.getVersion().getItem().getHost();
        if (owner == null) {
            return Optional.empty();
        }

        final TargetEnum resolved = resolveTarget(booking, owner.getId(), reviewerUserId, targetType);
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

    @Override
    @Transactional(readOnly = true)
    public HostReviewsPage findHostReviewsPage(final int hostUserId, final int page) {
        final int pageSize = ReviewPaging.DEFAULT_PAGE_SIZE;
        final var stats = reviewDao.hostReviewStats(hostUserId);
        final var reviews = reviewDao.findReviewsAboutHost(hostUserId, page, pageSize);
        return new HostReviewsPage(
                new PageModel<>(reviews, page, pageSize, stats.getTotalReviews()),
                stats.getAverageRating().orElse(null));
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
