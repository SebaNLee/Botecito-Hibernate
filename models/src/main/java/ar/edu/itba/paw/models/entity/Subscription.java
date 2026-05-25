package ar.edu.itba.paw.models.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
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
@Table(name = "subscriptons")
public class Subscription {

    @EmbeddedId
    private SubscriptionId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("subscriberId")
    @JoinColumn(name = "subscriber_id", nullable = false)
    private Users subscriber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("subscribedToId")
    @JoinColumn(name = "subscribed_to_id", nullable = false)
    private Users subscribedTo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
