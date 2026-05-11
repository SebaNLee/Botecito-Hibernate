package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.BookingSearchModel;
import ar.edu.itba.paw.models.nuevo.BookingSearchResult;
import ar.edu.itba.paw.models.nuevo.IncomingSearch;
import ar.edu.itba.paw.models.nuevo.OutcomingSearch;
import ar.edu.itba.paw.models.nuevo.enums.BookingStatus;
import ar.edu.itba.paw.services.Page;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.services.nuevo.BookingInterface;
import ar.edu.itba.paw.webapp.form.nuevo.BookingSearchForm;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class BookingPresentation {

    private static final String MESSAGE_PREFIX = "bookingSearch";

    private final BookingInterface bookingInterface;
    private final UserService userService;
    private final ToastPresentation toastPresentation;

    public ModelAndView outgoingBookingsGet(final HttpServletRequest request, final BookingSearchForm search) {
        return currentUserId()
                .map(userId -> {
                    final OutcomingSearch outcoming = toOutcomingSearch(search, userId);
                    final BookingSearchResult result = bookingInterface.searchOutcomingBookings(outcoming);
                    final List<Booking> bookings = result.getBookings();
                    final ModelAndView mav = new ModelAndView("nuevo/requests-outgoing", "bookingSearch", search);
                    mav.addObject("bookings", bookings);
                    addListingModelObjects(mav, search, bookings, result.getTotalCount());
                    return mav;
                })
                .orElseGet(() -> new ModelAndView("redirect:/login"));
    }

    public ModelAndView outgoingBookingsErrors(
            final HttpServletRequest request, final BookingSearchForm search, final BindingResult errors) {
        if (currentUserId().isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        final List<Booking> bookings = List.of();
        final ModelAndView mav = new ModelAndView("nuevo/requests-outgoing", "bookingSearch", search);
        mav.addAllObjects(errors.getModel());
        mav.addObject("bookings", bookings);
        addListingModelObjects(mav, search, bookings, 0L);
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        return mav;
    }

    public ModelAndView incomingBookingsGet(final HttpServletRequest request, final BookingSearchForm search) {
        return currentUserId()
                .map(userId -> {
                    final IncomingSearch incoming = toIncomingSearch(search, userId);
                    final BookingSearchResult result = bookingInterface.searchIncomingBookings(incoming);
                    final List<Booking> bookings = result.getBookings();
                    final ModelAndView mav = new ModelAndView("nuevo/requests-incoming", "bookingSearch", search);
                    mav.addObject("bookings", bookings);
                    addListingModelObjects(mav, search, bookings, result.getTotalCount());
                    return mav;
                })
                .orElseGet(() -> new ModelAndView("redirect:/login"));
    }

    public ModelAndView incomingBookingsErrors(
            final HttpServletRequest request, final BookingSearchForm search, final BindingResult errors) {
        if (currentUserId().isEmpty()) {
            return new ModelAndView("redirect:/login");
        }
        final List<Booking> bookings = List.of();
        final ModelAndView mav = new ModelAndView("nuevo/requests-incoming", "bookingSearch", search);
        mav.addObject("bookings", bookings);
        addListingModelObjects(mav, search, bookings, 0L);
        mav.addAllObjects(errors.getModel());
        mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
        return mav;
    }

    private void addListingModelObjects(
            final ModelAndView mav, final BookingSearchForm search, final List<Booking> bookings, final long total) {
        final int page = search.getPage() == null ? 1 : search.getPage();
        final int pageSize = search.getPageSize() == null ? 12 : search.getPageSize();
        final int totalItems = total > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) total;
        mav.addObject("bookingPage", new Page<>(bookings, page, pageSize, totalItems));
        mav.addObject("bookingsCount", totalItems);
        mav.addObject("sort", sortRequestValue(search.getSortBy()));
    }

    private static String sortRequestValue(final String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "newest";
        }
        return sortBy;
    }

    public IncomingSearch toIncomingSearch(final BookingSearchForm form, final int hostId) {
        final IncomingSearch incoming = new IncomingSearch();
        incoming.setHostId(hostId);
        final BookingSearchModel criteria = new BookingSearchModel();
        copyFormToCriteria(form, criteria);
        incoming.setSearch(criteria);
        return incoming;
    }

    public OutcomingSearch toOutcomingSearch(final BookingSearchForm form, final int guestId) {
        final OutcomingSearch outcoming = new OutcomingSearch();
        outcoming.setGuestId(guestId);
        final BookingSearchModel criteria = new BookingSearchModel();
        copyFormToCriteria(form, criteria);
        outcoming.setSearch(criteria);
        return outcoming;
    }

    private static void copyFormToCriteria(final BookingSearchForm form, final BookingSearchModel criteria) {
        criteria.setSearchQuery(form.getSearchQuery());
        criteria.setDate(PresentationUtils.parseDate(form.getDate()));
        criteria.setStatus(parseStatus(form.getStatus()));
        criteria.setPage(form.getPage());
        criteria.setPageSize(form.getPageSize());
        criteria.setSortBy(form.getSortBy());
    }

    private static BookingStatus parseStatus(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return BookingStatus.valueOf(raw.trim());
    }

    private Optional<Integer> currentUserId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return userService.findByEmail(authentication.getName()).flatMap(u -> Optional.ofNullable(u.getId()));
    }
}
