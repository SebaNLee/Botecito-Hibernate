package ar.edu.itba.paw.services.dto;

public final class PaymentProofUpload {
    private final String fileName;
    private final String contentType;
    private final byte[] fileData;
    private final String guestReply;

    public PaymentProofUpload(
            final String fileName, final String contentType, final byte[] fileData, final String guestReply) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileData = fileData;
        this.guestReply = guestReply;
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

    public String getGuestReply() {
        return guestReply;
    }
}
