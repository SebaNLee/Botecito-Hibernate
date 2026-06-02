package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.AvailabilityData;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.Media;
import ar.edu.itba.paw.models.entity.Review;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.services.BookingService;
import ar.edu.itba.paw.services.DetailService;
import ar.edu.itba.paw.services.FavouriteService;
import ar.edu.itba.paw.services.ReportService;
import ar.edu.itba.paw.services.SubscriptionService;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.PreBookingForm;
import ar.edu.itba.paw.webapp.util.AvailabilityJsonHelper;
import ar.edu.itba.paw.webapp.util.DetailAvailabilityPicker;
import ar.edu.itba.paw.webapp.util.MarketplaceReturnUrl;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Builds the item detail view and handles pre-booking POST (PRG back to
 * {@code /item/{id}}).
 */
@Component
@RequiredArgsConstructor
public class DetailPresentation {

    private static final String MESSAGE_PREFIX = "detail";
    private static final String VIEW_NAME = "item-detail";
    private static final String IMAGE_PATH_PREFIX = "/image/";
    private static final String PLACEHOLDER_IMAGE_PATH = "/css/boat-placeholder.svg";
    private static final DateTimeFormatter REVIEW_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DetailService detailService;
    private final BookingService bookingService;
    private final SubscriptionService subscriptionService;
    private final FavouriteService favouriteService;
    private final ReportService reportService;
    private final ToastPresentation toastPresentation;

    /**
     * GET: load listing by item id (always uses latest version from the service).
     */
    public ModelAndView detailPage(
            final int itemId,
            final BotecitoUserDetails viewer,
            final HttpServletRequest request,
            final int reviewPage) {
        final String marketplaceBackHref = MarketplaceReturnUrl.marketplaceBackHref(request);
        final Item item = detailService.getItemDetail(itemId, reviewPage);
        return buildDetailView(item, viewer, request, marketplaceBackHref);
    }

    /**
     * POST validation failed: re-render detail with field toasts instead of
     * redirecting.
     */
    public ModelAndView detailPageWithPreBookingValidationErrors(
            final int itemId,
            final BotecitoUserDetails viewer,
            final HttpServletRequest request,
            final BindingResult errors,
            final int reviewPage) {
        final ModelAndView mav = detailPage(itemId, viewer, request, reviewPage);
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        return mav;
    }

    /**
     * POST success path: book against latest version, flash toast, redirect to
     * detail.
     */
    public ModelAndView submitPreBooking(
            final BotecitoUserDetails viewer,
            final int itemId,
            final PreBookingForm form,
            final HttpServletRequest request,
            final RedirectAttributes redirectAttributes) {
        if (viewer == null) {
            ToastSupport.error(redirectAttributes, MESSAGE_PREFIX + ".preBooking.loginRequired");
            return new ModelAndView("redirect:/login");
        }

        bookingService.createBooking(
                itemId, form.getDate(), form.getStartTime(), form.getEndTime(), form.getMessage(), viewer.getId());
        ToastSupport.success(redirectAttributes, MESSAGE_PREFIX + ".preBooking.success");
        return itemRedirect(itemId, request);
    }

    private ModelAndView buildDetailView(
            final Item item,
            final BotecitoUserDetails viewer,
            final HttpServletRequest request,
            final String marketplaceBackHref) {
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        final Version version = item.getLatestVersion();
        final Users itemOwner = item.getHost();
        final boolean isActive = item.getStatus() == ItemStatusEnum.ACTIVE;

        final boolean isOwner = viewer != null && itemOwner != null && itemOwner.getId() == viewer.getId();
        final boolean canFavouriteItem = itemOwner == null || viewer == null || itemOwner.getId() != viewer.getId();
        final boolean favouriteItem =
                canFavouriteItem && viewer != null && favouriteService.isFavourite(viewer.getId(), item.getId());
        final boolean canReport = viewer != null && isActive && !isOwner;
        final boolean alreadyReported = canReport && reportService.hasReported(viewer.getId(), item.getId());
        final boolean canSubscribeToOwner = itemOwner != null && !isOwner;
        final boolean subscribedToOwner = canSubscribeToOwner
                && viewer != null
                && subscriptionService.isSubscribed(viewer.getId(), itemOwner.getId());

        final ModelAndView mav = new ModelAndView(VIEW_NAME);
        mav.addObject("item", item);
        mav.addObject("version", version);
        mav.addObject("viewer", viewer);
        mav.addObject("listingInactiveNotice", !isActive);
        mav.addObject("itemOwner", itemOwner);
        mav.addObject("isOwner", isOwner);
        mav.addObject("canFavouriteItem", canFavouriteItem);
        mav.addObject("favouriteItem", favouriteItem);
        mav.addObject("canReport", canReport && !alreadyReported);
        mav.addObject("alreadyReported", alreadyReported);
        mav.addObject("canSubscribeToOwner", canSubscribeToOwner);
        mav.addObject("subscribedToOwner", subscribedToOwner);
        mav.addObject("detailReturnPath", currentRequestPath(request));
        mav.addObject("itemImageUrls", imageUrls(version, contextPath));
        mav.addObject("itemOwnerDisplayName", itemOwner != null ? ownerDisplayName(itemOwner) : "");
        mav.addObject("ownerInitials", itemOwner != null ? ownerInitials(itemOwner) : "");
        mav.addObject("itemLocationSlug", version.getLocation().getSlug());
        mav.addObject("marketplaceBackHref", marketplaceBackHref);
        mav.addObject("reviewPage", item.getReviewPage());
        mav.addObject("totalReviews", item.getTotalReviews());
        mav.addObject("averageRating", item.getAverageRating());
        mav.addObject("reviewDatesById", formatReviewDates(item.getReviewPage()));

        final PreBookingForm preBookingForm = new PreBookingForm();
        preBookingForm.setVersionId(version.getId());
        mav.addObject("preBookingForm", preBookingForm);

        addAvailabilityModel(mav, item, version);
        mav.addObject("showPreBookingPanel", isActive);

        return mav;
    }

