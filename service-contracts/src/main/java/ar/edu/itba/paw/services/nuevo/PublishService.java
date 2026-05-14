package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.AvailabilityWindow;
import ar.edu.itba.paw.models.nuevo.PublishContent;
import ar.edu.itba.paw.models.nuevo.PublishItem;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PublishService {
    Optional<PublishItem> create(PublishContent draft, int ownerId);

    Optional<PublishItem> findById(int itemId);

    List<AvailabilityWindow> listAvailabilities(int itemId);

    Map<String, String> validate(PublishContent draft);
}
