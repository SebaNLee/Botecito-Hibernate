package ar.edu.itba.paw.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class PublishActionController {

    @RequestMapping(value = "/publish/delete", method = RequestMethod.GET)
    public ModelAndView deletePublicationPlaceholder() {
        // TODO replace this placeholder endpoint once publications are persisted.
        // A real delete action should resolve a DB-backed publication/token and update its state.
        // related to MailServiceImpl.java delete is a placeholder
        final ModelAndView mav = new ModelAndView("request-action-result");
        mav.addObject("actionTitle", "Publication delete unavailable");
        mav.addObject(
                "actionMessage",
                "This visual publish flow does not persist publications yet, so there is nothing to delete.");
        return mav;
    }
}
