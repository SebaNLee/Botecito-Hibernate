package ar.edu.itba.paw.persistence.orm;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "version")
public class VersionOrm {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "version_id_seq")
    @SequenceGenerator(name = "version_id_seq", sequenceName = "version_id_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemOrm item;

    @ManyToOne(optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    private ItemTypeOrm type;

    @Column(name = "title", nullable = false, length = 1000)
    private String title;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "weight", nullable = false)
    private Integer weight;

    @Column(name = "difficulty", nullable = false)
    private Integer difficulty;

    @ManyToOne(optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private LocationOrm location;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public VersionOrm() {}
}
