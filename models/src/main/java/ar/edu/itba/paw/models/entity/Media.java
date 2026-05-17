package ar.edu.itba.paw.models.entity;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "media")
public class Media {

    @EmbeddedId
    private MediaId id; // remember that this is a composite PK (version_id, index)

    @MapsId("versionId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private Version version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;
}
