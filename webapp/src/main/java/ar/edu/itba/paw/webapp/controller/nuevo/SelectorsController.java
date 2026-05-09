package ar.edu.itba.paw.webapp.controller.nuevo;

import ar.edu.itba.paw.models.nuevo.ItemTypeModel;
import ar.edu.itba.paw.models.nuevo.Location;
import ar.edu.itba.paw.webapp.presentation.nuevo.SelectorsPresentation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class SelectorsController {

    private final SelectorsPresentation selectorsPresentation;

    @ResponseBody
    @RequestMapping(
            value = "/location-options",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Location> getLocationOptions() {
        return selectorsPresentation.getLocationOptions();
    }

    @ResponseBody
    @RequestMapping(
            value = "/item-type-options",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ItemTypeModel> getItemTypeOptions() {
        return selectorsPresentation.getItemTypeOptions();
    }
}
