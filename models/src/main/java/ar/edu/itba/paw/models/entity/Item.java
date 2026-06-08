package ar.edu.itba.paw.models.entity;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.dto.ReviewSummary;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.Type;

@AllArgsConstructor
@Builder
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "item")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "new_item_id_seq")
    @SequenceGenerator(name = "new_item_id_seq", sequenceName = "new_item_id_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "host_id")
    private Users host;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(name = "status", nullable = false, columnDefinition = "item_status_enum")
    private ItemStatusEnum status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinFormula(
            value = "(SELECT v.id FROM version v WHERE v.item_id = id"
                    + " AND v.created_at = (SELECT MAX(v2.created_at) FROM version v2 WHERE v2.item_id = id)"
                    + " LIMIT 1)")
    private Version latestVersion;

    @Transient
    private List<Booking> bookings;

    @Transient
    private ReviewSummary reviewSummary;

    @Transient
    private PageModel<Review> itemReviews;
}
