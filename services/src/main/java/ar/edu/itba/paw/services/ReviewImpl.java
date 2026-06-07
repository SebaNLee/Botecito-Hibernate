package ar.edu.itba.paw.services;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public final class ReviewImpl implements ReviewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewImpl.class);

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

        final Optional<Review> review =
                reviewDao.createReview(bookingId, reviewerUserId, resolved, rating, normalizeComment(comment));
        review.ifPresent(r -> LOGGER.info(
                "Review created: user {} reviewed booking {} with rating {} for {}",
                reviewerUserId,
                bookingId,
                rating,
                resolved));
        return review;
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
    public PageModel<Review> findReviewsAboutHost(final int hostUserId, final int page) {
        final int pageSize = ReviewPaging.DEFAULT_PAGE_SIZE;
        final long total = reviewDao.countReviewsAboutHost(hostUserId);
        return new PageModel<>(reviewDao.findReviewsAboutHost(hostUserId, page, pageSize), page, pageSize, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Double> averageRatingAboutHost(final int hostUserId) {
        return reviewDao.averageRatingAboutHost(hostUserId);
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
