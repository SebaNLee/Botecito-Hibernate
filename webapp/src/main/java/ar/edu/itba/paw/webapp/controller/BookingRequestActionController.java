package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.MailService;
import ar.edu.itba.paw.services.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class BookingRequestActionController {

    private final BookingRequestService bookingRequestService;
    private final ItemService itemService;
    private final MailService mailService;
    private final UserService userService;

    public BookingRequestActionController(
            final BookingRequestService bookingRequestService,
            final ItemService itemService,
            final MailService mailService,
            final UserService userService) {
        this.bookingRequestService = bookingRequestService;
        this.itemService = itemService;
        this.mailService = mailService;
        this.userService = userService;
    }

    @RequestMapping(value = "/bookings/{token}/accept", method = RequestMethod.GET)
    public ModelAndView acceptBookingRequest(@PathVariable("token") final String token) {
        return new ModelAndView("redirect:/register?legacyToken=true");
    }

    @RequestMapping(value = "/bookings/{token}/decline", method = RequestMethod.GET)
    public ModelAndView declineBookingRequest(@PathVariable("token") final String token) {
        return new ModelAndView("redirect:/register?legacyToken=true");
    }

    @RequestMapping(value = "/bookings/{id:[0-9]+}/accept", method = RequestMethod.POST)
    public ModelAndView acceptBookingRequestInAccount(@PathVariable("id") final int bookingId) {
        return resolveBookingRequestInAccount(bookingId, BookingState.BOOKING_CONFIRMED);
    }

    @RequestMapping(value = "/bookings/{id:[0-9]+}/decline", method = RequestMethod.POST)
    public ModelAndView declineBookingRequestInAccount(@PathVariable("id") final int bookingId) {
        return resolveBookingRequestInAccount(bookingId, BookingState.BOOKING_REJECTED);
    }

    private ModelAndView resolveBookingRequestInAccount(final int bookingId, final BookingState bookingState) {
        final User currentUser = currentAuthenticatedUser();
        if (currentUser == null) {
            return new ModelAndView("redirect:/login");
        }

        final var booking = itemService.listBookings().stream()
                .filter(existingBooking -> existingBooking.getId() != null && existingBooking.getId() == bookingId)
                .findFirst()
                .orElse(null);
        if (booking == null || booking.getHostDecisionToken() == null) {
            return new ModelAndView("redirect:/profile?bookingAction=notFound");
        }

        final var item = itemService.findItemById(booking.getItemId()).orElse(null);
        if (item == null || item.getOwnerId() == null || !item.getOwnerId().equals(currentUser.getId())) {
            return new ModelAndView("redirect:/profile?bookingAction=forbidden");
        }

        final var resolved = bookingRequestService.resolveBookingRequest(booking.getHostDecisionToken(), bookingState);
        if (resolved.isEmpty()) {
            return new ModelAndView("redirect:/profile?bookingAction=error");
        }

        mailService.sendBookingResolutionEmail(resolved.get());
        final String action = bookingState == BookingState.BOOKING_CONFIRMED ? "accepted" : "rejected";
        return new ModelAndView("redirect:/profile?bookingAction=" + action);
    }

    private User currentAuthenticatedUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userService.findByEmail(authentication.getName()).orElse(null);
    }
}
