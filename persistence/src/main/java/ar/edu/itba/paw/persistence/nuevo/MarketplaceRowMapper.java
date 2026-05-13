package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.nuevo.AvailabilityWindow;
import ar.edu.itba.paw.models.nuevo.ItemModel;
import ar.edu.itba.paw.models.nuevo.enums.ItemStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.DayOfWeek;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.NonNull;

public final class MarketplaceRowMapper {

    private MarketplaceRowMapper() {}

    public static final @NonNull RowMapper<ItemModel> ITEM_ROW_MAPPER =
            (final ResultSet rs, final int rowNum) -> mapRow(rs);

    public static @NonNull ItemModel mapRow(final ResultSet rs) throws SQLException {
        final ItemModel item = new ItemModel();
        item.setItemId(rs.getInt("id"));
        final Integer versionId = (Integer) rs.getObject("version_id");
        item.setVersionId(versionId != null ? versionId : 0);
        final Integer hostId = (Integer) rs.getObject("host_id");
        item.setHostId(hostId != null ? hostId : 0);
        final String status = rs.getString("status");
        item.setStatus(status == null ? null : ItemStatus.valueOf(status));
        item.setTitle(rs.getString("title"));
        item.setDescription(rs.getString("description"));
        item.setPrice(rs.getBigDecimal("price"));
        item.setCapacity(rs.getInt("capacity"));
        item.setWeight(rs.getInt("weight"));
        item.setDifficulty(rs.getInt("difficulty"));
        item.setLocationId(rs.getInt("location_id"));
        item.setLocation(rs.getString("location_name"));
        final Integer coverImageId = (Integer) rs.getObject("cover_image_id");
        if (coverImageId != null) {
            item.setImages(List.of("/image/" + coverImageId));
        } else {
            item.setImages(List.of("/css/boat-placeholder.svg"));
        }

        item.setAverageRating(rs.getDouble("item_avg_rating"));
        item.setTotalReviews(rs.getInt("item_total_reviews"));

        final String weekday = rs.getString("weekday");
        final Time startTime = rs.getTime("start_time");
        final Time endTime = rs.getTime("end_time");
        if (weekday != null || startTime != null || endTime != null) {
            final AvailabilityWindow window = new AvailabilityWindow();
            if (weekday != null) {
                window.setWeekday(DayOfWeek.valueOf(weekday));
            }
            window.setStartTime(startTime == null ? null : startTime.toLocalTime());
            window.setEndTime(endTime == null ? null : endTime.toLocalTime());
            item.setAvailabilityWindows(List.of(window));
        } else {
            item.setAvailabilityWindows(List.of());
        }
        return item;
    }
}
