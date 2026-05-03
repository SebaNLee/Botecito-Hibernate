package ar.edu.itba.paw.services.dto;

public final class GalleryImageUpload {
    private final String fileName;
    private final String contentType;
    private final byte[] fileData;

    public GalleryImageUpload(final String fileName, final String contentType, final byte[] fileData) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileData = fileData;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getFileData() {
        return fileData;
    }
}
