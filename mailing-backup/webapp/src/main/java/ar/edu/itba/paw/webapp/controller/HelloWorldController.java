package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.MailService;
import ar.edu.itba.paw.services.UserService;
import java.util.Optional;
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
    private final UserService userService;

    public HelloWorldController(final MailService mailService, final UserService userService) {
        this.mailService = mailService;
        this.userService = userService;
    }

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
        return new ModelAndView("test-mail.jsp");
    }

    @RequestMapping(value = "/test-mail", method = RequestMethod.POST)
    public ModelAndView sendTestMail(@RequestParam("email") final String email) {
        final ModelAndView mav = new ModelAndView("test-mail.jsp");
        final String trimmedEmail = email == null ? "" : email.trim();

        mav.addObject("email", trimmedEmail);

        if (trimmedEmail.isEmpty()) {
            mav.addObject("mailError", "Please enter an email address.");
            return mav;
        }

        try {
            mailService.sendTestConfirmationEmail(trimmedEmail);
            mav.addObject("mailSuccess", "Test email sent to " + trimmedEmail + ".");
        } catch (final MailException | IllegalArgumentException e) {
            mav.addObject("mailError", "The test email could not be sent. Check the Gmail credentials and SMTP setup.");
        }

        return mav;
    }

    @RequestMapping(value = "/marketplace/{item_id}", method = RequestMethod.GET)
    public ModelAndView marketplaceItem(@PathVariable("item_id") final String itemId) {
        final ModelAndView mav = new ModelAndView("/WEB-INF/views/marketplace-item.jsp");
        mav.addObject("itemId", itemId);
        return mav;
    }

    // ====================================
    // TODO reference, demo code from class
    // start
    // ====================================

    // Note: change class root directory from / to /class/
    // Example: /example from class would be /class/example

    @RequestMapping(value = "/class", method = RequestMethod.GET)
    public ModelAndView helloWorld() {
        final ModelAndView mav = new ModelAndView("helloworld/index");
        mav.addObject("message", "Hello World from Controller");
        return mav;
    }

    @RequestMapping(value = "/class", method = RequestMethod.POST)
    public ModelAndView createUser(
            @RequestParam("email") final String email,
            @RequestParam("password") final String password,
            @RequestParam("username") final String username) {
        final ModelAndView mav = new ModelAndView("helloworld/index");
        User user = userService.createUser(email, password, username);
        mav.addObject("message", "Hello World " + user.getUsername());
        return mav;
    }

    @RequestMapping(value = "/class/profile/{id:[0-9]+}", method = RequestMethod.GET)
    public ModelAndView helloWorld(@PathVariable("id") final long id) {
        final ModelAndView mav = new ModelAndView("helloworld/index");
        final Optional<User> user = userService.findById(id);
        mav.addObject("message", "This it the profile for " + user.get().getUsername());
        return mav;
    }

    // ====================================
    // TODO reference, demo code from class
    // end
    // ====================================
}
