package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.BookingRequest;
import ar.edu.itba.paw.models.BookingState;
import ar.edu.itba.paw.services.BookingRequestService;
import ar.edu.itba.paw.services.MailService;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class BookingRequestActionController {

    private final MailService mailService;
    private final BookingRequestService bookingRequestService;

    @Autowired
    public BookingRequestActionController(
            final MailService mailService, final BookingRequestService bookingRequestService) {
        this.mailService = mailService;
        this.bookingRequestService = bookingRequestService;
    }

    @RequestMapping(value = "/bookings/{token}/accept", method = RequestMethod.GET)
    public ModelAndView acceptBookingRequest(@PathVariable("token") final String token) {
        return resolveBookingRequest(token, BookingState.BOOKING_CONFIRMED);
    }

    @RequestMapping(value = "/bookings/{token}/decline", method = RequestMethod.GET)
    public ModelAndView declineBookingRequest(@PathVariable("token") final String token) {
        return resolveBookingRequest(token, BookingState.BOOKING_REJECTED);
    }

    private ModelAndView resolveBookingRequest(final String token, final BookingState requestStatus) {
        final ModelAndView mav = new ModelAndView("booking-action-result");
        final Optional<BookingRequest> existingBookingRequest = bookingRequestService.findByToken(token);
        if (existingBookingRequest.isEmpty()) {
            mav.addObject("actionTitleCode", "bookingAction.notFound.title");
            mav.addObject("actionMessageCode", "bookingAction.notFound.message");
            return mav;
        }

        final BookingRequest bookingRequest = existingBookingRequest.get();
        mav.addObject("itemId", bookingRequest.getItemId());
        if (bookingRequest.getStatus() != BookingState.BOOKING_PENDING) {
            mav.addObject("actionTitleCode", "bookingAction.alreadyProcessed.title");
            mav.addObject("actionMessageCode", alreadyProcessedMessageCode(bookingRequest.getStatus()));
            return mav;
        }

        final Optional<BookingRequest> resolvedBookingRequest =
                bookingRequestService.resolveBookingRequest(token, requestStatus);
        if (resolvedBookingRequest.isEmpty()) {
            mav.addObject("actionTitleCode", "bookingAction.updateFailed.title");
            mav.addObject("actionMessageCode", "bookingAction.updateFailed.message");
            return mav;
        }

        mailService.sendBookingResolutionEmail(resolvedBookingRequest.get());
        mav.addObject("actionTitleCode", resolvedTitleCode(requestStatus));
        mav.addObject("actionMessageCode", "bookingAction.resolved.message");
        mav.addObject(
                "actionMessageArgs", new Object[] {resolvedBookingRequest.get().getRequesterEmail()});
        return mav;
    }

    private static String alreadyProcessedMessageCode(final BookingState bookingState) {
        return switch (bookingState) {
            case BOOKING_CONFIRMED -> "bookingAction.alreadyProcessed.confirmed";
            case BOOKING_REJECTED -> "bookingAction.alreadyProcessed.rejected";
            default -> "bookingAction.alreadyProcessed.message";
        };
    }

    private static String resolvedTitleCode(final BookingState bookingState) {
        return switch (bookingState) {
            case BOOKING_CONFIRMED -> "bookingAction.resolved.confirmed";
            case BOOKING_REJECTED -> "bookingAction.resolved.rejected";
            default -> "bookingAction.pageTitle";
        };
    }
}
