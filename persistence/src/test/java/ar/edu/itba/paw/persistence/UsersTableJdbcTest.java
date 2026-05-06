package ar.edu.itba.paw.persistence;

import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class UsersTableJdbcTest {

    @Autowired
    private @NonNull DataSource dataSource;

    @Test
    public void testCreateUserWhenDataIsValid() {
        final JdbcTemplate jdbcTemplate = jdbcTemplate();
        final String email = "a@a.com";

        jdbcTemplate.update("INSERT INTO \"user\" (first_name, last_name, email) VALUES (?, ?, ?)", "A", "A", email);

        Assertions.assertEquals(1, JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "\"user\"", "email = 'a@a.com'"));
    }

    @Test
    public void testCreateUserWhenEmailIsMissing() {
        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update("INSERT INTO \"user\" (first_name, last_name) VALUES (?, ?)", "A", "A"));
    }

    @Test
    public void testCreateUserWhenIdIsDuplicated() {
        final JdbcTemplate jdbcTemplate = jdbcTemplate();

        jdbcTemplate.update(
                "INSERT INTO \"user\" (id, first_name, last_name, email) VALUES (?, ?, ?, ?)",
                500,
                "A",
                "A",
                "a@a.com");

        Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "INSERT INTO \"user\" (id, first_name, last_name, email) VALUES (?, ?, ?, ?)",
                        500,
                        "B",
                        "B",
                        "b@b.com"));
    }

    @Test
    public void testCreateUserWhenPreferredLanguageIsInvalid() {
        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update(
                        "INSERT INTO \"user\" (first_name, last_name, email, language) VALUES (?, ?, ?, ?)",
                        "A",
                        "A",
                        "a@a.com",
                        "pt"));
    }

    @Test
    public void testAdminDebugUserWasMigratedToGivenAndLastName() {
        final JdbcTemplate jdbcTemplate = jdbcTemplate();
        final String givenName = jdbcTemplate.queryForObject(
                "SELECT first_name FROM \"user\" WHERE email = 'botecito.dev@gmail.com'", String.class);
        final String lastName = jdbcTemplate.queryForObject(
                "SELECT last_name FROM \"user\" WHERE email = 'botecito.dev@gmail.com'", String.class);

        Assertions.assertEquals("Admin", givenName);
        Assertions.assertEquals("Botecito", lastName);
    }

    private @NonNull JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
