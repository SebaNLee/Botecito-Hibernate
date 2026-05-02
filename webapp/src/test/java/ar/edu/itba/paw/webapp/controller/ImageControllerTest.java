package ar.edu.itba.paw.webapp.controller;

import ar.edu.itba.paw.services.ItemService;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class ImageControllerTest {

    @Mock
    private ItemService itemService;

    @Test
    public void testImageByIdReturnsNotFoundWhenImageIsMissing() {
        Mockito.when(itemService.findImageById(10)).thenReturn(Optional.empty());
        final ImageController controller = new ImageController(itemService);

        final ResponseEntity<byte[]> response = controller.imageById(10);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testImageByIdReturnsImageBytesWhenPresent() {
        final byte[] pngBytes = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        Mockito.when(itemService.findImageById(11)).thenReturn(Optional.of(pngBytes));
        final ImageController controller = new ImageController(itemService);

        final ResponseEntity<byte[]> response = controller.imageById(11);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertArrayEquals(pngBytes, response.getBody());
        Assertions.assertEquals(pngBytes.length, response.getHeaders().getContentLength());
    }
}
