package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.DashboardReviewView;
import ar.edu.itba.paw.models.nuevo.ItemReviewsView;
import ar.edu.itba.paw.models.nuevo.PendingReviewActionModel;
import ar.edu.itba.paw.models.nuevo.RatingSummaryModel;
import ar.edu.itba.paw.models.nuevo.ReviewModel;
import ar.edu.itba.paw.models.nuevo.enums.BookingStatus;
import ar.edu.itba.paw.models.nuevo.enums.TargetType;
import ar.edu.itba.paw.persistence.nuevo.BookingDao;
import ar.edu.itba.paw.persistence.nuevo.ReviewDao;
import ar.edu.itba.paw.persistence.nuevo.UserDao;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public final class ReviewImpl implements ReviewInterface {

    private final BookingDao bookingDao;
    private final ReviewDao reviewDao;
    private final UserDao userDao;

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
    public boolean deleteReview(final int reviewId, final int reviewerUserId) {
        return reviewDao.deleteReview(reviewId, reviewerUserId);
    }

    @Override
    public List<ReviewModel> listLatestItemReviews(final int itemId, final int limit) {
        return reviewDao.listLatestReviewsForItem(itemId, limit);
    }

    @Override
    public RatingSummaryModel getItemRatingSummary(final int itemId) {
        return reviewDao.ratingSummaryForItem(itemId);
    }

    @Override
    public List<PendingReviewActionModel> listPendingReviewActions(final int userId) {
        final List<PendingReviewActionModel> actions = new ArrayList<>();
        for (final Booking booking : bookingDao.listBookingsByGuestId(userId)) {
            final PendingReviewActionModel action = buildGuestPendingReviewAction(userId, booking);
            if (action != null) {
                actions.add(action);
            }
        }
        for (final Booking booking : bookingDao.listBookingsByOwnerId(userId)) {
            final PendingReviewActionModel action = buildOwnerPendingReviewAction(userId, booking);
            if (action != null) {
                actions.add(action);
            }
        }
        return actions;
    }

    @Override
    public Optional<PendingReviewActionModel> findPendingItemReviewAction(final int userId, final int itemId) {
        for (final Booking booking : bookingDao.listBookingsByGuestId(userId)) {
            if (bookingDao.findItemIdForBookingId(booking.getId()).orElse(-1) != itemId) {
                continue;
            }
            final PendingReviewActionModel action = buildGuestPendingReviewAction(userId, booking);
            if (action != null) {
                return Optional.of(action);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<ReviewModel> listAuthoredReviews(final int reviewerUserId) {
        return reviewDao.listReviewsBySender(reviewerUserId);
    }

    @Override
    public List<ReviewModel> listReceivedReviews(final int revieweeUserId) {
        return reviewDao.listReviewsForUser(revieweeUserId);
    }

    @Override
    public ItemReviewsView getItemReviewsView(final int itemId, final int limit, final Integer viewerUserId) {
        final RatingSummaryModel ratingSummary = reviewDao.ratingSummaryForItem(itemId);
        final List<ReviewModel> reviews = reviewDao.listLatestReviewsForItem(itemId, limit);
        final Map<Integer, String> authorNamesBySenderId = new LinkedHashMap<>();
        for (final ReviewModel review : reviews) {
            final int senderId = review.getSenderId();
            if (senderId <= 0 || authorNamesBySenderId.containsKey(senderId)) {
                continue;
            }
            authorNamesBySenderId.put(
                    senderId,
                    userDao.findById(senderId)
                            .map(u -> u.getName() == null ? "" : u.getName())
                            .orElse(""));
        }
        final PendingReviewActionModel pendingViewerAction = viewerUserId == null
                ? null
                : findPendingItemReviewAction(viewerUserId, itemId).orElse(null);
        return new ItemReviewsView(ratingSummary, reviews, authorNamesBySenderId, pendingViewerAction);
    }

    @Override
    public DashboardReviewView getDashboardReviewView(final int userId) {
        final Map<Integer, PendingReviewActionModel> pendingItemReviewsByBookingId = new LinkedHashMap<>();
        final Map<Integer, PendingReviewActionModel> pendingUserReviewsByBookingId = new LinkedHashMap<>();
        for (final PendingReviewActionModel action : listPendingReviewActions(userId)) {
            if (action.getTargetType() == TargetType.ITEM) {
                pendingItemReviewsByBookingId.put(action.getBookingId(), action);
            } else if (action.getTargetType() == TargetType.USER) {
                pendingUserReviewsByBookingId.put(action.getBookingId(), action);
            }
        }

        final Map<Integer, ReviewModel> authoredItemReviewsByBookingId = new LinkedHashMap<>();
        final Map<Integer, ReviewModel> authoredUserReviewsByBookingId = new LinkedHashMap<>();
        for (final ReviewModel review : listAuthoredReviews(userId)) {
            if (review.getBookingId() <= 0) {
                continue;
            }
            if (review.getTargetType() == TargetType.ITEM) {
                authoredItemReviewsByBookingId.putIfAbsent(review.getBookingId(), review);
            } else if (review.getTargetType() == TargetType.USER) {
                authoredUserReviewsByBookingId.putIfAbsent(review.getBookingId(), review);
            }
        }

        return new DashboardReviewView(
                pendingItemReviewsByBookingId,
                pendingUserReviewsByBookingId,
                authoredItemReviewsByBookingId,
                authoredUserReviewsByBookingId);
    }

    private PendingReviewActionModel buildGuestPendingReviewAction(final int userId, final Booking booking) {
        if (booking == null || booking.getGuestId() != userId || !isReviewWindowOpen(booking)) {
            return null;
        }
        final Optional<Integer> itemId = bookingDao.findItemIdForBookingId(booking.getId());
        final Optional<Integer> ownerId = bookingDao.findOwnerIdForBookingId(booking.getId());
        if (itemId.isEmpty() || ownerId.isEmpty() || ownerId.get() == userId) {
            return null;
        }
        if (reviewDao
                .findReviewByBookingSenderAndTargetType(booking.getId(), userId, TargetType.ITEM)
                .isPresent()) {
            return null;
        }
        final TargetDisplay target = resolveTargetDisplay(ownerId.get());
        return new PendingReviewActionModel(
                booking.getId(),
                itemId.get(),
                ownerId.get(),
                TargetType.ITEM,
                booking.getStart(),
                booking.getEnd(),
                target.name,
                target.email);
    }

    private PendingReviewActionModel buildOwnerPendingReviewAction(final int userId, final Booking booking) {
        if (booking == null || !isReviewWindowOpen(booking) || booking.getGuestId() == userId) {
            return null;
        }
        final Optional<Integer> itemId = bookingDao.findItemIdForBookingId(booking.getId());
        final Optional<Integer> ownerId = bookingDao.findOwnerIdForBookingId(booking.getId());
        if (itemId.isEmpty() || ownerId.isEmpty() || ownerId.get() != userId) {
            return null;
        }
        if (reviewDao
                .findReviewByBookingSenderAndTargetType(booking.getId(), userId, TargetType.USER)
                .isPresent()) {
            return null;
        }
        final int guestId = booking.getGuestId();
        final TargetDisplay target = resolveTargetDisplay(guestId);
        return new PendingReviewActionModel(
                booking.getId(),
                itemId.get(),
                guestId,
                TargetType.USER,
                booking.getStart(),
                booking.getEnd(),
                target.name,
                target.email);
    }

    private TargetDisplay resolveTargetDisplay(final int userId) {
        return userDao.findById(userId)
                .map(u -> new TargetDisplay(
                        u.getName() == null ? "" : u.getName(), u.getEmail() == null ? "" : u.getEmail()))
                .orElseGet(() -> new TargetDisplay("", ""));
    }

    private record TargetDisplay(String name, String email) {}

    private static ReviewDescriptor resolveReviewDescriptor(
            final Booking booking, final int ownerId, final int reviewerUserId) {
        if (booking.getGuestId() == reviewerUserId && ownerId != reviewerUserId) {
            return new ReviewDescriptor(TargetType.ITEM, ownerId);
        }
        if (ownerId == reviewerUserId && booking.getGuestId() != reviewerUserId) {
            return new ReviewDescriptor(TargetType.USER, booking.getGuestId());
        }
        return null;
    }

    private record ReviewDescriptor(TargetType targetType, int revieweeUserId) {}

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
