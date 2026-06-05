package ar.edu.itba.paw.persistence;

import static ar.edu.itba.paw.persistence.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.paw.models.entity.ItemType;
import ar.edu.itba.paw.models.entity.Location;
import java.util.List;
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
public class SelectorsJpaDaoTest {

    @Autowired
    private SelectorsDao selectorsDao;

    @PersistenceContext
    private EntityManager em;

    private Location locationOne;
    private Location locationTwo;
    private ItemType typeOne;
    private ItemType typeTwo;

    @BeforeEach
    public void setup() {
        locationOne = insertLocation(em, "Portezuelo", "portezuelo");
        locationTwo = insertLocation(em, "Los Castores", "los-castores");
        typeOne = insertItemType(em, "Kayak", "kayak");
        typeTwo = insertItemType(em, "Paddle", "paddle");
        em.flush();
    }

    @Test
    public void testGetLocationOptions() {
        List<Location> locations = selectorsDao.getLocationOptions();

        assertEquals(2, locations.size());
    }

    @Test
    public void testGetLocationOptionsById() {
        List<Location> locations = selectorsDao.getLocationOptions();

        assertTrue(locations.get(0).getId() <= locations.get(1).getId());
    }

    @Test
    public void testGetLocationOptionsFields() {
        List<Location> locations = selectorsDao.getLocationOptions();

        Location found = locations.stream()
                .filter(l -> l.getId().equals(locationOne.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals("Portezuelo", found.getName());
        assertEquals("portezuelo", found.getSlug());
    }

    @Test
    public void testGetItemTypeOptions() {
        List<ItemType> types = selectorsDao.getItemTypeOptions();

        assertEquals(2, types.size());
    }

    @Test
    public void testGetItemTypeOptionsByName() {
        List<ItemType> types = selectorsDao.getItemTypeOptions();

        assertEquals("Kayak", types.get(0).getName());
        assertEquals("Paddle", types.get(1).getName());
    }

    @Test
    public void testGetItemTypeOptionsFields() {
        List<ItemType> types = selectorsDao.getItemTypeOptions();

        ItemType found = types.stream()
                .filter(t -> t.getId().equals(typeOne.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals("Kayak", found.getName());
        assertEquals("kayak", found.getSlug());
    }
}
