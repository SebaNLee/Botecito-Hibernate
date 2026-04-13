package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.User;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
        user.setGivenName(rs.getString("given_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setPreferredLanguage(rs.getString("preferred_language"));
        user.setPasswordHash(rs.getString("password_hash"));
        return user;
    };

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserJdbcDao(final DataSource dataSource) {
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
    public User createUser(final String givenName, final String lastName, final String email, final String passwordHash) {
        final int insertedRows = jdbcTemplate.update(
                "INSERT INTO users (given_name, last_name, email, preferred_language, password_hash) VALUES (?, ?, ?, ?, ?)",
                givenName,
                lastName,
                email,
                "es",
                passwordHash);
        if (insertedRows == 0) {
            throw new IllegalStateException("Could not create user for email " + email);
        }

        final Integer id = jdbcTemplate.queryForObject("SELECT id FROM users WHERE lower(email) = lower(?)", Integer.class, email);
        if (id == null) {
            throw new IllegalStateException("Could not read inserted user id for email " + email);
        }
        return findById(id.intValue())
                .orElseThrow(() -> new IllegalStateException("Could not read inserted user " + id));
    }

    @Override
    public Optional<User> claimUser(
            final String givenName, final String lastName, final String email, final String passwordHash) {
        final int updatedRows = jdbcTemplate.update(
                "UPDATE users"
                        + " SET given_name = ?, last_name = ?, password_hash = ?"
                        + " WHERE lower(email) = lower(?)"
                        + " AND password_hash IS NULL",
                givenName,
                lastName,
                passwordHash,
                email);
        if (updatedRows == 0) {
            return Optional.empty();
        }
        return findByEmail(email);
    }

    private static OffsetDateTime readOffsetDateTime(final ResultSet rs, final String columnName) throws java.sql.SQLException {
        final Timestamp timestamp = rs.getTimestamp(columnName);
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
