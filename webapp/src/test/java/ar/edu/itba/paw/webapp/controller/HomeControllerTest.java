package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.ItemService;
import ar.edu.itba.paw.services.dto.AvailabilityPickerData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;

@ExtendWith(MockitoExtension.class)
public class HomeControllerTest {

    @Mock
    private ItemService itemService;

    @Test
    public void testLandingReturnsIndexView() {

        Mockito.when(itemService.buildGlobalAvailabilityPicker())
                .thenReturn(new AvailabilityPickerData(List.of(), List.of(), Map.of(), Map.of()));

        final HomeController controller = new HomeController(itemService);
        final ModelAndView mav = controller.landing();

        Assertions.assertEquals("index", mav.getViewName());
        Assertions.assertTrue(mav.getModel().containsKey("searchOfferedDatesJson"));
    }
}
