package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.dto.AvailabilityData;
import ar.edu.itba.paw.models.dto.ItemDetailPageData;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.Media;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.webapp.auth.BotecitoUserDetails;
import ar.edu.itba.paw.webapp.form.ItemDetailViewForm;
import ar.edu.itba.paw.webapp.form.PreBookingForm;
import ar.edu.itba.paw.webapp.form.ReportForm;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public final class DetailPresentation {

    private static final String VIEW_NAME = "item-detail";
    private static final String IMAGE_PATH_PREFIX = "/image/";
    private static final String PLACEHOLDER_IMAGE_PATH = "/css/boat-placeholder.svg";
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private DetailPresentation() {}

    public static ModelAndView detailPage(
            final ItemDetailPageData pageData,
            final BotecitoUserDetails viewer,
            final DetailPageFlags flags,
            final HttpServletRequest request,
            final ItemDetailViewForm itemDetailView) {
        return buildDetailView(pageData, viewer, flags, request, itemDetailView, null, null, null, false);
    }

    public static ModelAndView detailPageWithViewValidationErrors(
            final ItemDetailPageData pageData,
            final BotecitoUserDetails viewer,
            final DetailPageFlags flags,
            final HttpServletRequest request,
            final ItemDetailViewForm itemDetailView,
            final BindingResult errors,
            final List<Map<String, String>> toasts) {
        final ModelAndView mav =
                buildDetailView(pageData, viewer, flags, request, itemDetailView, null, toasts, null, false);
        mav.addAllObjects(errors.getModel());
        return mav;
    }

    public static ModelAndView detailPageWithPreBookingValidationErrors(
            final ItemDetailPageData pageData,
            final BotecitoUserDetails viewer,
            final DetailPageFlags flags,
            final HttpServletRequest request,
            final List<Map<String, String>> toasts) {
        final ItemDetailViewForm itemDetailView = new ItemDetailViewForm();
        itemDetailView.setItemId(pageData.getItem().getId());
        itemDetailView.setPage(1);
        return buildDetailView(pageData, viewer, flags, request, itemDetailView, null, toasts, null, false);
    }

    public static ModelAndView detailPageWithReportValidationErrors(
            final ItemDetailPageData pageData,
            final BotecitoUserDetails viewer,
            final DetailPageFlags flags,
            final HttpServletRequest request,
            final ReportForm form,
            final List<Map<String, String>> toasts) {
        final ItemDetailViewForm itemDetailView = new ItemDetailViewForm();
        itemDetailView.setItemId(pageData.getItem().getId());
        if (pageData.getItem().getItemReviews() != null) {
            itemDetailView.setPage(pageData.getItem().getItemReviews().getPage());
        } else {
            itemDetailView.setPage(1);
        }
        return buildDetailView(pageData, viewer, flags, request, itemDetailView, form, toasts, null, true);
    }

    public static ModelAndView submitPreBookingSuccess(final int itemId, final RedirectAttributes redirectAttributes) {
        ToastSupport.success(redirectAttributes, "detail.preBooking.success");
        return new ModelAndView("redirect:/item/" + itemId);
    }

    private static ModelAndView buildDetailView(
            final ItemDetailPageData pageData,
            final BotecitoUserDetails viewer,
            final DetailPageFlags flags,
            final HttpServletRequest request,
            final ItemDetailViewForm itemDetailView,
            final ReportForm reportForm,
            final List<Map<String, String>> toasts,
            final BindingResult errors,
            final boolean openReportModal) {
        final Item item = pageData.getItem();
        final String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        final Version version = item.getLatestVersion();
        final Users itemOwner = item.getHost();
        final boolean isActive = item.getStatus() == ItemStatusEnum.ACTIVE;

        final boolean isOwner = viewer != null && itemOwner != null && itemOwner.getId() == viewer.getId();
        final boolean canFavouriteItem = itemOwner == null || viewer == null || itemOwner.getId() != viewer.getId();
        final boolean canReport = viewer != null && isActive && !isOwner;
        final boolean canSubscribeToOwner = itemOwner != null && !isOwner;

        final ModelAndView mav = new ModelAndView(VIEW_NAME);
        mav.addObject("item", item);
        mav.addObject("version", version);
        mav.addObject("viewer", viewer);
        mav.addObject("listingInactiveNotice", !isActive);
        mav.addObject("itemOwner", itemOwner);
        mav.addObject("isOwner", isOwner);
        mav.addObject("canFavouriteItem", canFavouriteItem);
        mav.addObject("favouriteItem", flags.favouriteItem());
        mav.addObject("canReport", canReport && !flags.alreadyReported());
        mav.addObject("alreadyReported", flags.alreadyReported());
        mav.addObject("canSubscribeToOwner", canSubscribeToOwner);
        mav.addObject("subscribedToOwner", flags.subscribedToOwner());
        mav.addObject("itemImageUrls", imageUrls(version, contextPath));
        mav.addObject("itemOwnerDisplayName", itemOwner != null ? ownerDisplayName(itemOwner) : "");
        mav.addObject("ownerInitials", itemOwner != null ? ownerInitials(itemOwner) : "");
        mav.addObject("itemLocationSlug", version.getLocation().getSlug());
        mav.addObject("itemReviews", item.getItemReviews());
        mav.addObject("itemDetailView", itemDetailView);

        final PreBookingForm preBookingForm = new PreBookingForm();
        preBookingForm.setVersionId(version.getId());
        mav.addObject("preBookingForm", preBookingForm);

        addAvailabilityModel(
                mav,
                pageData.getAvailabilityData(),
                version.getTimezone(),
                pageData.getListingCalendarToday(),
                pageData.getListingCalendarMaxInclusive());
        mav.addObject("showPreBookingPanel", isActive);

        if (reportForm != null) {
            mav.addObject("reportForm", reportForm);
        }
        if (openReportModal) {
            mav.addObject("openReportModal", true);
        }
        if (toasts != null) {
            mav.addObject("toasts", toasts);
        }
        if (errors != null) {
            mav.addAllObjects(errors.getModel());
        }
        return mav;
    }

    private static void addAvailabilityModel(
            final ModelAndView mav,
            final AvailabilityData detailData,
            final String listingTz,
            final LocalDate listingCalendarToday,
            final LocalDate listingCalendarMaxInclusive) {
        mav.addObject("detailOfferedDates", detailData.getOfferedDates());
        mav.addObject("detailOccupiedDates", detailData.getOccupiedDates());
        mav.addObject("detailOfferedTimesByDate", detailData.getOfferedTimesByDate());
        mav.addObject("detailOccupiedTimesByDate", detailData.getOccupiedTimesByDate());
        mav.addObject("detailListingTimezoneId", listingTz == null || listingTz.isBlank() ? "" : listingTz.trim());
        mav.addObject("detailListingTodayIso", listingCalendarToday.format(ISO_DATE));
        mav.addObject("detailListingMaxDateIso", listingCalendarMaxInclusive.format(ISO_DATE));
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
        return itemOwner.getFirstName().trim() + " " + itemOwner.getLastName().trim();
    }

    private static String ownerInitials(final Users itemOwner) {
        return (itemOwner.getFirstName().trim().substring(0, 1)
                        + itemOwner.getLastName().trim().substring(0, 1))
                .toUpperCase();
    }
}
