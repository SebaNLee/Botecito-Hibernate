package ar.edu.itba.paw.services;

import static ar.edu.itba.paw.services.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.dto.PageModel;
import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.Report;
import ar.edu.itba.paw.models.entity.ReportEnum;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.models.exceptions.ReportAlreadyExistsException;
import ar.edu.itba.paw.models.exceptions.ReportNotFoundException;
import ar.edu.itba.paw.persistence.ReportDao;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReportServiceImplTest {

    private static final int ITEM_ID = 1;
    private static final int SENDER_ID = 10;
    private static final int HOST_ID = 20;
    private static final int REPORT_ID = 100;

    @Mock
    private ReportDao reportDao;

    @Mock
    private ItemService itemService;

    @Mock
    private UserService userService;

    @Mock
    private MailService mailService;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private ReportServiceImpl reportService;

    @Test
    public void createReportSucceeds() {
        when(itemService.findItemById(ITEM_ID)).thenReturn(activeItem(HOST_ID));
        when(userService.findById(SENDER_ID)).thenReturn(Optional.of(sender()));
        when(reportDao.hasReported(SENDER_ID, ITEM_ID)).thenReturn(false);

        assertDoesNotThrow(() -> reportService.createReport(ITEM_ID, SENDER_ID, ReportEnum.SPAM, "test desc"));
    }

    @Test
    public void createReportThrowsWhenAlreadyReported() {
        when(itemService.findItemById(ITEM_ID)).thenReturn(activeItem(HOST_ID));
        when(userService.findById(SENDER_ID)).thenReturn(Optional.of(sender()));
        when(reportDao.hasReported(SENDER_ID, ITEM_ID)).thenReturn(true);

        assertThrows(
                ReportAlreadyExistsException.class,
                () -> reportService.createReport(ITEM_ID, SENDER_ID, ReportEnum.SPAM, null));
    }

    @Test
    public void findByIdReturnsReport() {
        Version version = Version.builder().title("Test Title").build();
        Item item = Item.builder().id(ITEM_ID).latestVersion(version).build();
        Report report = Report.builder().id(REPORT_ID).item(item).build();
        when(reportDao.findById(REPORT_ID)).thenReturn(Optional.of(report));

        Report result = reportService.findById(REPORT_ID);

        assertNotNull(result);
        assertEquals(REPORT_ID, result.getId());
    }

    @Test
    public void findByIdThrowsWhenNotFound() {
        when(reportDao.findById(REPORT_ID)).thenReturn(Optional.empty());

        assertThrows(ReportNotFoundException.class, () -> reportService.findById(REPORT_ID));
    }

    @Test
    public void dismissReportSucceeds() {
        Users sender = sender();
        Version version = Version.builder().title("Test Title").build();
        Item item = Item.builder().id(ITEM_ID).latestVersion(version).build();
        Report report = Report.builder().id(REPORT_ID).item(item).sender(sender).build();
        when(reportDao.findById(REPORT_ID)).thenReturn(Optional.of(report));

        assertDoesNotThrow(() -> reportService.dismissReport(REPORT_ID));
    }

    @Test
    public void dismissReportThrowsWhenNotFound() {
        when(reportDao.findById(REPORT_ID)).thenReturn(Optional.empty());

        assertThrows(ReportNotFoundException.class, () -> reportService.dismissReport(REPORT_ID));
    }

    @Test
    public void hasReportedReturnsTrue() {
        when(reportDao.hasReported(SENDER_ID, ITEM_ID)).thenReturn(true);

        assertTrue(reportService.hasReported(SENDER_ID, ITEM_ID));
    }

    @Test
    public void searchReportsReturnsResult() {
        var expected = new PageModel<Report>(List.of(), 1, 12, 0L);
        when(reportDao.searchReports(1, 10, "newest")).thenReturn(expected);

        var result = reportService.searchReports(1, 10, "newest");

        assertEquals(expected, result);
    }

    @Test
    public void deletePublicationForReportSucceeds() {
        var host = user(HOST_ID, "host@mail.com", "Host", "User");
        var version = Version.builder().title("Item Title").build();
        var item = Item.builder().id(ITEM_ID).host(host).latestVersion(version).build();
        var report = Report.builder()
                .id(REPORT_ID)
                .item(item)
                .reason(ReportEnum.SPAM)
                .build();
        when(reportDao.findById(REPORT_ID)).thenReturn(Optional.of(report));
        when(reportDao.countReports(item)).thenReturn(0L);

        assertDoesNotThrow(() -> reportService.deletePublicationForReport(REPORT_ID));
    }

    @Test
    public void deleteAllByItemIdSucceeds() {
        assertDoesNotThrow(() -> reportService.deleteAllByItemId(ITEM_ID));
    }

    private static Item activeItem(final int hostId) {
        return item(ITEM_ID, user(hostId), ItemStatusEnum.ACTIVE);
    }

    private static Users sender() {
        return user(SENDER_ID, "botecito.dev@gmail.com", "Sender", "Test");
    }
}
