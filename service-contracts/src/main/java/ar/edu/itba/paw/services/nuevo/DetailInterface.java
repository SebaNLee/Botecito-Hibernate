package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.ItemDetail;
import java.util.Optional;

// Fix: instead of one method with optional parameters, make two overloads, one that only takes itemId (for current
// version), and one that takes all 3 parameters, not optional (for specific version)

public interface DetailInterface {
    /**
     * Resolves visible {@link ItemDetail} for an item. Visibility matches existing
     * rules (anonymous: current version
     * only; host: all versions; other users: current plus versions tied to their
     * bookings). When {@code versionId} is
     * present, the version must appear in that visible set or the result is empty.
     * When absent, the same visible set
     * is returned (the current head is always {@code versions.get(0)}).
     */
    Optional<ItemDetail> getItemDetail(int itemId, Optional<Integer> viewerUserId, Optional<Long> versionId);
}
