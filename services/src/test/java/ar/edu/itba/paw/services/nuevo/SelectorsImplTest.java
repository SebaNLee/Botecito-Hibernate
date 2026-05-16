package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.persistence.nuevo.SelectorsDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class SelectorsImplTest {

    @InjectMocks
    private SelectorsImpl selectors;

    @Mock
    private SelectorsDao selectorsDao;

    @Mock
    private MessageSource messageSource;

    @Test
    void testGetDifficultyOptionsReturnsFiveLevels() {
        var options = selectors.getDifficultyOptions();

        assertEquals(5, options.size());
        assertEquals("1 - Principiante", options.get("1"));
        assertEquals("2 - Basico", options.get("2"));
        assertEquals("3 - Intermedio", options.get("3"));
        assertEquals("4 - Avanzado", options.get("4"));
        assertEquals("5 - Experto", options.get("5"));
    }
}
