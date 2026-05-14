package ar.edu.itba.paw.models.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

@AllArgsConstructor
@Builder
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "item")
public class ItemOrm {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "new_item_id_seq")
    @SequenceGenerator(name = "new_item_id_seq", sequenceName = "new_item_id_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "host_id")
    private UsersOrm host;

    @Enumerated(EnumType.STRING)
    @Type(type = "pgsql_enum")
    @Column(name = "status", nullable = false, columnDefinition = "item_status_enum")
    private ItemStatusEnumOrm status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
