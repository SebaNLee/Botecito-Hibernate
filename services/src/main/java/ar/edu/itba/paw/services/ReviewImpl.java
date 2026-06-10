package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.HostReviewsPage;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.dto.ReviewSummary;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.BookingStatusEnum;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.TargetEnum;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.paging.ReviewPaging;
import ar.edu.itba.paw.persistence.ReviewDao;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
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

        final Optional<Review> review = reviewDao.createReview(bookingId, reviewerUserId, resolved, rating, comment);
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
        if (guestId != reviewerUserId || ownerId == reviewerUserId) {
            return null;
        }
        if (explicit != null) {
            if (explicit == TargetEnum.ITEM || explicit == TargetEnum.USER) {
                return explicit;
            }
            return null;
        }
        return TargetEnum.ITEM;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, List<Review>> findReviewsByBookingIds(
            final int reviewerUserId, final Collection<Integer> bookingIds) {
        if (bookingIds == null || bookingIds.isEmpty()) {
            return Map.of();
        }
        return reviewDao.findReviewsBySenderAndBookingIds(reviewerUserId, bookingIds).stream()
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

    @Override
    @Transactional(readOnly = true)
    public void attachReviewSummaries(final List<Item> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        final var itemIds = items.stream().map(Item::getId).toList();
        final var summaryByItemId = reviewDao.reviewSummariesForItems(itemIds);
        for (final Item item : items) {
            item.setReviewSummary(summaryByItemId.getOrDefault(item.getId(), ReviewSummary.EMPTY));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void attachItemReviews(final Item item, final int itemId, final int page) {
        final int pageSize = ReviewPaging.DEFAULT_PAGE_SIZE;
        final var reviewSummary = reviewDao.reviewSummaryForItem(itemId);
        final var reviews = reviewDao.findReviewsAboutItem(itemId, page, pageSize);
        item.setReviewSummary(reviewSummary);
        item.setItemReviews(new PageModel<>(reviews, page, pageSize, reviewSummary.getTotalReviews()));
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
