package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.ClassUser;
import ar.edu.itba.paw.services.ClassUserService;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class ClassDemoController {

    private final ClassUserService classUserService;

    @Autowired
    public ClassDemoController(final ClassUserService classUserService) {
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
    public ModelAndView profile(@PathVariable("id") final long id) {
        final ModelAndView mav = new ModelAndView("helloworld/index");
        final Optional<ClassUser> classUser = classUserService.findClassUserById(id);
        mav.addObject("message", "This it the profile for " + classUser.get().getUsername());
        return mav;
    }
}
