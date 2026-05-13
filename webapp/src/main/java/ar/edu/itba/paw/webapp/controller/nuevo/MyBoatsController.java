package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.webapp.presentation.MyBoatsPresentation;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequiredArgsConstructor
public class MyBoatsController {

    private final MyBoatsPresentation myBoatsPresentation;

    @RequestMapping(value = "/my-boats", method = RequestMethod.GET)
    public ModelAndView myBoats(final HttpServletRequest request) {
        return myBoatsPresentation.myBoats(request);
    }
}
