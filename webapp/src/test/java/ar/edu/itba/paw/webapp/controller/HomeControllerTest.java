package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.DisabledTimeSlotService;
import ar.edu.itba.paw.services.ItemService;
import java.util.List;
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

    @Mock
    private DisabledTimeSlotService disabledTimeSlotService;

    @Test
    public void testLandingReturnsIndexView() {
        Mockito.when(itemService.listAvailabilities()).thenReturn(List.of());
        Mockito.when(itemService.listBookings()).thenReturn(List.of());
        Mockito.when(disabledTimeSlotService.listAll()).thenReturn(List.of());

        final HomeController controller = new HomeController(itemService, disabledTimeSlotService);
        final ModelAndView mav = controller.landing();

        Assertions.assertEquals("index", mav.getViewName());
        Assertions.assertTrue(mav.getModel().containsKey("searchOfferedDatesJson"));
    }
}
