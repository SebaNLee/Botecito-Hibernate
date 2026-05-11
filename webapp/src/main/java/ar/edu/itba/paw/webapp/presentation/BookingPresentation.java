package ar.edu.itba.paw.webapp.presentation;

import ar.edu.itba.paw.models.nuevo.Booking;
import ar.edu.itba.paw.models.nuevo.BookingSearchModel;
import ar.edu.itba.paw.models.nuevo.BookingSearchResult;
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

    public ModelAndView bookingsGet(final HttpServletRequest request, final BookingSearchForm search) {
        return currentGuestUserId()
                .map(guestId -> {
                    final BookingSearchModel model = toModel(search, guestId);
                    final BookingSearchResult result = bookingInterface.searchBookings(model);
                    final List<Booking> bookings = result.getBookings();
                    final ModelAndView mav = new ModelAndView("nuevo/my-bookings", "bookingSearch", search);
                    mav.addObject("bookings", bookings);
                    addListingModelObjects(mav, search, bookings, result.getTotalCount());
                    return mav;
                })
                .orElseGet(() -> new ModelAndView("redirect:/login"));
    }

    public ModelAndView bookingsErrors(
            final HttpServletRequest request, final BookingSearchForm search, final BindingResult errors) {
        return currentGuestUserId()
                .map(guestId -> {
                    final List<Booking> bookings = List.of();
                    final ModelAndView mav = new ModelAndView("nuevo/my-bookings", "bookingSearch", search);
                    mav.addAllObjects(errors.getModel());
                    mav.addObject("bookings", bookings);
                    addListingModelObjects(mav, search, bookings, 0L);
                    mav.addObject("toasts", toastPresentation.validationToasts(errors, MESSAGE_PREFIX));
                    return mav;
                })
                .orElseGet(() -> new ModelAndView("redirect:/login"));
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

    public BookingSearchModel toModel(final BookingSearchForm form, final int guestId) {
        final BookingSearchModel model = new BookingSearchModel();
        model.setGuestId(guestId);
        model.setSearchQuery(form.getSearchQuery());
        model.setDate(PresentationUtils.parseDate(form.getDate()));
        model.setStatus(parseStatus(form.getStatus()));
        model.setPage(form.getPage());
        model.setPageSize(form.getPageSize());
        model.setSortBy(form.getSortBy());
        return model;
    }

    private static BookingStatus parseStatus(final String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return BookingStatus.valueOf(raw.trim());
    }

    private Optional<Integer> currentGuestUserId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return userService.findByEmail(authentication.getName()).flatMap(u -> Optional.ofNullable(u.getId()));
    }
}
