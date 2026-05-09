package ar.edu.itba.paw.persistence.orm.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

@Getter
@Setter
@Entity
@Table(name = "review")
public class ReviewOrm {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "new_review_id_seq")
    @SequenceGenerator(name = "new_review_id_seq", sequenceName = "new_review_id_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private BookingOrm booking;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private UsersOrm sender;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(name = "target_type", nullable = false, columnDefinition = "target_enum")
    private TargetEnumOrm targetType;

    @Column(name = "rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "comment", length = 255)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ReviewOrm() {}
}
