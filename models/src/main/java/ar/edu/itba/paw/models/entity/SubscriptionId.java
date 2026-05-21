package ar.edu.itba.paw.models.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class SubscriptionId implements Serializable {

    @Column(name = "subscriber_id")
    private Integer subscriberId;

    @Column(name = "subscribed_to_id")
    private Integer subscribedToId;
}
