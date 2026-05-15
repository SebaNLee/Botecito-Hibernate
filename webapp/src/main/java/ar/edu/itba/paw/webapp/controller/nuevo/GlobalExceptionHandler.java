package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.models.nuevo.exceptions.BookingCollisionException;
import ar.edu.itba.paw.models.nuevo.exceptions.IllegalBookingOperationException;
import ar.edu.itba.paw.models.nuevo.exceptions.NoAnticipationException;
import ar.edu.itba.paw.models.nuevo.exceptions.OutsideAvailabilityException;
import ar.edu.itba.paw.services.SelfBookingNotAllowedException;
import ar.edu.itba.paw.webapp.controller.support.ToastSupport;
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

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFound(final HttpServletRequest request) {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        return new ModelAndView("forward:/404");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ModelAndView handleResponseStatus(final ResponseStatusException exception) {
        return new ModelAndView("forward:/" + exception.getStatus().value());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ModelAndView handleMethodNotAllowed(final HttpServletRequest request) {
        final String requestUri = request == null ? null : request.getRequestURI();
        if (requestUri != null && requestUri.contains("/publish")) {
            return new ModelAndView("redirect:/publish/availability?availabilityAction=invalidMethod");
        }
        return new ModelAndView("forward:/400");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ModelAndView handleUnsupportedMediaType() {
        return new ModelAndView("forward:/400");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleMaxUploadSize(final HttpServletRequest request) {
        LOGGER.debug("Max upload size exceeded");
        request.setAttribute("maxUploadSizeExceeded", true);
        return new ModelAndView("forward:/400");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied() {
        return new ModelAndView("redirect:/403");
    }

    @ExceptionHandler(SelfBookingNotAllowedException.class)
    public ModelAndView handleSelfBookingNotAllowed(final HttpServletRequest request) {
        return new ModelAndView("redirect:" + request.getRequestURI() + "?error=selfBooking");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleUnexpected(final Exception exception) {
        LOGGER.error("Unhandled exception", exception);
        return new ModelAndView("forward:/500");
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

    private static ModelAndView redirectToReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        return new ModelAndView("redirect:" + (referer != null ? referer : "/"));
    }
}
