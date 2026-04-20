package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;

public class BookingPaymentProof {
    private Integer id;
    private Integer bookingId;
    private Integer uploaderId;
    private String fileName;
    private String contentType;
    private byte[] fileData;
    private OffsetDateTime createdAt;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getBookingId() {
        return bookingId;
    }

    public void setBookingId(final Integer bookingId) {
        this.bookingId = bookingId;
    }

    public Integer getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(final Integer uploaderId) {
        this.uploaderId = uploaderId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public byte[] getFileData() {
        return fileData == null ? null : fileData.clone();
    }

    public void setFileData(final byte[] fileData) {
        this.fileData = fileData == null ? null : fileData.clone();
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt == null ? null : OffsetDateTime.parse(createdAt);
    }
}
