package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.ItemAvailability;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class ItemAvailabilityJdbcDao implements ItemAvailabilityDao {

    private final @NonNull JdbcTemplate jdbcTemplate;

    @Autowired
    public ItemAvailabilityJdbcDao(final @NonNull DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public ItemAvailability createItemAvailability(
            final int itemId, final String weekday, final String startTime, final String endTime) {
        final int id = Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                        "INSERT INTO item_availability (item_id, weekday, start_time, end_time)"
                                + " VALUES (?, ?::availability_weekday, ?, ?)"
                                + " RETURNING id",
                        Integer.class,
                        itemId,
                        weekday,
                        Time.valueOf(LocalTime.parse(startTime)),
                        Time.valueOf(LocalTime.parse(endTime))),
                "Could not create availability for item " + itemId);
        return jdbcTemplate
                .query(
                        "SELECT * FROM item_availability WHERE id = ?",
                        ItemJdbcRowMappers.ITEM_AVAILABILITY_ROW_MAPPER,
                        id)
                .stream()
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Could not read inserted availability " + id));
    }

    @Override
    public List<ItemAvailability> listAvailabilities() {
        return jdbcTemplate.query(
                "SELECT * FROM item_availability ORDER BY id", ItemJdbcRowMappers.ITEM_AVAILABILITY_ROW_MAPPER);
    }

    @Override
    public List<ItemAvailability> listAvailabilitiesByItemId(final int itemId) {
        return jdbcTemplate.query(
                "SELECT * FROM item_availability WHERE item_id = ? ORDER BY id",
                ItemJdbcRowMappers.ITEM_AVAILABILITY_ROW_MAPPER,
                itemId);
    }

    @Override
    public Optional<ItemAvailability> findNextAvailabilityByItemId(final int itemId) {
        return jdbcTemplate
                .query(
                        "SELECT * FROM item_availability WHERE item_id = ? ORDER BY start_time ASC LIMIT 1",
                        ItemJdbcRowMappers.ITEM_AVAILABILITY_ROW_MAPPER,
                        itemId)
                .stream()
                .findAny();
    }

    @Override
    public Integer insertAvailability(
            final int itemId, final DayOfWeek weekday, final LocalTime startTime, final LocalTime endTime) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO item_availability (item_id, weekday, start_time, end_time) VALUES (?, ?::availability_weekday, ?, ?) RETURNING id",
                Integer.class,
                itemId,
                weekday.name(),
                Time.valueOf(startTime),
                Time.valueOf(endTime));
    }
}
