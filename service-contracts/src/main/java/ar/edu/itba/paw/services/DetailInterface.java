package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.ItemDetail;
import java.util.List;
import java.util.Optional;

public interface DetailInterface {
    /**
     * Anonymous access; returns the current published version only.
     */
    Optional<ItemDetail> getItemDetail(int itemId);

    /**
     * Authenticated access for a specific version; returns all visible
     * versions if {@code versionId} is in the viewer's visible set,
     * empty otherwise.
     */
    Optional<ItemDetail> getItemDetail(int itemId, int viewerUserId, long versionId);

    /**
     * Returns the version ids visible to the viewer (for populating
     * the version selector control).
     */
    List<Long> getVisibleVersionIds(int itemId, int viewerUserId);
}
