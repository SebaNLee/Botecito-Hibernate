package ar.edu.itba.paw.services;

import static ar.edu.itba.paw.services.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.persistence.MarketplaceDao;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MarketplaceImplTest {

    @Mock
    private MarketplaceDao marketplaceDao;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private MarketplaceImpl marketplaceService;

    @Test
    public void searchMarketplaceReturnsItems() {
        Item item = item(1);
        PageModel<Item> expected = new PageModel<>(List.of(item), 1, 12, 10);
        when(marketplaceDao.searchMarketplace(any())).thenReturn(expected);

        PageModel<Item> result = marketplaceService.searchMarketplace(
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertSame(item, result.getContent().get(0));
        assertEquals(10, result.getTotalItems());
    }
}
