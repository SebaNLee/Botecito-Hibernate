package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.RequestStatus;
import ar.edu.itba.paw.models.RequestSubmission;
import ar.edu.itba.paw.services.MailService;
import ar.edu.itba.paw.services.RequestService;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class RequestActionController {

    private final MailService mailService;
    private final RequestService requestService;

    @Autowired
    public RequestActionController(final MailService mailService, final RequestService requestService) {
        this.mailService = mailService;
        this.requestService = requestService;
    }

    @RequestMapping(value = "/requests/{token}/accept", method = RequestMethod.GET)
    public ModelAndView acceptRequest(@PathVariable("token") final String token) {
        return resolveRequest(token, RequestStatus.ACCEPTED);
    }

    @RequestMapping(value = "/requests/{token}/decline", method = RequestMethod.GET)
    public ModelAndView declineRequest(@PathVariable("token") final String token) {
        return resolveRequest(token, RequestStatus.DECLINED);
    }

    private ModelAndView resolveRequest(final String token, final RequestStatus requestStatus) {
        final ModelAndView mav = new ModelAndView("request-action-result");
        final Optional<RequestSubmission> existingRequest = requestService.findByToken(token);
        if (existingRequest.isEmpty()) {
            mav.addObject("actionTitle", "Request not found");
            mav.addObject("actionMessage", "The request token is invalid or no longer available.");
            return mav;
        }

        final RequestSubmission requestSubmission = existingRequest.get();
        mav.addObject("itemId", requestSubmission.getItemId());
        if (requestSubmission.getStatus() != RequestStatus.PENDING) {
            mav.addObject("actionTitle", "Request already processed");
            mav.addObject(
                    "actionMessage",
                    "This request was already "
                            + requestSubmission.getStatus().name().toLowerCase() + ".");
            return mav;
        }

        final Optional<RequestSubmission> resolvedRequest = requestService.resolveRequest(token, requestStatus);
        if (resolvedRequest.isEmpty()) {
            mav.addObject("actionTitle", "Request could not be updated");
            mav.addObject("actionMessage", "Try again or verify that the request is still pending.");
            return mav;
        }

        mailService.sendRequestResolutionEmail(resolvedRequest.get());
        mav.addObject("actionTitle", "Request " + requestStatus.name().toLowerCase());
        mav.addObject(
                "actionMessage",
                "The requester was notified at " + resolvedRequest.get().getRequesterEmail() + ".");
        return mav;
    }
}
