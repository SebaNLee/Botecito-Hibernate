package ar.edu.itba.paw.models.nuevo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImageUpload {
    private final byte[] data;
    private final String contentType;
}
