package ar.edu.itba.paw.services.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class GalleryImageUpload {
    private final String fileName;
    private final String contentType;
    private final byte[] fileData;
}
