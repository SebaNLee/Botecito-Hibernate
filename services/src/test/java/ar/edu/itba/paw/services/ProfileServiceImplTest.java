package ar.edu.itba.paw.services;

import static ar.edu.itba.paw.services.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.dto.HostReviewsPage;
import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProfileServiceImplTest {

    private static final int OWNER_ID = 1;
    private static final int PAGE = 1;
    private static final int PAGE_SIZE = 10;

    @Mock
    private ItemService itemService;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @Test
    public void testListProfileListings() {
        var items = List.of(item(1));
        var expected = new PageModel<Item>(items, PAGE, PAGE_SIZE, 1);
        when(itemService.listOwnerItems(anyInt(), isNull(), isNull(), anyInt(), anyInt(), isNull()))
                .thenReturn(expected);

        var result = profileService.listProfileListings(OWNER_ID, PAGE, PAGE_SIZE, null);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    public void testFindHostReviewsPage() {
        var expected = new HostReviewsPage(new PageModel<>(List.of(), 1, 3, 0), null);
        when(reviewService.findHostReviewsPage(OWNER_ID, 1)).thenReturn(expected);

        var result = profileService.findHostReviewsPage(OWNER_ID, 1);

        assertEquals(expected, result);
    }
}
