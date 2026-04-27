package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.RatingSummary;
import ar.edu.itba.paw.models.Review;
import ar.edu.itba.paw.models.ReviewTargetType;
import ar.edu.itba.paw.persistence.ItemDao;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ItemDao itemDao;

    public ReviewServiceImpl(final ItemDao itemDao) {
        this.itemDao = itemDao;
    }

    @Override
    public Optional<Review> createReviewForBooking(
            final int bookingId, final int reviewerUserId, final int rating, final String comment) {
        if (rating < 1 || rating > 5) {
            return Optional.empty();
        }

        final Optional<ItemBooking> booking = itemDao.findBookingById(bookingId);
        if (booking.isEmpty()
                || booking.get().getItemId() == null
                || booking.get().getGuestId() == null) {
            return Optional.empty();
        }
        if (!isReviewWindowOpen(booking.get())) {
            return Optional.empty();
        }

        final Optional<Item> item = itemDao.findAnyItemById(booking.get().getItemId());
        if (item.isEmpty() || item.get().getOwnerId() == null) {
            return Optional.empty();
        }

        final ReviewDescriptor descriptor = resolveReviewDescriptor(booking.get(), item.get(), reviewerUserId);
        if (descriptor == null) {
            return Optional.empty();
        }

        return itemDao.createReview(
                bookingId,
                reviewerUserId,
                descriptor.revieweeUserId,
                descriptor.targetType,
                descriptor.targetId,
                rating,
                normalizeComment(comment));
    }

    @Override
    public boolean deleteReview(final int reviewId, final int reviewerUserId) {
        return itemDao.deleteReview(reviewId, reviewerUserId);
    }

    @Override
    public List<Review> listLatestItemReviews(final int itemId, final int limit) {
        return itemDao.listLatestReviewsByTarget(ReviewTargetType.ITEM, itemId, limit);
    }

    @Override
    public RatingSummary getItemRatingSummary(final int itemId) {
        return itemDao.ratingSummaryByTarget(ReviewTargetType.ITEM, itemId);
    }

    @Override
    public Map<Integer, RatingSummary> getItemRatingSummaries(final List<Integer> itemIds) {
        final Map<Integer, RatingSummary> summaries = new LinkedHashMap<>();
        if (itemIds == null || itemIds.isEmpty()) {
            return summaries;
        }

        final LinkedHashSet<Integer> uniqueItemIds = new LinkedHashSet<>(itemIds);
        for (final Integer itemId : uniqueItemIds) {
            if (itemId == null) {
                continue;
            }
            summaries.put(itemId, getItemRatingSummary(itemId));
        }
        return summaries;
    }

    @Override
    public List<PendingReviewAction> listPendingReviewActions(final int userId) {
        final List<PendingReviewAction> actions = new ArrayList<>();

        for (final ItemBooking booking : itemDao.listBookingsByGuestId(userId)) {
            final PendingReviewAction action = buildGuestPendingReviewAction(userId, booking);
            if (action != null) {
                actions.add(action);
            }
        }
        for (final ItemBooking booking : itemDao.listBookingsByOwnerId(userId)) {
            final PendingReviewAction action = buildOwnerPendingReviewAction(userId, booking);
            if (action != null) {
                actions.add(action);
            }
        }

        return actions;
    }

    @Override
    public Optional<PendingReviewAction> findPendingItemReviewAction(final int userId, final int itemId) {
        for (final ItemBooking booking : itemDao.listBookingsByGuestId(userId)) {
            if (booking.getItemId() == null || booking.getItemId() != itemId) {
                continue;
            }

            final PendingReviewAction action = buildGuestPendingReviewAction(userId, booking);
            if (action != null) {
                return Optional.of(action);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Review> listAuthoredReviews(final int reviewerUserId) {
        return itemDao.listReviewsByReviewer(reviewerUserId);
    }

    @Override
    public List<Review> listReceivedReviews(final int revieweeUserId) {
        return itemDao.listReviewsByReviewee(revieweeUserId);
    }

    private PendingReviewAction buildGuestPendingReviewAction(final int userId, final ItemBooking booking) {
        if (booking == null || booking.getGuestId() == null || booking.getGuestId() != userId) {
            return null;
        }
        if (!isReviewWindowOpen(booking) || booking.getItemId() == null) {
            return null;
        }

        final Optional<Item> item = itemDao.findAnyItemById(booking.getItemId());
        if (item.isEmpty() || item.get().getOwnerId() == null || item.get().getOwnerId() == userId) {
            return null;
        }

        if (booking.getId() == null
                || itemDao.findReviewByBookingReviewerAndTargetType(booking.getId(), userId, ReviewTargetType.ITEM)
                        .isPresent()) {
            return null;
        }

        return new PendingReviewAction(
                booking.getId(),
                booking.getItemId(),
                item.get().getOwnerId(),
                ReviewTargetType.ITEM,
                booking.getStartTime(),
                booking.getEndTime());
    }

    private PendingReviewAction buildOwnerPendingReviewAction(final int userId, final ItemBooking booking) {
        if (booking == null || booking.getItemId() == null || booking.getGuestId() == null || booking.getId() == null) {
            return null;
        }
        if (!isReviewWindowOpen(booking)) {
            return null;
        }

        final Optional<Item> item = itemDao.findAnyItemById(booking.getItemId());
        if (item.isEmpty()
                || item.get().getOwnerId() == null
                || item.get().getOwnerId() != userId
                || booking.getGuestId() == userId) {
            return null;
        }

        if (itemDao.findReviewByBookingReviewerAndTargetType(booking.getId(), userId, ReviewTargetType.USER)
                .isPresent()) {
            return null;
        }

        return new PendingReviewAction(
                booking.getId(),
                booking.getItemId(),
                booking.getGuestId(),
                ReviewTargetType.USER,
                booking.getStartTime(),
                booking.getEndTime());
    }

    private static ReviewDescriptor resolveReviewDescriptor(
            final ItemBooking booking, final Item item, final int reviewerUserId) {
        if (booking.getGuestId() != null && booking.getGuestId() == reviewerUserId) {
            if (item.getOwnerId() == null || item.getId() == null) {
                return null;
            }
            return new ReviewDescriptor(ReviewTargetType.ITEM, item.getId(), item.getOwnerId());
        }

        if (item.getOwnerId() != null && item.getOwnerId() == reviewerUserId && booking.getGuestId() != null) {
            return new ReviewDescriptor(ReviewTargetType.USER, booking.getGuestId(), booking.getGuestId());
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

    private static boolean isReviewWindowOpen(final ItemBooking booking) {
        if (booking == null || booking.getState() == null || booking.getEndTime() == null) {
            return false;
        }
        return (booking.getState() == BookingState.BOOKING_PAID || booking.getState() == BookingState.BOOKING_COMPLETED)
                && booking.getEndTime().isBefore(OffsetDateTime.now());
    }

    private record ReviewDescriptor(ReviewTargetType targetType, int targetId, int revieweeUserId) {}
}
