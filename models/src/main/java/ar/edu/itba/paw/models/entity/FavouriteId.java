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
public class FavouriteId implements Serializable {

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "item_id")
    private Integer itemId;
}
