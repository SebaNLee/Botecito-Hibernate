package ar.edu.itba.paw.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.persistence.PublishDao;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PublishServiceImplTest {

    private static final int OWNER_ID = 1;
    private static final int TYPE_ID = 5;
    private static final String TITLE = "Test Item";
    private static final String DESCRIPTION = "Test Description";
    private static final int PRICE = 1000;
    private static final int CAPACITY = 4;
    private static final int WEIGHT = 50;
    private static final int DIFFICULTY = 3;
    private static final int LOCATION_ID = 2;

    @Mock
    private PublishDao publishDao;

    @Mock
    private MailService mailService;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private PublishServiceImpl publishService;

    @Test
    public void createWorksOk() {
        Item item = Item.builder().id(1).build();
        when(publishDao.persistItem(any())).thenReturn(item);
        when(publishDao.persistVersion(any()))
                .thenReturn(Version.builder().id(1).item(item).build());

        publishService.create(
                OWNER_ID,
                TYPE_ID,
                TITLE,
                DESCRIPTION,
                PRICE,
                CAPACITY,
                WEIGHT,
                DIFFICULTY,
                LOCATION_ID,
                List.of(),
                List.of());
    }

    @Test
    public void createExceptionWhenIncomplete() {
        assertThrows(
                NullPointerException.class,
                () -> publishService.create(
                        OWNER_ID,
                        TYPE_ID,
                        TITLE,
                        DESCRIPTION,
                        PRICE,
                        CAPACITY,
                        WEIGHT,
                        null,
                        LOCATION_ID,
                        List.of(),
                        List.of()));
    }
}
