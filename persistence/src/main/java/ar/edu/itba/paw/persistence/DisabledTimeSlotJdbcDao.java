package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.models.DisabledTimeSlot;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public class DisabledTimeSlotJdbcDao implements DisabledTimeSlotDao {

    private static final @NonNull RowMapper<DisabledTimeSlot> ROW_MAPPER = (ResultSet rs, int rowNum) -> {
        final DisabledTimeSlot slot = new DisabledTimeSlot();
        slot.setId(rs.getInt("id"));
        slot.setItemId(rs.getInt("item_id"));
        slot.setSlotDate(rs.getDate("slot_date").toLocalDate());
        slot.setStartTime(rs.getTime("start_time").toLocalTime());
        slot.setEndTime(rs.getTime("end_time").toLocalTime());
        slot.setCreatedAt(readOffsetDateTime(rs, "created_at"));
        return slot;
    };

    private final @NonNull JdbcTemplate jdbcTemplate;
    private final @NonNull SimpleJdbcInsert jdbcInsert;

    @Autowired
    public DisabledTimeSlotJdbcDao(final @NonNull DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("disabled_time_slot")
                .usingColumns("item_id", "slot_date", "start_time", "end_time")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public DisabledTimeSlot insert(
            final int itemId, final LocalDate date, final LocalTime startTime, final LocalTime endTime) {
        final Map<String, Object> args = new HashMap<>();
        args.put("item_id", itemId);
        args.put("slot_date", Date.valueOf(date));
        args.put("start_time", Time.valueOf(startTime));
        args.put("end_time", Time.valueOf(endTime));
        final int id = Objects.requireNonNull(jdbcInsert.executeAndReturnKey(args), "Could not insert disabled slot")
                .intValue();
        return jdbcTemplate.query("SELECT * FROM disabled_time_slot WHERE id = ?", ROW_MAPPER, id).stream()
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Could not read inserted disabled slot " + id));
    }

    @Override
    public List<DisabledTimeSlot> listAll() {
        return jdbcTemplate.query(
                "SELECT * FROM disabled_time_slot ORDER BY item_id ASC, slot_date ASC, start_time ASC", ROW_MAPPER);
    }

    @Override
    public List<DisabledTimeSlot> listByItem(final int itemId) {
        return jdbcTemplate.query(
                "SELECT * FROM disabled_time_slot WHERE item_id = ? ORDER BY slot_date ASC, start_time ASC",
                ROW_MAPPER,
                itemId);
    }

    @Override
    public List<DisabledTimeSlot> listByItemBetween(
            final int itemId, final LocalDate fromDate, final LocalDate toDate) {
        return jdbcTemplate.query(
                "SELECT * FROM disabled_time_slot WHERE item_id = ? AND slot_date BETWEEN ? AND ?"
                        + " ORDER BY slot_date ASC, start_time ASC",
                ROW_MAPPER,
                itemId,
                Date.valueOf(fromDate),
                Date.valueOf(toDate));
    }

    @Override
    public boolean deleteById(final int itemId, final int disabledSlotId) {
        return jdbcTemplate.update(
                        "DELETE FROM disabled_time_slot WHERE id = ? AND item_id = ?", disabledSlotId, itemId)
                > 0;
    }

    @Override
    public boolean deleteByRange(
            final int itemId, final LocalDate date, final LocalTime startTime, final LocalTime endTime) {
        return jdbcTemplate.update(
                        "DELETE FROM disabled_time_slot WHERE item_id = ? AND slot_date = ? AND start_time = ? AND end_time = ?",
                        itemId,
                        Date.valueOf(date),
                        Time.valueOf(startTime),
                        Time.valueOf(endTime))
                > 0;
    }

    private static OffsetDateTime readOffsetDateTime(final ResultSet rs, final String column) throws SQLException {
        final Object value = rs.getObject(column);
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
        }
        return OffsetDateTime.parse(value.toString());
    }
}
