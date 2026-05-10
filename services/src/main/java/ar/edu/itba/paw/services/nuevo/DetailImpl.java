package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.ItemDetail;
import ar.edu.itba.paw.persistence.nuevo.DetailDao;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public final class DetailImpl implements DetailInterface {
    private final DetailDao detailDao;

    @Override
    public Optional<ItemDetail> getItemDetail(
            final int itemId, final Optional<Integer> viewerUserId, final Optional<Long> versionId) {
        final Optional<ItemDetail> visible = resolveVisibleItemDetail(itemId, viewerUserId);
        if (visible.isEmpty()) {
            return Optional.empty();
        }
        if (versionId.isEmpty()) {
            return visible;
        }
        final long requested = versionId.get();
        final boolean allowed = visible.get().getVersions().stream().anyMatch(v -> v.getVersionId() == requested);
        if (!allowed) {
            return Optional.empty();
        }
        return visible;
    }

    private Optional<ItemDetail> resolveVisibleItemDetail(final int itemId, final Optional<Integer> viewerUserId) {
        final Optional<ItemDetail> currentOnly = detailDao.getItemDetailById(itemId, false);
        if (currentOnly.isEmpty()) {
            return Optional.empty();
        }
        if (viewerUserId.isEmpty()) {
            return currentOnly;
        }
        final int viewer = viewerUserId.get();
        final Integer ownerId = parseHostId(
                currentOnly.get().getVersions().get(0).getItemModel().getHostId());
        if (ownerId != null && ownerId.equals(viewer)) {
            return detailDao.getItemDetailById(itemId, true);
        }
        final List<Integer> bookedVersionIds = detailDao.findVersionIdsFromGuestBookingsForItem(itemId, viewer);
        if (bookedVersionIds.isEmpty()) {
            return currentOnly;
        }
        final long currentVersionId = currentOnly.get().getVersions().get(0).getVersionId();
        final boolean anyBookingOnOtherVersion =
                bookedVersionIds.stream().anyMatch(id -> id != null && id.longValue() != currentVersionId);
        if (!anyBookingOnOtherVersion) {
            return currentOnly;
        }
        final Set<Integer> merged = new HashSet<>();
        merged.add((int) currentVersionId);
        for (final Integer id : bookedVersionIds) {
            if (id != null) {
                merged.add(id);
            }
        }
        final List<Integer> ordered =
                merged.stream().sorted(Comparator.reverseOrder()).toList();
        return detailDao.getItemDetailForVersionIds(itemId, ordered).or(() -> currentOnly);
    }

    private static Integer parseHostId(final String hostId) {
        if (hostId == null || hostId.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(hostId.trim());
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }
}
