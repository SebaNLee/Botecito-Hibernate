package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.models.dto.BookingStatusOptionModel;
import ar.edu.itba.paw.models.entity.ItemType;
import ar.edu.itba.paw.models.entity.Location;
import ar.edu.itba.paw.services.SelectorsService;
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

    private final SelectorsService selectorsService;

    @ResponseBody
    @RequestMapping(
            value = "/location-options",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Location> getLocationOptions() {
        return selectorsService.getLocationOptions();
    }

    @ResponseBody
    @RequestMapping(
            value = "/item-type-options",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ItemType> getItemTypeOptions() {
        return selectorsService.getItemTypeOptions();
    }

    @ResponseBody
    @RequestMapping(
            value = "/booking-status-options",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public List<BookingStatusOptionModel> getBookingStatusOptions() {
        return selectorsService.getBookingStatusOptions();
    }
}
