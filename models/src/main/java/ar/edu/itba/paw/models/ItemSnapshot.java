package ar.edu.itba.paw.models;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemSnapshot extends Item {
    private Integer versionId;
    private Integer bookingId;
    private byte[] coverImageData;
    private OffsetDateTime snapshotCreatedAt;
}
