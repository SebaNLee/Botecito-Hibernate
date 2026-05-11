package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.webapp.form.ItemBookingForm;
import javax.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/item")
public class ItemController {

    @RequestMapping(value = "/{id}/book", method = RequestMethod.GET)
    public ModelAndView bookForm(@PathVariable("id") long id, @ModelAttribute("bookForm") final ItemBookingForm form) {
        ModelAndView mav = new ModelAndView("bookItem");
        mav.addObject("itemId", id);
        return mav;
    }

    @RequestMapping(value = "/{id}/book", method = RequestMethod.POST)
    public ModelAndView book(
            @PathVariable("id") long id,
            @Valid @ModelAttribute("bookForm") final ItemBookingForm form,
            final BindingResult errors) {
        if (errors.hasErrors()) {
            return bookForm(id, form);
        }

        if (form.getStartTime() != null
                && form.getEndTime() != null
                && !form.getStartTime().isBefore(form.getEndTime())) {
            errors.rejectValue("endTime", "endtime.before.starttime");
            return bookForm(id, form);
        }

        // booking logic goes here
        return new ModelAndView("redirect:/");
    }
}
