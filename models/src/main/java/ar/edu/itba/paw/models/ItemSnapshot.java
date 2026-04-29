package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;

public class ItemSnapshot extends Item {
    private Integer versionId;
    private Integer bookingId;
    private byte[] coverImageData;
    private OffsetDateTime snapshotCreatedAt;

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(final Integer versionId) {
        this.versionId = versionId;
    }

    public Integer getBookingId() {
        return bookingId;
    }

    public void setBookingId(final Integer bookingId) {
        this.bookingId = bookingId;
    }

    public byte[] getCoverImageData() {
        return coverImageData == null ? null : coverImageData.clone();
    }

    public void setCoverImageData(final byte[] coverImageData) {
        this.coverImageData = coverImageData == null ? null : coverImageData.clone();
    }

    public OffsetDateTime getSnapshotCreatedAt() {
        return snapshotCreatedAt;
    }

    public void setSnapshotCreatedAt(final OffsetDateTime snapshotCreatedAt) {
        this.snapshotCreatedAt = snapshotCreatedAt;
    }

    public void setSnapshotCreatedAt(final String snapshotCreatedAt) {
        this.snapshotCreatedAt = snapshotCreatedAt == null ? null : OffsetDateTime.parse(snapshotCreatedAt);
    }
}
