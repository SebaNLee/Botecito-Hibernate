package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ClassUser;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ClassUserJdbcDaoTest {

    @Autowired
    private ClassUserDao classUserDao;

    @Autowired
    private @NonNull DataSource dataSource;

    @Test
    public void testCreateUserWhenUserDoesNotExist() {
        // 1. Arrange
        final String username = "[USERNAME]";
        final String password = "[PASSWORD]";
        final String email = "[EMAIL_ADDRESS]";

        // 2. Exercise
        final ClassUser classUser = classUserDao.createClassUser(email, password, username);

        // 3. Assert
        Assertions.assertNotNull(classUser);
        Assertions.assertEquals(username, classUser.getUsername());
        Assertions.assertEquals(password, classUser.getPassword());
        Assertions.assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate(), "class_users"));
    }

    @Test
    public void testCreateUserWhenUserDoesNotExist2() {
        // 1. Arrange
        final String username = "[USERNAME]";
        final String password = "[PASSWORD]";
        final String email = "[EMAIL_ADDRESS]";

        // 2. Exercise
        final ClassUser classUser = classUserDao.createClassUser(email, password, username);

        // 3. Assert
        Assertions.assertNotNull(classUser);
        Assertions.assertEquals(username, classUser.getUsername());
        Assertions.assertEquals(password, classUser.getPassword());
        Assertions.assertEquals(1, JdbcTestUtils.countRowsInTable(jdbcTemplate(), "class_users"));
    }

    private @NonNull JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
