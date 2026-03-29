package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ClassUser;
import ar.edu.itba.paw.models.RequestStatus;
import ar.edu.itba.paw.models.RequestSubmission;
import ar.edu.itba.paw.services.ClassUserService;
import ar.edu.itba.paw.services.MailService;
import ar.edu.itba.paw.services.RequestService;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HelloWorldController {

    private final MailService mailService;
    private final RequestService requestService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView landing() {
        return new ModelAndView("index");
    }

    @RequestMapping(value = "/marketplace", method = RequestMethod.GET)
    public ModelAndView marketplace() {
        return new ModelAndView("marketplace");
    }

    @RequestMapping(value = "/test-mail", method = RequestMethod.GET)
    public ModelAndView testMail() {
        return new ModelAndView("test-mail");
    }

    @RequestMapping(value = "/test-mail", method = RequestMethod.POST)
    public ModelAndView sendTestMail(
            @RequestParam("requesterName") final String requesterName,
            @RequestParam("requesterEmail") final String requesterEmail,
            @RequestParam("description") final String description) {
        final ModelAndView mav = new ModelAndView("test-mail");
        final String trimmedName = requesterName == null ? "" : requesterName.trim();
        final String trimmedEmail = requesterEmail == null ? "" : requesterEmail.trim();
        final String trimmedDescription = description == null ? "" : description.trim();

        mav.addObject("requesterName", trimmedName);
        mav.addObject("requesterEmail", trimmedEmail);
        mav.addObject("description", trimmedDescription);

        if (trimmedName.isEmpty() || trimmedEmail.isEmpty() || trimmedDescription.isEmpty()) {
            mav.addObject("mailError", "Please enter your name, email address, and request description.");
            return mav;
        }

        try {
            final RequestSubmission requestSubmission =
                    requestService.createRequest(trimmedName, trimmedEmail, trimmedDescription);
            mailService.sendRequestReviewEmail(requestSubmission);
            mav.addObject("mailSuccess", "Your request was sent to Botecito for review.");
        } catch (final MailException | IllegalArgumentException e) {
            mav.addObject(
                    "mailError", "The request email could not be sent. Check the Gmail credentials and SMTP setup.");
        }

        return mav;
    }

    @RequestMapping(value = "/requests/{token}/accept", method = RequestMethod.GET)
    public ModelAndView acceptRequest(@PathVariable("token") final String token) {
        return resolveRequest(token, RequestStatus.ACCEPTED);
    }

    @RequestMapping(value = "/requests/{token}/decline", method = RequestMethod.GET)
    public ModelAndView declineRequest(@PathVariable("token") final String token) {
        return resolveRequest(token, RequestStatus.DECLINED);
    }

    @RequestMapping(value = "/marketplace/{item_id}", method = RequestMethod.GET)
    public ModelAndView marketplaceItem(@PathVariable("item_id") final String itemId) {
        final ModelAndView mav = new ModelAndView("/WEB-INF/views/marketplace-item.jsp");
        mav.addObject("itemId", itemId);
        return mav;
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

    // ====================================
    // TODO reference, demo code from class
    // start
    // ====================================

    // Note: change class root directory from / to /class/
    // Example: /example from class would be /class/example

    private final ClassUserService classUserService;

    // TODO, mail and request services added by us (cleanup classUserService)
    @Autowired
    public HelloWorldController(
            final ClassUserService classUserService,
            final RequestService requestService,
            final MailService mailService) {
        this.mailService = mailService;
        this.requestService = requestService;
        this.classUserService = classUserService;
    }

    @RequestMapping(value = "/class", method = RequestMethod.GET)
    public ModelAndView helloWorld() {
        final ModelAndView mav = new ModelAndView("helloworld/index");
        mav.addObject("message", "Hello World from Controller");
        return mav;
    }

    @RequestMapping(value = "/class", method = RequestMethod.POST)
    public ModelAndView createClassUser(
            @RequestParam("email") final String email,
            @RequestParam("password") final String password,
            @RequestParam("username") final String username) {
        final ModelAndView mav = new ModelAndView("helloworld/index");
        final ClassUser classUser = classUserService.createClassUser(email, password, username);
        mav.addObject("message", "Hello World " + classUser.getUsername());
        return mav;
    }

    @RequestMapping(value = "/class/profile/{id:[0-9]+}", method = RequestMethod.GET)
    public ModelAndView helloWorld(@PathVariable("id") final long id) {
        final ModelAndView mav = new ModelAndView("helloworld/index");
        final Optional<ClassUser> classUser = classUserService.findClassUserById(id);
        mav.addObject("message", "This it the profile for " + classUser.get().getUsername());
        return mav;
    }

    // ====================================
    // TODO reference, demo code from class
    // end
    // ====================================
}
