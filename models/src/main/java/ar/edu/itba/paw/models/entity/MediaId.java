package ar.edu.itba.paw.models.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
@EqualsAndHashCode
public class MediaId implements Serializable {

    // remember that this is a composite PK (version_id, index)

    @Column(name = "version_id", nullable = false)
    private Integer versionId;

    @Column(name = "index", nullable = false)
    private Integer index;
}
