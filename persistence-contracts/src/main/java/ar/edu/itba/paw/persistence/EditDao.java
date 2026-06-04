package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.entity.Version;
import java.util.Optional;

public interface EditDao {

    Optional<Version> findVersionById(int versionId);

    void removeVersionChildren(Version version);
}
