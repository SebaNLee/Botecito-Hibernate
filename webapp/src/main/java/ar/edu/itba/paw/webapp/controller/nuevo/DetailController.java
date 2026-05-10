package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.webapp.presentation.DetailPresentation;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequiredArgsConstructor
public class DetailController {

    private final DetailPresentation detailPresentation;

    @RequestMapping(value = "/item/{id:[1-9]\\d*}", method = RequestMethod.GET)
    public Object itemDetail(@PathVariable("id") final int itemId, final HttpServletRequest request) {
        return detailPresentation.detailPage(itemId, request, Optional.empty());
    }

    @RequestMapping(value = "/item/{id:[1-9]\\d*}/snapshot/{versionId:[1-9]\\d*}", method = RequestMethod.GET)
    public Object itemDetailSnapshot(
            @PathVariable("id") final int itemId,
            @PathVariable("versionId") final int versionId,
            final HttpServletRequest request) {
        return detailPresentation.detailPage(itemId, request, Optional.of((long) versionId));
    }
}
