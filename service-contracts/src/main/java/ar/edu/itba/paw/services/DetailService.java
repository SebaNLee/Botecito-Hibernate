package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.entity.Item;

public interface DetailService {

    Item getItemDetail(int itemId, int reviewPage);
}
