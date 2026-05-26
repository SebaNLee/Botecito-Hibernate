package ar.edu.itba.paw.models.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment_proof")
public class PaymentProof {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_proof_id_seq")
    @SequenceGenerator(name = "payment_proof_id_seq", sequenceName = "payment_proof_id_seq", allocationSize = 1)
    private Integer id;

    @OneToOne(optional = false)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(name = "filename", nullable = false, length = 100)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Type(type = "org.hibernate.type.BinaryType")
    @Column(name = "file_data", nullable = false)
    private byte[] fileData;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "refuse_msg", length = 255)
    private String refuseMsg;

    @Column(name = "refused_at")
    private LocalDateTime refusedAt;

    @Column(name = "reply_msg", length = 255)
    private String replyMsg;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;
}
