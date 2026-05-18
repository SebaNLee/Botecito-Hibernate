package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.exceptions.BookingCollisionException;
import ar.edu.itba.paw.models.exceptions.EmailAlreadyExistsException;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.models.exceptions.IllegalBookingOperationException;
import ar.edu.itba.paw.models.exceptions.InvalidBookingStatusException;
import ar.edu.itba.paw.models.exceptions.InvalidDateFormatException;
import ar.edu.itba.paw.models.exceptions.InvalidSlotException;
import ar.edu.itba.paw.models.exceptions.NoAnticipationException;
import ar.edu.itba.paw.models.exceptions.OutsideAvailabilityException;
import ar.edu.itba.paw.models.exceptions.PastSlotException;
import ar.edu.itba.paw.models.exceptions.SlotOverlapException;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ForbiddenOperationException.class)
    public ModelAndView handleForbiddenOperation() {
        return new ModelAndView("redirect:/errors?status=403");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFound(final HttpServletRequest request) {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        return new ModelAndView("forward:/errors");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView handleResponseStatus(
            final HttpServletRequest request, final ResponseStatusException exception) {
        request.setAttribute(
                RequestDispatcher.ERROR_STATUS_CODE, exception.getStatus().value());
        return new ModelAndView("forward:/errors");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ModelAndView handleMethodNotAllowed(final HttpServletRequest request) {
        final String requestUri = request == null ? null : request.getRequestURI();
        if (requestUri != null && requestUri.contains("/publish")) {
            return new ModelAndView("redirect:/publish/availability?availabilityAction=invalidMethod");
        }
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 405);
        return new ModelAndView("forward:/errors");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ModelAndView handleUnsupportedMediaType(final HttpServletRequest request) {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 415);
        return new ModelAndView("forward:/errors");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ModelAndView handleMaxUploadSize(final HttpServletRequest request) {
        LOGGER.debug("Max upload size exceeded");
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 413);
        return new ModelAndView("forward:/errors");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied() {
        return new ModelAndView("redirect:/errors?status=403");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleUnexpected(final HttpServletRequest request, final Exception exception) {
        LOGGER.error("Unhandled exception", exception);
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        return new ModelAndView("forward:/errors");
    }

    @ExceptionHandler(NoAnticipationException.class)
    public ModelAndView handleNoAnticipation(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "requests.booking.noAnticipation");
        return redirectToReferer(request);
    }

    @ExceptionHandler(OutsideAvailabilityException.class)
    public ModelAndView handleOutsideAvailability(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "detail.preBooking.outsideAvailability");
        return redirectToReferer(request);
    }

    @ExceptionHandler(BookingCollisionException.class)
    public ModelAndView handleBookingCollision(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "detail.preBooking.collision");
        return redirectToReferer(request);
    }

    @ExceptionHandler(IllegalBookingOperationException.class)
    public ModelAndView handleIllegalOperation(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "requests.booking.operationFailed");
        return redirectToReferer(request);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ModelAndView handleEmailAlreadyExists(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "register.validation.email.duplicate");
        return redirectToReferer(request);
    }

    @ExceptionHandler(InvalidDateFormatException.class)
    public ModelAndView handleInvalidDateFormat(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "bookingSearch.validation.date.invalid");
        return redirectToReferer(request);
    }

    @ExceptionHandler(InvalidBookingStatusException.class)
    public ModelAndView handleInvalidBookingStatus(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "bookingSearch.validation.status.invalid");
        return redirectToReferer(request);
    }

    @ExceptionHandler(PastSlotException.class)
    public ModelAndView handlePastSlot(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "manageAvailability.msg.invalid");
        return redirectToReferer(request);
    }

    @ExceptionHandler(InvalidSlotException.class)
    public ModelAndView handleInvalidSlot(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "manageAvailability.msg.invalid");
        return redirectToReferer(request);
    }

    @ExceptionHandler(SlotOverlapException.class)
    public ModelAndView handleSlotOverlap(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "manageAvailability.msg.hasBookings");
        return redirectToReferer(request);
    }

    private static ModelAndView redirectToReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        return new ModelAndView("redirect:" + (referer != null ? referer : "/"));
    }
}
