package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.models.dto.SelfBlockCreate;
import ar.edu.itba.paw.models.dto.SelfBlockUpdate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class SaveSelfBlocksForm {

    private static final int MIN_RANGE_MINUTES = 120;
    private static final int MIN_SEPARATION_MINUTES = 30;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @NotBlank
    private String changesJson;

    private transient ParsedChanges parsed;

    public List<Integer> deletedBlockIds() {
        return parsedChanges().deletedBlockIds();
    }

    public List<SelfBlockUpdate> updates() {
        return parsedChanges().updates();
    }

    public List<SelfBlockCreate> creates() {
        return parsedChanges().creates();
    }

    @AssertTrue(message = "{publish.availability.format.invalid}")
    public boolean isChangesJsonValid() {
        return parsedChanges().valid();
    }

    @AssertTrue(message = "{publish.availability.end.invalid}")
    public boolean isBlocksEndAfterStart() {
        if (!parsedChanges().valid()) {
            return true;
        }
        for (final BlockRange block : parsedChanges().blocks()) {
            if (block.startTime() == null
                    || block.endTime() == null
                    || !block.endTime().isAfter(block.startTime())) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(message = "{publish.availability.min.duration}")
    public boolean isBlocksMinDuration() {
        if (!parsedChanges().valid()) {
            return true;
        }
        for (final BlockRange block : parsedChanges().blocks()) {
            if (block.startTime() == null
                    || block.endTime() == null
                    || Duration.between(block.startTime(), block.endTime()).toMinutes() < MIN_RANGE_MINUTES) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(message = "{publish.availability.min.separation}")
    public boolean isBlocksMinSeparation() {
        if (!parsedChanges().valid()) {
            return true;
        }
        return blocksHaveMinSeparation(parsedChanges().blocks());
    }

    private ParsedChanges parsedChanges() {
        if (parsed == null) {
            parsed = ParsedChanges.parse(changesJson);
        }
        return parsed;
    }

    private static boolean blocksHaveMinSeparation(final List<BlockRange> blocks) {
        if (blocks.size() < 2) {
            return true;
        }
        final List<BlockRange> sorted = blocks.stream()
                .filter(block -> block.startTime() != null && block.endTime() != null)
                .sorted(Comparator.comparing(BlockRange::startTime))
                .toList();
        LocalTime previousEnd = null;
        for (final BlockRange block : sorted) {
            if (previousEnd != null
                    && Duration.between(previousEnd, block.startTime()).toMinutes() < MIN_SEPARATION_MINUTES) {
                return false;
            }
            previousEnd = block.endTime();
        }
        return true;
    }

    private record BlockRange(LocalTime startTime, LocalTime endTime) {}

    private record ParsedChanges(
            boolean valid,
            List<Integer> deletedBlockIds,
            List<SelfBlockUpdate> updates,
            List<SelfBlockCreate> creates,
            List<BlockRange> blocks) {

        private static ParsedChanges parse(final String rawJson) {
            if (rawJson == null || rawJson.isBlank()) {
                return emptyInvalid();
            }
            final JsonNode root;
            try {
                root = OBJECT_MAPPER.readTree(rawJson);
            } catch (final JsonProcessingException ignored) {
                return emptyInvalid();
            }
            final List<Integer> deletedBlockIds = new ArrayList<>();
            final JsonNode deletes = root.get("deletes");
            if (deletes != null && deletes.isArray()) {
                for (final JsonNode node : deletes) {
                    if (node != null && node.isInt()) {
                        deletedBlockIds.add(node.intValue());
                    }
                }
            }
            final List<SelfBlockUpdate> updates = new ArrayList<>();
            final JsonNode updatesNode = root.get("updates");
            if (updatesNode != null && updatesNode.isArray()) {
                for (final JsonNode node : updatesNode) {
                    if (node == null || !node.isObject()) {
                        continue;
                    }
                    final JsonNode idNode = node.get("id");
                    final JsonNode startNode = node.get("startTime");
                    final JsonNode endNode = node.get("endTime");
                    if (idNode == null
                            || !idNode.isInt()
                            || startNode == null
                            || endNode == null
                            || !startNode.isTextual()
                            || !endNode.isTextual()) {
                        continue;
                    }
                    updates.add(new SelfBlockUpdate(idNode.intValue(), startNode.asText(), endNode.asText()));
                }
            }
            final List<SelfBlockCreate> creates = new ArrayList<>();
            final JsonNode createsNode = root.get("creates");
            if (createsNode != null && createsNode.isArray()) {
                for (final JsonNode node : createsNode) {
                    if (node == null || !node.isObject()) {
                        continue;
                    }
                    final JsonNode startNode = node.get("startTime");
                    final JsonNode endNode = node.get("endTime");
                    if (startNode == null || endNode == null || !startNode.isTextual() || !endNode.isTextual()) {
                        continue;
                    }
                    creates.add(new SelfBlockCreate(startNode.asText(), endNode.asText()));
                }
            }
            final List<BlockRange> blocks = new ArrayList<>();
            final JsonNode blocksNode = root.get("blocks");
            if (blocksNode == null || !blocksNode.isArray()) {
                return emptyInvalid();
            }
            for (final JsonNode node : blocksNode) {
                if (node == null || !node.isObject()) {
                    return emptyInvalid();
                }
                final JsonNode startNode = node.get("startTime");
                final JsonNode endNode = node.get("endTime");
                if (startNode == null || endNode == null || !startNode.isTextual() || !endNode.isTextual()) {
                    return emptyInvalid();
                }
                final BlockRange block = parseBlockRange(startNode.asText(), endNode.asText());
                if (block == null) {
                    return emptyInvalid();
                }
                blocks.add(block);
            }
            return new ParsedChanges(true, deletedBlockIds, updates, creates, blocks);
        }

        private static BlockRange parseBlockRange(final String startTime, final String endTime) {
            try {
                return new BlockRange(LocalTime.parse(startTime), LocalTime.parse(endTime));
            } catch (final DateTimeParseException ignored) {
                return null;
            }
        }

        private static ParsedChanges emptyInvalid() {
            return new ParsedChanges(false, List.of(), List.of(), List.of(), List.of());
        }
    }
}
