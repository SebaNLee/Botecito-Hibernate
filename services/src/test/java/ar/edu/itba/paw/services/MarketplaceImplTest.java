package ar.edu.itba.paw.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.dto.SearchResult;
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

    @InjectMocks
    private MarketplaceImpl marketplaceService;

    @Test
    public void searchMarketplaceReturnsItems() {
        Item item = Item.builder().id(1).build();
        SearchResult<Item> expected = new SearchResult<>(List.of(item), 10L);
        when(marketplaceDao.searchMarketplace(any())).thenReturn(expected);

        SearchResult<Item> result = marketplaceService.searchMarketplace(
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.getPageElements().size());
        assertSame(item, result.getPageElements().get(0));
        assertEquals(10L, result.getTotalCount());
        verify(marketplaceDao).searchMarketplace(any());
    }
}
