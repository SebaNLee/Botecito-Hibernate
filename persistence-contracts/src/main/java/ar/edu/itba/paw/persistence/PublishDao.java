package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Availability;
import ar.edu.itba.paw.models.entity.Image;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.Media;
import ar.edu.itba.paw.models.entity.Version;

public interface PublishDao {

    Item persistItem(Item item);

    Version persistVersion(Version version);

    Availability persistAvailability(Availability availability);

    Image persistImage(Image image);

    Media persistMedia(Media media);

    void flush();
}
