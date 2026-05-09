package ar.edu.itba.paw.persistence.orm.entities;

import java.time.LocalTime;
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

@Getter
@Setter
@Entity
@Table(name = "availability")
public class AvailabilityOrm {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "availability_id_seq")
    @SequenceGenerator(name = "availability_id_seq", sequenceName = "availability_id_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private VersionOrm version;

    @Enumerated(EnumType.STRING)
    @Column(name = "weekday", nullable = false)
    private WeekdayEnumOrm weekday;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    protected AvailabilityOrm() {}
}
