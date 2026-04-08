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
import org.springframework.transaction.annotation.Transactional;

@Rollback
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
public class ItemTypeTableJdbcTest {

    @Autowired
    private @NonNull DataSource dataSource;

    @Test
    public void testCreateItemTypeWhenNameIsValid() {
        final JdbcTemplate jdbcTemplate = jdbcTemplate();

        jdbcTemplate.update("INSERT INTO item_type (name) VALUES (?)", "x");

        final Integer count =
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM item_type WHERE name = ?", Integer.class, "x");
        Assertions.assertEquals(1, count);
    }

    @Test
    public void testCreateItemTypeWhenNameIsDuplicated() {
        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update("INSERT INTO item_type (name) VALUES (?)", "Kayak"));
    }

    @Test
    public void testCreateItemTypeWhenNameIsMissing() {
        Assertions.assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate()
                .update("INSERT INTO item_type (name) VALUES (?)", new Object[] {null}));
    }

    private @NonNull JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
