package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.User;
import ar.edu.itba.paw.services.UserService;
import ar.edu.itba.paw.webapp.form.RegisterForm;
import javax.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(final UserService userService) {
        this.userService = userService;
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView login(
            @RequestParam(value = "error", required = false) final String error,
            @RequestParam(value = "logout", required = false) final String logout,
            @RequestParam(value = "registered", required = false) final String registered) {

        final ModelAndView mav = new ModelAndView("login");
        if (error != null) {
            mav.addObject("loginError", true);
        }
        if (logout != null) {
            mav.addObject("logoutSuccess", true);
        }
        if (registered != null) {
            mav.addObject("registeredSuccess", true);
        }
        return mav;
    }

    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public ModelAndView registerForm(@ModelAttribute("registerForm") final RegisterForm form) {
        return new ModelAndView("register");
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public ModelAndView registerSubmit(
            @Valid @ModelAttribute("registerForm") final RegisterForm form, final BindingResult errors) {

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            errors.rejectValue("confirmPassword", "register.validation.password.mismatch");
        }

        if (errors.hasErrors()) {
            return new ModelAndView("register");
        }

        if (userService.findByEmail(form.getEmail().trim()).isPresent()) {
            errors.rejectValue("email", "register.validation.email.duplicate");
            return new ModelAndView("register");
        }

        userService.register(
                form.getGivenName().trim(),
                form.getLastName().trim(),
                form.getEmail().trim(),
                form.getPassword());

        return new ModelAndView("redirect:/login?registered=true");
    }

    @RequestMapping(value = "/profile", method = RequestMethod.GET)
    public ModelAndView profile() {
        // TODO: replace mock data with authenticated user lookup:
        //   final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        //   if (auth == null || !auth.isAuthenticated()) {
        //       return new ModelAndView("redirect:/login");
        //   }
        //   final User user = userService.findByEmail(auth.getName()).orElse(null);
        //   if (user == null) {
        //       return new ModelAndView("redirect:/login");
        //   }

        final User user = new User();
        user.setGivenName("Juan");
        user.setLastName("Perez");
        user.setEmail("juan.perez@botecito.com");
        user.setPhone("+54 11 1234-5678");
        user.setPreferredLanguage("es");
        user.setCreatedAt("2026-01-15T10:30:00-03:00");

        final ModelAndView mav = new ModelAndView("profile");
        mav.addObject("user", user);
        return mav;
    }

    @RequestMapping("/403")
    public ModelAndView forbidden() {
        return new ModelAndView("403");
    }
}
