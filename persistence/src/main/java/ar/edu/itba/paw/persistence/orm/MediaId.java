package ar.edu.itba.paw.persistence.orm;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@EqualsAndHashCode
public class MediaId implements Serializable {

    // remember that this is a composite PK (version_id, index)

    @Column(name = "version_id", nullable = false)
    private Integer versionId;

    @Column(name = "index", nullable = false)
    private Integer index;

    public MediaId() {}

    public MediaId(final Integer versionId, final Integer index) {
        this.versionId = versionId;
        this.index = index;
    }
}
