package ar.edu.itba.paw.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static ar.edu.itba.paw.persistence.TestUtils.*;

import ar.edu.itba.paw.models.entity.*;
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

        assertNotNull(report.getId());
    }

    @Test
    public void testHasReported() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Boat");
        em.flush();

        Report report = Report.builder()
                .sender(sender)
                .item(item)
                .reason(ReportEnum.INAPPROPRIATE)
                .createdAt(LocalDateTime.now())
                .build();
        reportDao.create(report);
        em.flush();

        assertTrue(reportDao.hasReported(sender.getId(), item.getId()));
    }

    @Test
    public void testDeleteReport() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Boat");
        em.flush();

        Report report = Report.builder()
                .sender(sender)
                .item(item)
                .reason(ReportEnum.INAPPROPRIATE)
                .createdAt(LocalDateTime.now())
                .build();
        reportDao.create(report);
        em.flush();
        int reportId = report.getId();

        reportDao.deleteById(reportId);
        em.flush();
        em.clear();

        assertFalse(reportDao.findById(reportId).isPresent());
    }

    @Test
    public void testDeleteAllByItemId() {
        Item item = insertItem(em, host, ItemStatusEnum.ACTIVE);
        insertVersion(em, item, itemType, location, "Boat");
        em.flush();

        Report report1 = Report.builder().sender(sender).item(item).reason(ReportEnum.INAPPROPRIATE).createdAt(LocalDateTime.now()).build();
        Report report2 = Report.builder().sender(sender).item(item).reason(ReportEnum.SPAM).createdAt(LocalDateTime.now()).build();
        reportDao.create(report1);
        reportDao.create(report2);
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

        Report report = Report.builder().sender(sender).item(item).reason(ReportEnum.INAPPROPRIATE).createdAt(LocalDateTime.now()).build();
        reportDao.create(report);
        em.flush();

        assertTrue(reportDao.findById(report.getId()).isPresent());
    }
}
