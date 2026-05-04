package ar.edu.itba.paw.persistence;

import java.util.List;
import java.util.Optional;

public interface ItemMediaDao {

    Optional<byte[]> findImageById(int id);

    List<Integer> listImageIdsByItemIdOrdered(int itemId);

    Optional<Integer> findCoverImageIdByItemId(int itemId);

    int countImagesByItemId(int itemId);

    Integer insertImage(int itemId, byte[] imageData, int displayOrder);

    boolean deleteImage(int itemId, int imageId);

    void reorderImages(int itemId, List<Integer> imageIdsInOrder);

    Integer replacePrimaryImage(int itemId, byte[] imageData);
}
