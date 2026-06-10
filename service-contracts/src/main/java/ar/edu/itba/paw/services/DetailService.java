package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.ItemDetailPageData;

public interface DetailService {

    ItemDetailPageData getItemDetailPage(int itemId, int reviewPage, Integer viewerId);

    ItemDetailPageData getItemDetailPage(int itemId, int reviewPage, int hostId, Integer viewerId);
}
