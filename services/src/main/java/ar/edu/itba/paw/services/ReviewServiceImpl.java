package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.Item;
import ar.edu.itba.paw.models.ItemBooking;
import ar.edu.itba.paw.models.RatingSummary;
import ar.edu.itba.paw.models.Review;
import ar.edu.itba.paw.models.ReviewTargetType;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.persistence.ItemBookingDao;
import ar.edu.itba.paw.persistence.ItemDao;
import ar.edu.itba.paw.persistence.ReviewDao;
import ar.edu.itba.paw.persistence.UserDao;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

// Legacy review service. Unwired from Spring (no @Service); the new ReviewImpl in services.nuevo is the active path.
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ItemDao itemDao;
    private final ItemBookingDao itemBookingDao;
    private final ReviewDao reviewDao;
    private final UserDao userDao;

    @Override
    public Optional<Review> createReviewForBooking(
            final int bookingId, final int reviewerUserId, final int rating, final String comment) {
        if (rating < 1 || rating > 5) {
            return Optional.empty();
        }

        final Optional<ItemBooking> booking = itemBookingDao.findBookingById(bookingId);
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

        return reviewDao.createReview(
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
        return reviewDao.deleteReview(reviewId, reviewerUserId);
    }

    @Override
    public List<Review> listLatestItemReviews(final int itemId, final int limit) {
        return reviewDao.listLatestReviewsByTarget(ReviewTargetType.ITEM, itemId, limit);
    }

    @Override
    public RatingSummary getItemRatingSummary(final int itemId) {
        return reviewDao.ratingSummaryByTarget(ReviewTargetType.ITEM, itemId);
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

        for (final ItemBooking booking : itemBookingDao.listBookingsByGuestId(userId)) {
            final PendingReviewAction action = buildGuestPendingReviewAction(userId, booking);
            if (action != null) {
                actions.add(action);
            }
        }
        for (final ItemBooking booking : itemBookingDao.listBookingsByOwnerId(userId)) {
            final PendingReviewAction action = buildOwnerPendingReviewAction(userId, booking);
            if (action != null) {
                actions.add(action);
            }
        }

        return actions;
    }

    @Override
    public Optional<PendingReviewAction> findPendingItemReviewAction(final int userId, final int itemId) {
        for (final ItemBooking booking : itemBookingDao.listBookingsByGuestId(userId)) {
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
        return reviewDao.listReviewsByReviewer(reviewerUserId);
    }

    @Override
    public List<Review> listReceivedReviews(final int revieweeUserId) {
        return reviewDao.listReviewsByReviewee(revieweeUserId);
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
                || reviewDao
                        .findReviewByBookingReviewerAndTargetType(booking.getId(), userId, ReviewTargetType.ITEM)
                        .isPresent()) {
            return null;
        }

        final int ownerId = item.get().getOwnerId();
        final TargetDisplay target = resolveTargetDisplay(ownerId);
        return new PendingReviewAction(
                booking.getId(),
                booking.getItemId(),
                ownerId,
                ReviewTargetType.ITEM,
                booking.getStartTime(),
                booking.getEndTime(),
                target.name(),
                target.email());
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

        if (reviewDao
                .findReviewByBookingReviewerAndTargetType(booking.getId(), userId, ReviewTargetType.USER)
                .isPresent()) {
            return null;
        }

        final int guestId = booking.getGuestId();
        final TargetDisplay target = resolveTargetDisplay(guestId);
        return new PendingReviewAction(
                booking.getId(),
                booking.getItemId(),
                guestId,
                ReviewTargetType.USER,
                booking.getStartTime(),
                booking.getEndTime(),
                target.name(),
                target.email());
    }

    private TargetDisplay resolveTargetDisplay(final int userId) {
        final Optional<User> user = userDao.findById(userId);
        if (user.isEmpty()) {
            return new TargetDisplay("", "");
        }
        final String name = user.get().getName() == null ? "" : user.get().getName();
        final String email = user.get().getEmail() == null ? "" : user.get().getEmail();
        return new TargetDisplay(name, email);
    }

    private record TargetDisplay(String name, String email) {}

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
        return isBookingEligibleForPostStayReview(booking.getState())
                && booking.getEndTime().isBefore(OffsetDateTime.now());
    }

    /**
     * Reviews are allowed after the booking end time for stays that were accepted and not cancelled or rejected.
     */
    private static boolean isBookingEligibleForPostStayReview(final BookingState state) {
        return switch (state) {
            case BOOKING_CONFIRMED, BOOKING_PAYMENT_SUBMITTED, BOOKING_PAID, BOOKING_COMPLETED -> true;
            default -> false;
        };
    }

    private record ReviewDescriptor(ReviewTargetType targetType, int targetId, int revieweeUserId) {}
}
