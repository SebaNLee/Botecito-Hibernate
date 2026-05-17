package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.dto.ItemDetail;
import java.util.List;
import java.util.Optional;

public interface DetailDao {
    /**
     * @param allVersions when {@code false}, only the row for
     *                    {@code MAX(version.id)} per item (public “current”
     *                    listing). When {@code true}, every version row for the
     *                    item, newest {@code version.id} first.
     */
    Optional<ItemDetail> getItemDetailById(int itemId, boolean allVersions);

    List<Integer> findVersionIdsFromGuestBookingsForItem(int itemId, int guestUserId);

    Optional<ItemDetail> getItemDetailForVersionIds(int itemId, List<Integer> versionIds);

    Optional<ItemDetail> getItemDetailCurrent(int itemId);
}
