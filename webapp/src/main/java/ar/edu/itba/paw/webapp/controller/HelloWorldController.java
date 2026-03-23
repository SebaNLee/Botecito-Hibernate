package ar.edu.itba.paw.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HelloWorldController {

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ModelAndView landing() {
        return new ModelAndView("/index.jsp");
    }

    @RequestMapping(value = "/marketplace", method = RequestMethod.GET)
    public ModelAndView marketplace() {
        return new ModelAndView("/marketplace.jsp");
    }

    @RequestMapping(value = "/marketplace/{item_id}", method = RequestMethod.GET)
    public ModelAndView marketplaceItem(@PathVariable("item_id") final String itemId) {
        final ModelAndView mav = new ModelAndView("/marketplace-item.jsp");
        mav.addObject("itemId", itemId);
        return mav;
    }

    // TODO reference, demo code from class
    // @Getter
    // private final UserService userService;

    // @Autowired
    // public HelloWorldController(final UserService userService) {
    //     this.userService = userService;
    // }

    // @RequestMapping(value = "/", method = RequestMethod.GET)
    // public ModelAndView helloWorld() {
    //     final ModelAndView mav = new ModelAndView("index.jsp");
    //     mav.addObject("message", "Hello World from Controller");
    //     return mav;
    // }

    // @RequestMapping(value = "/", method = RequestMethod.POST)
    // public ModelAndView createUser(@RequestParam("email") final String email) {
    //     final ModelAndView mav = new ModelAndView("index.jsp");
    //     Object user = userService.createUser(email);
    //     mav.addObject("message", "Hello World " + user.toString());
    //     return mav;
    // }
}
