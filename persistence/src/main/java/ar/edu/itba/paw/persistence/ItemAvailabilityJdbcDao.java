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
    private final boolean postgresDialect;

    @Autowired
    public ItemAvailabilityJdbcDao(final @NonNull DataSource dataSource, final @NonNull JdbcDialect jdbcDialect) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.postgresDialect = jdbcDialect.isPostgres();
    }

    @Override
    public ItemAvailability createItemAvailability(
            final int itemId, final String weekday, final String startTime, final String endTime) {
        final String insertSql = postgresDialect
                ? "INSERT INTO availability (version_id, weekday, start_time, end_time)"
                        + " VALUES ((SELECT MAX(v.id) FROM version v WHERE v.item_id = ?), ?::weekday_enum, ?, ?)"
                        + " RETURNING id"
                : "INSERT INTO availability (version_id, weekday, start_time, end_time)"
                        + " VALUES ((SELECT MAX(v.id) FROM version v WHERE v.item_id = ?), ?, ?, ?)";
        final Integer idValue = postgresDialect
                ? jdbcTemplate.queryForObject(
                        insertSql,
                        Integer.class,
                        itemId,
                        weekday,
                        Time.valueOf(LocalTime.parse(startTime)),
                        Time.valueOf(LocalTime.parse(endTime)))
                : insertAvailability(
                        itemId, DayOfWeek.valueOf(weekday), LocalTime.parse(startTime), LocalTime.parse(endTime));
        final int id = Objects.requireNonNull(idValue, "Could not create availability for item " + itemId);
        return jdbcTemplate
                .query(
                        "SELECT ia.*, i.id AS item_id"
                                + " FROM availability ia"
                                + " JOIN version v ON v.id = ia.version_id"
                                + " JOIN item i ON i.id = v.item_id"
                                + " WHERE ia.id = ?",
                        ItemJdbcRowMappers.ITEM_AVAILABILITY_ROW_MAPPER,
                        id)
                .stream()
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Could not read inserted availability " + id));
    }

    @Override
    public List<ItemAvailability> listAvailabilities() {
        return jdbcTemplate.query(
                "SELECT ia.*, i.id AS item_id"
                        + " FROM availability ia"
                        + " JOIN version v ON v.id = ia.version_id"
                        + " JOIN item i ON i.id = v.item_id"
                        + " ORDER BY ia.id",
                ItemJdbcRowMappers.ITEM_AVAILABILITY_ROW_MAPPER);
    }

    @Override
    public List<ItemAvailability> listAvailabilitiesByItemId(final int itemId) {
        return jdbcTemplate.query(
                "SELECT ia.*, i.id AS item_id"
                        + " FROM availability ia"
                        + " JOIN version v ON v.id = ia.version_id"
                        + " JOIN item i ON i.id = v.item_id"
                        + " WHERE i.id = ? ORDER BY ia.id",
                ItemJdbcRowMappers.ITEM_AVAILABILITY_ROW_MAPPER,
                itemId);
    }

    @Override
    public Optional<ItemAvailability> findNextAvailabilityByItemId(final int itemId) {
        return jdbcTemplate
                .query(
                        "SELECT ia.*, i.id AS item_id"
                                + " FROM availability ia"
                                + " JOIN version v ON v.id = ia.version_id"
                                + " JOIN item i ON i.id = v.item_id"
                                + " WHERE i.id = ? ORDER BY ia.start_time ASC LIMIT 1",
                        ItemJdbcRowMappers.ITEM_AVAILABILITY_ROW_MAPPER,
                        itemId)
                .stream()
                .findAny();
    }

    @Override
    public Integer insertAvailability(
            final int itemId, final DayOfWeek weekday, final LocalTime startTime, final LocalTime endTime) {
        if (postgresDialect) {
            return jdbcTemplate.queryForObject(
                    "INSERT INTO availability (version_id, weekday, start_time, end_time)"
                            + " VALUES ((SELECT MAX(v.id) FROM version v WHERE v.item_id = ?), ?::weekday_enum, ?, ?)"
                            + " RETURNING id",
                    Integer.class,
                    itemId,
                    weekday.name(),
                    Time.valueOf(startTime),
                    Time.valueOf(endTime));
        }
        jdbcTemplate.update(
                "INSERT INTO availability (version_id, weekday, start_time, end_time)"
                        + " VALUES ((SELECT MAX(v.id) FROM version v WHERE v.item_id = ?), ?, ?, ?)",
                itemId,
                weekday.name(),
                Time.valueOf(startTime),
                Time.valueOf(endTime));
        return jdbcTemplate.queryForObject("CALL IDENTITY()", Integer.class);
    }
}
