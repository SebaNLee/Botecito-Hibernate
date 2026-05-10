package ar.edu.itba.paw.persistence.nuevo;

import ar.edu.itba.paw.models.nuevo.ItemModel;
import ar.edu.itba.paw.models.nuevo.ItemStatus;
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
        item.setId(rs.getInt("id"));
        final Integer hostId = (Integer) rs.getObject("host_id");
        item.setHostId(hostId == null ? null : hostId.toString());
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
        item.setWeekday(weekday == null ? null : DayOfWeek.valueOf(weekday));
        final Time startTime = rs.getTime("start_time");
        item.setStartTime(startTime == null ? null : startTime.toLocalTime());
        final Time endTime = rs.getTime("end_time");
        item.setEndTime(endTime == null ? null : endTime.toLocalTime());
        return item;
    }
}
