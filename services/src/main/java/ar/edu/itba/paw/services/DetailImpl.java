package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.ItemDetail;
import ar.edu.itba.paw.persistence.DetailDao;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves item detail visibility for guests and hosts. {@link ItemDetail#getVersions()} entries include per-version
 * availability windows, bookings, and reviews supplied by {@link DetailDao} implementations.
 */
@Service
@RequiredArgsConstructor
public final class DetailImpl implements DetailService {
    private final DetailDao detailDao;

    @Override
    @Transactional(readOnly = true)
    public Optional<ItemDetail> getItemDetail(final int itemId) {
        return resolveVisibleItemDetail(itemId, Optional.empty());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ItemDetail> getItemDetail(final int itemId, final int viewerUserId, final long versionId) {
        final Optional<ItemDetail> visible = resolveVisibleItemDetail(itemId, Optional.of(viewerUserId));
        if (visible.isEmpty()) {
            return Optional.empty();
        }
        final boolean allowed = visible.get().getVersions().stream().anyMatch(v -> v.getVersionId() == versionId);
        if (!allowed) {
            return Optional.empty();
        }
        return visible;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getVisibleVersionIds(final int itemId, final int viewerUserId) {
        final Optional<ItemDetail> visible = resolveVisibleItemDetail(itemId, Optional.of(viewerUserId));
        if (visible.isEmpty()) {
            return List.of();
        }
        return visible.get().getVersions().stream()
                .map(v -> (long) v.getVersionId())
                .toList();
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
        if (currentOnly.get().getVersions().get(0).getHostId() == viewer) {
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
}
