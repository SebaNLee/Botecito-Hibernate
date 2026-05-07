package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.PreferredLanguage;
import ar.edu.itba.paw.models.User;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class UserJdbcDao implements UserDao {

    private static final @NonNull RowMapper<User> USER_ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        final User user = new User();
        user.setId(rs.getInt("id"));
        user.setCreatedAt(readOffsetDateTime(rs, "created_at"));
        user.setGivenName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setPaymentAlias(rs.getString("alias"));
        user.setPreferredLanguage(PreferredLanguage.fromPersistence(rs.getString("language")));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setPasswordRecoveryToken(rs.getString("mail_token"));
        user.setPasswordRecoveryUsedAt(readOffsetDateTime(rs, "mail_token_emitted_at"));
        return user;
    };

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserJdbcDao(final @NonNull DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Optional<User> findById(final int id) {
        return jdbcTemplate.query("SELECT * FROM users WHERE id = ?", USER_ROW_MAPPER, id).stream()
                .findAny();
    }

    @Override
    public Optional<User> findByEmail(final String email) {
        return jdbcTemplate.query("SELECT * FROM users WHERE lower(email) = lower(?)", USER_ROW_MAPPER, email).stream()
                .findAny();
    }

    @Override
    public User createUser(
            final String givenName,
            final String lastName,
            final String email,
            final String passwordHash,
            final String paymentAlias,
            final String preferredLanguage) {
        final int insertedRows = jdbcTemplate.update(
                "INSERT INTO users (first_name, last_name, email, language, password_hash, alias)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                givenName,
                lastName,
                email,
                preferredLanguage,
                passwordHash,
                paymentAlias);
        if (insertedRows == 0) {
            throw new IllegalStateException("Could not create user for email " + email);
        }

        final Integer id =
                jdbcTemplate.queryForObject("SELECT id FROM users WHERE lower(email) = lower(?)", Integer.class, email);
        return findById(Objects.requireNonNull(id, "Could not read inserted user id for email " + email)
                        .intValue())
                .orElseThrow(() -> new IllegalStateException("Could not read inserted user " + id));
    }

    @Override
    public Optional<User> claimUser(
            final String givenName,
            final String lastName,
            final String email,
            final String passwordHash,
            final String paymentAlias,
            final String preferredLanguage) {
        final int updatedRows = jdbcTemplate.update(
                "UPDATE users"
                        + " SET first_name = ?, last_name = ?, language = ?, password_hash = ?, alias = COALESCE(?, alias)"
                        + " WHERE lower(email) = lower(?)"
                        + " AND password_hash IS NULL",
                givenName,
                lastName,
                preferredLanguage,
                passwordHash,
                paymentAlias,
                email);
        if (updatedRows == 0) {
            return Optional.empty();
        }
        return findByEmail(email);
    }

    @Override
    public Optional<User> updateProfile(
            final int userId,
            final String givenName,
            final String lastName,
            final String email,
            final String phone,
            final String paymentAlias,
            final String preferredLanguage) {
        final int updatedRows = jdbcTemplate.update(
                "UPDATE users"
                        + " SET first_name = ?, last_name = ?, email = ?, phone = ?, alias = ?, language = ?"
                        + " WHERE id = ?",
                givenName,
                lastName,
                email,
                phone,
                paymentAlias,
                preferredLanguage,
                userId);
        if (updatedRows == 0) {
            return Optional.empty();
        }
        return findById(userId);
    }

    @Override
    public User createUserWithoutCredentials(
            final String givenName, final String lastName, final String email, final String preferredLanguage) {
        final int id = Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                        "INSERT INTO users (first_name, last_name, email, language) VALUES (?, ?, ?, ?) RETURNING id",
                        Integer.class,
                        givenName,
                        lastName,
                        email,
                        preferredLanguage),
                "Could not create user for email " + email);
        return findById(id).orElseThrow(() -> new IllegalStateException("Could not read inserted user " + id));
    }

    @Override
    public boolean updateBasicProfileNamesAndLanguage(
            final int userId, final String givenName, final String lastName, final String preferredLanguage) {
        return jdbcTemplate.update(
                        "UPDATE users SET first_name = ?, last_name = ?, language = ? WHERE id = ?",
                        givenName,
                        lastName,
                        preferredLanguage,
                        userId)
                > 0;
    }

    @Override
    public List<User> findUsersByIds(final Collection<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        final String placeholders = String.join(", ", Collections.nCopies(userIds.size(), "?"));
        return jdbcTemplate.query(
                "SELECT * FROM users WHERE id IN (" + placeholders + ")", USER_ROW_MAPPER, userIds.toArray());
    }

    @Override
    public Optional<User> updatePasswordRecoveryToken(final int userId, final String token) {
        final int updatedRows = jdbcTemplate.update(
                "UPDATE users SET mail_token = ?, mail_token_emitted_at = NULL WHERE id = ?", token, userId);
        if (updatedRows == 0) {
            return Optional.empty();
        }
        return findById(userId);
    }

    @Override
    public Optional<User> findByPasswordRecoveryToken(final String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return jdbcTemplate.query("SELECT * FROM users WHERE mail_token = ?", USER_ROW_MAPPER, token).stream()
                .findAny();
    }

    @Override
    public boolean resetPasswordByRecoveryToken(
            final String token, final String passwordHash, final OffsetDateTime usedAt) {
        final int updatedRows = jdbcTemplate.update(
                "UPDATE users"
                        + " SET password_hash = ?, mail_token_emitted_at = ?"
                        + " WHERE mail_token = ?"
                        + " AND mail_token_emitted_at IS NULL",
                passwordHash,
                Timestamp.from(Objects.requireNonNull(usedAt).toInstant()),
                token);
        return updatedRows > 0;
    }

    private static OffsetDateTime readOffsetDateTime(final ResultSet rs, final String columnName)
            throws java.sql.SQLException {
        final Timestamp timestamp = rs.getTimestamp(columnName);
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
