package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.SelfBookingNotAllowedException;
import ar.edu.itba.paw.webapp.controller.support.ErrorPageSupport;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalMvcExceptionAdvice {

    private static final Logger LOG = Logger.getLogger(GlobalMvcExceptionAdvice.class.getName());

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFound(final NoHandlerFoundException exception, final HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        return ErrorPageSupport.modelAndView(HttpStatus.NOT_FOUND.value(), exception.getRequestURL());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ModelAndView handleMethodNotAllowed(
            final HttpRequestMethodNotSupportedException exception,
            final HttpServletResponse response,
            final HttpServletRequest request) {
        response.setStatus(HttpStatus.METHOD_NOT_ALLOWED.value());
        final String requestUri = request == null ? null : request.getRequestURI();
        if (requestUri != null && requestUri.contains("/publish")) {
            return new ModelAndView("redirect:/publish/availability?availabilityAction=invalidMethod");
        }
        return ErrorPageSupport.modelAndView(HttpStatus.METHOD_NOT_ALLOWED.value(), null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    public ModelAndView handleUnsupportedMediaType(
            final HttpMediaTypeNotSupportedException exception, final HttpServletResponse response) {
        response.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
        return ErrorPageSupport.modelAndView(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), null);
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
    public ModelAndView handleUnexpected(final Exception exception, final HttpServletResponse response) {
        LOG.log(Level.SEVERE, exception, () -> "Unhandled exception in MVC layer");
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return ErrorPageSupport.modelAndView(HttpStatus.INTERNAL_SERVER_ERROR.value(), null);
    }
}
