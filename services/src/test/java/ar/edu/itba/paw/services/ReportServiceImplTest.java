package ar.edu.itba.paw.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ar.edu.itba.paw.models.entity.Item;
import ar.edu.itba.paw.models.entity.ItemStatusEnum;
import ar.edu.itba.paw.models.entity.Report;
import ar.edu.itba.paw.models.entity.ReportEnum;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;
import ar.edu.itba.paw.models.exceptions.ReportAlreadyExistsException;
import ar.edu.itba.paw.models.exceptions.ReportNotFoundException;
import ar.edu.itba.paw.persistence.ReportDao;
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

    private static Item activeItem(final int hostId) {
        Users host = new Users();
        host.setId(hostId);
        return Item.builder()
                .id(ITEM_ID)
                .host(host)
                .status(ItemStatusEnum.ACTIVE)
                .build();
    }

    private static Users sender() {
        Users user = new Users();
        user.setId(SENDER_ID);
        user.setEmail("botecito.dev@gmail.com");
        user.setFirstName("Sender");
        user.setLastName("Test");
        return user;
    }
}
