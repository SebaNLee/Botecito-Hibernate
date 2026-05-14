package ar.edu.itba.paw.models.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "item_type")
public class ItemTypeOrm {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "new_item_type_id_seq")
    @SequenceGenerator(name = "new_item_type_id_seq", sequenceName = "new_item_type_id_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;
}