    // Date/time picker JSON and calendar bounds; occupied slots use all item
    // bookings (every version).
    private void addAvailabilityModel(final ModelAndView mav, final Item item, final Version version) {
        final List<Availability> availabilityWindows =
                version.getAvailabilities() == null ? List.of() : version.getAvailabilities();
        final List<Booking> itemBookings = item.getBookings() == null ? List.of() : item.getBookings();
        final String listingTz = version.getTimezone();
        final AvailabilityData detailData =
                DetailAvailabilityPicker.build(availabilityWindows, itemBookings, listingTz);
        AvailabilityJsonHelper.addAvailabilityPickerData(mav, MESSAGE_PREFIX, detailData);
        mav.addObject("detailListingTimezoneId", listingTz == null || listingTz.isBlank() ? "" : listingTz.trim());
        mav.addObject(
                "detailListingTodayIso",
                DetailAvailabilityPicker.listingCalendarToday(listingTz).format(DateTimeFormatter.ISO_LOCAL_DATE));
        mav.addObject(
                "detailListingMaxDateIso",
                DetailAvailabilityPicker.listingCalendarMaxInclusive(listingTz)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    private static Map<Integer, String> formatReviewDates(final PageModel<Review> page) {
        final Map<Integer, String> dates = new LinkedHashMap<>();
        if (page == null || page.getContent() == null) {
            return dates;
        }
        for (final Review review : page.getContent()) {
            if (review == null || review.getId() == null || review.getCreatedAt() == null) {
                continue;
            }
            dates.put(review.getId(), review.getCreatedAt().format(REVIEW_DATE_FORMAT));
        }
        return dates;
    }

    private static String currentRequestPath(final HttpServletRequest request) {
        if (request == null) {
            return "/marketplace";
        }
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String path = request.getRequestURI() == null ? "/marketplace" : request.getRequestURI();
        if (!contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        final String query = request.getQueryString();
        return query == null || query.isBlank() ? path : path + "?" + query;
    }

    private static ModelAndView itemRedirect(final int itemId, final HttpServletRequest request) {
        final String returnTo = MarketplaceReturnUrl.relativeReturnTo(request.getParameter("returnTo"));
        if ("/marketplace".equals(returnTo)) {
            return new ModelAndView("redirect:/item/" + itemId);
        }
        return new ModelAndView(
                "redirect:/item/" + itemId + "?returnTo=" + URLEncoder.encode(returnTo, StandardCharsets.UTF_8));
    }

    private static List<String> imageUrls(final Version version, final String contextPath) {
        if (version.getMedia() == null || version.getMedia().isEmpty()) {
            return List.of(contextPath + PLACEHOLDER_IMAGE_PATH);
        }
        return version.getMedia().stream()
                .sorted(Comparator.comparingInt(m -> m.getId().getIndex()))
                .map(Media::getImage)
                .filter(Objects::nonNull)
                .map(image -> contextPath + IMAGE_PATH_PREFIX + image.getId())
                .toList();
    }

    private static String ownerDisplayName(final Users itemOwner) {
        // non-null, non-blank firstName and lastName.
        return itemOwner.getFirstName().trim() + " " + itemOwner.getLastName().trim();
    }

    private static String ownerInitials(final Users itemOwner) {
        // Uses first character of the owner's first name and first character of the
        // last name.
        return (itemOwner.getFirstName().trim().substring(0, 1)
                        + itemOwner.getLastName().trim().substring(0, 1))
                .toUpperCase();
    }
}
