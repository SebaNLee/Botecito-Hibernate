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
    public void searchMarketplaceReturnsResult() {
        SearchResult<Item> expected = new SearchResult<>(List.of(), 5L);
        when(marketplaceDao.searchMarketplace(any())).thenReturn(expected);

        SearchResult<Item> result = marketplaceService.searchMarketplace(
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertNotNull(result);
        assertEquals(expected.getTotalCount(), result.getTotalCount());
    }
}
