package ar.edu.itba.paw.models.dto;

import lombok.Value;

@Value
public class DetailPageFlags {
    boolean isOwner;
    boolean canFavouriteItem;
    boolean favouriteItem;
    boolean alreadyReported;
    boolean canSubscribeToOwner;
    boolean subscribedToOwner;
}
