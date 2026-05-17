package ar.edu.itba.paw.models.entity;

import java.time.LocalDateTime;
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
@Table(name = "users")
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "new_users_id_seq")
    @SequenceGenerator(name = "new_users_id_seq", sequenceName = "new_users_id_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @Column(name = "alias", length = 30)
    private String alias;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "mail_token", length = 100)
    private String mailToken;

    @Column(name = "mail_token_emitted_at")
    private LocalDateTime mailTokenEmittedAt;

    @Column(name = "verified", nullable = false)
    private Boolean verified;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
