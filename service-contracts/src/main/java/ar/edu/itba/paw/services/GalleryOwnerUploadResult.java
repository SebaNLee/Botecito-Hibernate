package ar.edu.itba.paw.services;

import java.util.Objects;

public final class GalleryOwnerUploadResult {

    private final GalleryOwnerUploadStatus status;
    private final Integer newImageId;

    private GalleryOwnerUploadResult(final GalleryOwnerUploadStatus status, final Integer newImageId) {
        this.status = Objects.requireNonNull(status);
        this.newImageId = newImageId;
    }

    public static GalleryOwnerUploadResult success(final int newImageId) {
        return new GalleryOwnerUploadResult(GalleryOwnerUploadStatus.SUCCESS, newImageId);
    }

    public static GalleryOwnerUploadResult failure(final GalleryOwnerUploadStatus status) {
        if (status == GalleryOwnerUploadStatus.SUCCESS) {
            throw new IllegalArgumentException("status");
        }
        return new GalleryOwnerUploadResult(status, null);
    }

    public GalleryOwnerUploadStatus getStatus() {
        return status;
    }

    public Integer getNewImageId() {
        return newImageId;
    }
}
