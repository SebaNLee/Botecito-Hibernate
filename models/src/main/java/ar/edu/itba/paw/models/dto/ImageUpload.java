package ar.edu.itba.paw.models.dto;

import lombok.Getter;

@Getter
public class ImageUpload {

    private final Integer existingImageId;
    private final byte[] data;
    private final String contentType;

    public ImageUpload(final byte[] data, final String contentType) {
        this(null, data, contentType);
    }

    public ImageUpload(final Integer existingImageId, final byte[] data, final String contentType) {
        this.existingImageId = existingImageId;
        this.data = data == null ? null : data.clone();
        this.contentType = contentType;
    }

    public static ImageUpload ofNew(final byte[] data, final String contentType) {
        return new ImageUpload(data, contentType);
    }

    public static ImageUpload ofExisting(final int existingImageId) {
        return new ImageUpload(existingImageId, null, null);
    }

    public boolean isExisting() {
        return existingImageId != null;
    }
}
