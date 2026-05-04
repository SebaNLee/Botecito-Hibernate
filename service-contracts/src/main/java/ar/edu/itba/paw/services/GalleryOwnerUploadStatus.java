package ar.edu.itba.paw.services;

/** Outcome of an owner gallery image upload attempt (see {@link ItemService#uploadGalleryImage}). */
public enum GalleryOwnerUploadStatus {
    SUCCESS,
    NOT_OWNER,
    EMPTY_FILE,
    INVALID_CONTENT_TYPE,
    FILE_TOO_LARGE,
    GALLERY_FULL
}
