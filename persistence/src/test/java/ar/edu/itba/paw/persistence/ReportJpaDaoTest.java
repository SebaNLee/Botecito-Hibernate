package ar.edu.itba.paw.persistence;

import static ar.edu.itba.paw.persistence.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.*;
import java.time.LocalDateTime;
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
public class ReportJpaDaoTest {

    @Autowired
    private ReportDao reportDao;

    @PersistenceContext
    private EntityManager em;

    private Users sender;
    private Users host;
    private Location location;
    private ItemType itemType;

    @BeforeEach
    public void setup() {
        sender = insertUser(em, "Sender", "User", "botecito.user@gmail.com");
        host = insertUser(em, "Host", "User", "botecito.dev@gmail.com");
        location = insertLocation(em, "Portezuelo", "portezuelo");
        itemType = insertItemType(em, "Kayak", "kayak");
        em.flush();
    }

    @Test
    public void testCreateReport() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Boat");
        em.flush();

        Report report = Report.builder()
                .sender(sender)
                .item(item)
                .reason(ReportEnum.INAPPROPRIATE)
                .description("Not ok")
                .createdAt(LocalDateTime.now())
                .build();
        reportDao.create(report);
        em.flush();
        em.clear();

        assertNotNull(report.getId());
        Report persisted = em.find(Report.class, report.getId());
        assertNotNull(persisted);
        assertEquals(ReportEnum.INAPPROPRIATE, persisted.getReason());
        assertEquals("Not ok", persisted.getDescription());
    }

    @Test
    public void testHasReported() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Boat");
        em.flush();
        insertReport(em, sender, item, ReportEnum.INAPPROPRIATE, "Not ok");
        em.flush();
        em.clear();

        Long count = em.createQuery(
                        "SELECT COUNT(r) FROM Report r WHERE r.sender.id = :senderId AND r.item.id = :itemId",
                        Long.class)
                .setParameter("senderId", sender.getId())
                .setParameter("itemId", item.getId())
                .getSingleResult();
        assertEquals(1L, count);
    }

    @Test
    public void testDeleteReport() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Boat");
        em.flush();

        Report report = insertReport(em, sender, item, ReportEnum.INAPPROPRIATE, "Not ok");
        em.flush();
        int reportId = report.getId();

        reportDao.deleteById(reportId);
        em.flush();
        em.clear();

        assertNull(em.find(Report.class, reportId));
    }

    @Test
    public void testDeleteAllByItemId() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Boat");
        em.flush();

        Report report1 = insertReport(em, sender, item, ReportEnum.INAPPROPRIATE, "Not ok");
        Report report2 = insertReport(em, sender, item, ReportEnum.SPAM, "Spam");
        em.flush();
        int itemId = item.getId();

        reportDao.deleteAllByItemId(itemId);
        em.flush();
        em.clear();

        assertNull(em.find(Report.class, report1.getId()));
        assertNull(em.find(Report.class, report2.getId()));
    }

    @Test
    public void testFindReportById() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Boat");
        em.flush();

        Report report = insertReport(em, sender, item, ReportEnum.INAPPROPRIATE, "Not ok");
        em.flush();

        assertTrue(reportDao.findById(report.getId()).isPresent());
    }

    @Test
    public void testSearchReports() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Boat");
        em.flush();

        Report report = insertReport(em, sender, item, ReportEnum.INAPPROPRIATE, "Not ok");
        em.flush();

        PageModel<Report> result = reportDao.searchReports(1, 12, "newest", item);

        assertEquals(1, result.getTotalItems());
        assertEquals(report.getId(), result.getContent().get(0).getId());
    }

    @Test
    public void testCountReports() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Boat");
        em.flush();

        insertReport(em, sender, item, ReportEnum.INAPPROPRIATE, "Not ok");
        insertReport(em, sender, item, ReportEnum.SPAM, "Spam");
        em.flush();

        long count = reportDao.countReports(item);

        assertEquals(2, count);
    }
}
