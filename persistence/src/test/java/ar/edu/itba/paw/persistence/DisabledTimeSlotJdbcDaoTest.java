package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.DisabledTimeSlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
public class DisabledTimeSlotJdbcDaoTest {

    @Autowired
    private @NonNull DataSource dataSource;

    @Autowired
    private @NonNull DisabledTimeSlotJdbcDao dao;

    @Test
    public void testInsertAndList() {
        final int itemId = insertItem("owner-insert@x.com", "item-insert");

        final DisabledTimeSlot inserted =
                dao.insert(itemId, LocalDate.of(2026, 5, 1), LocalTime.of(9, 0), LocalTime.of(10, 30));
        Assertions.assertNotNull(inserted.getId());

        final List<DisabledTimeSlot> rows = dao.listByItem(itemId);
        Assertions.assertEquals(1, rows.size());
        Assertions.assertEquals(LocalDate.of(2026, 5, 1), rows.get(0).getSlotDate());
        Assertions.assertEquals(LocalTime.of(9, 0), rows.get(0).getStartTime());
        Assertions.assertEquals(LocalTime.of(10, 30), rows.get(0).getEndTime());
    }

    @Test
    public void testUniqueConstraintRejectsDuplicate() {
        final int itemId = insertItem("owner-dup@x.com", "item-dup");

        dao.insert(itemId, LocalDate.of(2026, 5, 2), LocalTime.of(9, 0), LocalTime.of(10, 0));

        Assertions.assertThrows(
                DataIntegrityViolationException.class,
                () -> dao.insert(itemId, LocalDate.of(2026, 5, 2), LocalTime.of(9, 0), LocalTime.of(10, 0)));
    }

    @Test
    public void testDeleteByIdRequiresMatchingItem() {
        final int itemId = insertItem("owner-del@x.com", "item-del");
        final DisabledTimeSlot slot =
                dao.insert(itemId, LocalDate.of(2026, 5, 3), LocalTime.of(14, 0), LocalTime.of(15, 0));

        Assertions.assertFalse(dao.deleteById(itemId + 999, slot.getId()));
        Assertions.assertTrue(dao.deleteById(itemId, slot.getId()));
        Assertions.assertTrue(dao.listByItem(itemId).isEmpty());
    }

    @Test
    public void testListByItemBetween() {
        final int itemId = insertItem("owner-range@x.com", "item-range");
        dao.insert(itemId, LocalDate.of(2026, 5, 1), LocalTime.of(9, 0), LocalTime.of(10, 0));
        dao.insert(itemId, LocalDate.of(2026, 5, 10), LocalTime.of(9, 0), LocalTime.of(10, 0));
        dao.insert(itemId, LocalDate.of(2026, 6, 1), LocalTime.of(9, 0), LocalTime.of(10, 0));

        final List<DisabledTimeSlot> rows =
                dao.listByItemBetween(itemId, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));
        Assertions.assertEquals(2, rows.size());
    }

    private int insertItem(final String ownerEmail, final String itemTitle) {
        final int ownerId = insertUser(ownerEmail);
        jdbcTemplate()
                .update(
                        "INSERT INTO item (owner_id, type_id, title, price_per_hour, capacity_people, location_option_id)"
                                + " VALUES (?, ?, ?, ?, ?, ?)",
                        ownerId,
                        1,
                        itemTitle,
                        1200,
                        2,
                        1);
        return jdbcTemplate().queryForObject("SELECT id FROM item WHERE title = ?", Integer.class, itemTitle);
    }

    private int insertUser(final String email) {
        jdbcTemplate().update("INSERT INTO users (given_name, last_name, email) VALUES (?, ?, ?)", "A", "A", email);
        return jdbcTemplate().queryForObject("SELECT id FROM users WHERE email = ?", Integer.class, email);
    }

    private @NonNull JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }
}
