package ar.edu.itba.paw.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.dto.BookingStatusOptionModel;
import ar.edu.itba.paw.models.entity.ItemType;
import ar.edu.itba.paw.models.entity.Location;
import ar.edu.itba.paw.persistence.SelectorsDao;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class SelectorsImplTest {

    @InjectMocks
    private SelectorsImpl selectors;

    @Mock
    private SelectorsDao selectorsDao;

    @Mock
    private MessageSource messageSource;

    @Test
    void testgetDifficultyOptionsReturnsFiveLevels() {
        var options = selectors.getDifficultyOptions();

        assertEquals(5, options.size());
        assertEquals("1 - Principiante", options.get("1"));
        assertEquals("2 - Basico", options.get("2"));
        assertEquals("3 - Intermedio", options.get("3"));
        assertEquals("4 - Avanzado", options.get("4"));
        assertEquals("5 - Experto", options.get("5"));
    }

    @Test
    void getLocationOptionsReturnsList() {
        List<Location> expected = List.of(new Location());
        when(selectorsDao.getLocationOptions()).thenReturn(expected);

        List<Location> result = selectors.getLocationOptions();

        assertNotNull(result);
        assertEquals(expected.size(), result.size());
    }

    @Test
    void getItemTypeOptionsReturnsList() {
        List<ItemType> expected = List.of(new ItemType());
        when(selectorsDao.getItemTypeOptions()).thenReturn(expected);

        List<ItemType> result = selectors.getItemTypeOptions();

        assertNotNull(result);
        assertEquals(expected.size(), result.size());
    }

    @Test
    void getBookingStatusOptionsReturnsAllStatuses() {
        when(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
                .thenReturn("label");

        List<BookingStatusOptionModel> result = selectors.getBookingStatusOptions();

        assertNotNull(result);
        assertEquals(8, result.size());
    }
}
