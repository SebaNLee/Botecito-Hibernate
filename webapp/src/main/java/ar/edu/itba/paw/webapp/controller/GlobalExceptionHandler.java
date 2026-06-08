package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.exceptions.BookingCollisionException;
import ar.edu.itba.paw.models.exceptions.EmailAlreadyExistsException;
import ar.edu.itba.paw.models.exceptions.ForbiddenOperationException;
import ar.edu.itba.paw.models.exceptions.IllegalBookingOperationException;
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
import ar.edu.itba.paw.webapp.util.UploadLimitRedirects;
import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.servlet.view.RedirectView;

/**
 * Maps controller-layer exceptions to either a shared HTTP error page
 * ({@code forward:/errors})
 * or a flash toast plus redirect for recoverable domain/validation failures.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ForbiddenOperationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleForbiddenOperation(final HttpServletRequest request) {
        return errorPage(request, HttpStatus.FORBIDDEN.value());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFound(final HttpServletRequest request) {
        return errorPage(request, HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler({ItemNotFoundException.class, UserNotFoundException.class, ReportNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleResourceNotFound(final HttpServletRequest request) {
        return errorPage(request, HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView handleResponseStatus(
            final HttpServletRequest request, final ResponseStatusException exception) {
        return errorPage(request, exception.getStatus().value());
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
        return errorPage(request, HttpStatus.METHOD_NOT_ALLOWED.value());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ModelAndView handleUnsupportedMediaType(final HttpServletRequest request) {
        return errorPage(request, HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleMaxUploadSize(final HttpServletRequest request) {
        LOGGER.debug("Max upload size exceeded");
        return UploadLimitRedirects.exceededUploadDecision(request)
                .map(UploadLimitRedirects::redirectWithToast)
                .orElseGet(() -> postRedirect("/errors?status=" + HttpStatus.PAYLOAD_TOO_LARGE.value()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleUnexpected(final HttpServletRequest request, final Exception exception) {
        LOGGER.error("Unhandled exception", exception);
        return errorPage(request, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @ExceptionHandler(NoAnticipationException.class)
    public ModelAndView handleNoAnticipation(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "requests.booking.noAnticipation");
        return UploadLimitRedirects.redirectToReferer(request);
    }

    @ExceptionHandler(OutsideAvailabilityException.class)
    public ModelAndView handleOutsideAvailability(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "detail.preBooking.outsideAvailability");
        return UploadLimitRedirects.redirectToReferer(request);
    }

    @ExceptionHandler(BookingCollisionException.class)
    public ModelAndView handleBookingCollision(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "detail.preBooking.collision");
        return UploadLimitRedirects.redirectToReferer(request);
    }

    @ExceptionHandler(SelfBlockCollisionException.class)
    public ModelAndView handleSelfBlockCollision(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "manageAvailability.msg.hasBookings");
        return UploadLimitRedirects.redirectToReferer(request);
    }

    @ExceptionHandler(IllegalBookingOperationException.class)
    public ModelAndView handleIllegalOperation(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "requests.booking.operationFailed");
        return UploadLimitRedirects.redirectToReferer(request);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ModelAndView handleEmailAlreadyExists(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "register.validation.email.duplicate");
        return UploadLimitRedirects.redirectToReferer(request);
    }

    @ExceptionHandler(ReportAlreadyExistsException.class)
    public ModelAndView handleReportAlreadyExists(final HttpServletRequest request, final RedirectAttributes ra) {
        ToastSupport.error(ra, "report.alreadyReported");
        return UploadLimitRedirects.redirectToReferer(request);
    }

    @ExceptionHandler(PastSlotException.class)
    public ModelAndView handlePastSlot(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "manageAvailability.msg.invalid");
        return UploadLimitRedirects.redirectToReferer(request);
    }

    @ExceptionHandler(InvalidSlotException.class)
    public ModelAndView handleInvalidSlot(HttpServletRequest request, RedirectAttributes ra) {
        ToastSupport.error(ra, "manageAvailability.msg.invalid");
        return UploadLimitRedirects.redirectToReferer(request);
    }

    @ExceptionHandler(InvalidPaymentProofException.class)
    public ModelAndView handleInvalidPaymentProof(final HttpServletRequest request, final RedirectAttributes ra) {
        ToastSupport.error(ra, "requests.booking.paymentProofRequired");
        return UploadLimitRedirects.redirectToReferer(request);
    }

    @ExceptionHandler(IOException.class)
    public ModelAndView handleIOException(final HttpServletRequest request, final RedirectAttributes ra) {
        final String requestUri = request == null ? null : request.getRequestURI();
        if (requestUri != null && requestUri.contains("/payment")) {
            return UploadLimitRedirects.redirectToRefererWithToast(request, "requests.booking.paymentInvalidFile");
        }
        LOGGER.error("Unhandled IOException");
        return errorPage(request, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    private static ModelAndView errorPage(final HttpServletRequest request, final int status) {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
        if (request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) == null) {
            request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, request.getRequestURI());
        }
        return new ModelAndView("forward:/errors");
    }

    private static ModelAndView postRedirect(final String target) {
        final RedirectView redirectView = new RedirectView(target);
        redirectView.setContextRelative(true);
        redirectView.setStatusCode(HttpStatus.SEE_OTHER);
        return new ModelAndView(redirectView);
    }
}
