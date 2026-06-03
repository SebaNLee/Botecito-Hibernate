package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.exceptions.BookingCollisionException;
import ar.edu.itba.paw.models.exceptions.EmailAlreadyExistsException;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.models.exceptions.IllegalBookingOperationException;
import ar.edu.itba.paw.models.exceptions.InvalidBookingStatusException;
import ar.edu.itba.paw.models.exceptions.InvalidDateFormatException;
import ar.edu.itba.paw.models.exceptions.InvalidPaymentProofException;
import ar.edu.itba.paw.models.exceptions.InvalidSlotException;
import ar.edu.itba.paw.models.exceptions.ItemNotFoundException;
import ar.edu.itba.paw.models.exceptions.NoAnticipationException;
import ar.edu.itba.paw.models.exceptions.OutsideAvailabilityException;
import ar.edu.itba.paw.models.exceptions.PastSlotException;
import ar.edu.itba.paw.models.exceptions.ReportAlreadyExistsException;
import ar.edu.itba.paw.models.exceptions.ReportNotFoundException;
import ar.edu.itba.paw.models.exceptions.SelfBlockCollisionException;
import ar.edu.itba.paw.models.exceptions.UserNotFoundException;
import ar.edu.itba.paw.webapp.util.ToastSupport;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final Pattern EDIT_IMAGES_URI = Pattern.compile("/edit/(\\d+)/images");

    @ExceptionHandler(ForbiddenOperationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleForbiddenOperation(final HttpServletRequest request) {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 403);
        return new ModelAndView("forward:/errors");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFound(final HttpServletRequest request) {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        return new ModelAndView("forward:/errors");
    }

    @ExceptionHandler(ItemNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleItemNotFound(final HttpServletRequest request) {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        return new ModelAndView("forward:/errors");
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleUserNotFound(final HttpServletRequest request) {
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
            if (requestUri.contains("/publish/images")) {
                return new ModelAndView("redirect:/publish/images?availabilityAction=invalidMethod");
            }
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
    public ModelAndView handleMaxUploadSize(
            final HttpServletRequest request, final RedirectAttributes redirectAttributes) {
        LOGGER.debug("Max upload size exceeded");
        final String requestUri = request == null ? null : request.getRequestURI();
        if (requestUri != null && requestUri.contains("/publish/images")) {
            ToastSupport.error(redirectAttributes, "publish.validation.images.size");
            return new ModelAndView("redirect:/publish/images");
        }
        if (requestUri != null) {
            final Matcher editImages = EDIT_IMAGES_URI.matcher(requestUri);
            if (editImages.find()) {
                ToastSupport.error(redirectAttributes, "publish.validation.images.size");
                return new ModelAndView("redirect:/edit/" + editImages.group(1) + "/images");
            }
        }
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 413);
        return new ModelAndView("forward:/errors");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleAccessDenied(final HttpServletRequest request) {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 403);
        return new ModelAndView("forward:/errors");
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

    @ExceptionHandler(SelfBlockCollisionException.class)
    public ModelAndView handleSelfBlockCollision(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "manageAvailability.msg.hasBookings");
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

    @ExceptionHandler(ReportAlreadyExistsException.class)
    public ModelAndView handleReportAlreadyExists(final HttpServletRequest request, final RedirectAttributes ra) {
        ToastSupport.error(ra, "report.alreadyReported");
        return redirectToReferer(request);
    }

    @ExceptionHandler(ReportNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleReportNotFound(final HttpServletRequest request) {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        return new ModelAndView("forward:/errors");
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

    @ExceptionHandler(InvalidPaymentProofException.class)
    public ModelAndView handleInvalidPaymentProof(final HttpServletRequest request, final RedirectAttributes ra) {
        ToastSupport.error(ra, "requests.booking.paymentProofRequired");
        return redirectToReferer(request);
    }

    @ExceptionHandler(IOException.class)
    public ModelAndView handleIOException(final HttpServletRequest request, final RedirectAttributes ra) {
        final String requestUri = request == null ? null : request.getRequestURI();
        if (requestUri != null && requestUri.contains("/payment")) {
            ToastSupport.error(ra, "requests.booking.paymentInvalidFile");
            return redirectToReferer(request);
        }
        LOGGER.error("Unhandled IOException");
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        return new ModelAndView("forward:/errors");
    }

    private static ModelAndView redirectToReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        return new ModelAndView("redirect:" + (referer != null ? referer : "/"));
    }
}
