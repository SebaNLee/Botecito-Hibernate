package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.ItemDetail;
import java.util.Optional;

public interface DetailInterface {
    /**
     * Resolves visible {@link ItemDetail} for an item. Visibility matches existing rules (anonymous: current version
     * only; host: all versions; other users: current plus versions tied to their bookings). When {@code versionId} is
     * present, the version must appear in that visible set or the result is empty. When absent, the same visible set
     * is returned (the current head is always {@code versions.get(0)}).
     */
    Optional<ItemDetail> getItemDetail(int itemId, Optional<Integer> viewerUserId, Optional<Long> versionId);
}
