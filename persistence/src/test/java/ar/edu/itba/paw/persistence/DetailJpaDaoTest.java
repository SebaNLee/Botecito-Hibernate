package ar.edu.itba.paw.persistence;

import static ar.edu.itba.paw.persistence.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.paw.models.entity.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfiguration.class)
@Transactional
public class DetailJpaDaoTest {

    @Autowired
    private DetailDao detailDao;

    @PersistenceContext
    private EntityManager em;

    private Users host;
    private Users guest;
    private Location location;
    private ItemType itemType;
    private Item item;
    private Version version;

    @BeforeEach
    public void setup() {
        host = insertUser(em, "Host", "User", "botecito.dev@gmail.com");
        guest = insertUser(em, "Guest", "User", "botecito.user@gmail.com");
        location = insertLocation(em, "Portezuelo", "portezuelo");
        itemType = insertItemType(em, "Kayak", "kayak");
        item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        version = insertVersion(em, item, itemType, location, "Test Boat");
        em.flush();
    }

    @Test
    public void testGetItemDetailNoVersion() {
        Item itemNoVersion = insertItem(em, host, ItemStatusEnum.ACTIVE);
        em.flush();

        Optional<Item> result = detailDao.getItemDetail(itemNoVersion.getId());

        assertFalse(result.isPresent());
    }

    @Test
    public void testGetItemDetailReturnsItem() {
        Optional<Item> result = detailDao.getItemDetail(item.getId());

        assertTrue(result.isPresent());
        assertEquals(item.getId(), result.get().getId());
    }

    @Test
    public void testGetItemDetailLatestVersion() {
        insertVersion(
                em,
                item,
                itemType,
                location,
                "Old Boat",
                BigDecimal.valueOf(100),
                4,
                200,
                2,
                LocalDateTime.now().minusDays(10));
        em.flush();

        Optional<Item> result = detailDao.getItemDetail(item.getId());

        assertTrue(result.isPresent());
        assertEquals(version.getId(), result.get().getLatestVersion().getId());
    }

    @Test
    public void testGetItemDetailWithReview() {
        Booking booking = insertBooking(em, version, guest, BookingStatusEnum.FINISHED);
        insertReview(em, booking, guest, TargetEnum.ITEM, 4.5, "Botecito mejor app");
        em.flush();

        Optional<Item> result = detailDao.getItemDetail(item.getId());

        assertTrue(result.isPresent());
    }
}
