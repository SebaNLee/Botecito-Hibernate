package ar.edu.itba.paw.webapp.form;

import ar.edu.itba.paw.models.dto.SelfBlockCreate;
import ar.edu.itba.paw.models.dto.SelfBlockUpdate;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Getter
@Setter
public class SaveSelfBlocksForm {

    private static final int MIN_RANGE_MINUTES = 120;
    private static final int MIN_SEPARATION_MINUTES = 30;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    private List<Integer> deletedBlockIds = new ArrayList<>();

    private List<UpdateBinding> blockUpdates = new ArrayList<>();

    private List<CreateBinding> blockCreates = new ArrayList<>();

    private List<BlockBinding> blocks = new ArrayList<>();

    public void setDeletedBlockIds(final List<Integer> deletedBlockIds) {
        this.deletedBlockIds = deletedBlockIds == null ? new ArrayList<>() : deletedBlockIds;
    }

    public void setBlockUpdates(final List<UpdateBinding> blockUpdates) {
        this.blockUpdates = blockUpdates == null ? new ArrayList<>() : blockUpdates;
    }

    public void setBlockCreates(final List<CreateBinding> blockCreates) {
        this.blockCreates = blockCreates == null ? new ArrayList<>() : blockCreates;
    }

    public void setBlocks(final List<BlockBinding> blocks) {
        this.blocks = blocks == null ? new ArrayList<>() : blocks;
    }

    public List<Integer> deletedBlockIds() {
        return List.copyOf(deletedBlockIds);
    }

    public List<SelfBlockUpdate> updates() {
        final List<SelfBlockUpdate> updates = new ArrayList<>();
        for (final UpdateBinding update : blockUpdates) {
            if (update == null
                    || update.getBookingId() == null
                    || update.getStartTime() == null
                    || update.getEndTime() == null) {
                continue;
            }
            updates.add(new SelfBlockUpdate(
                    update.getBookingId(),
                    update.getStartTime().trim(),
                    update.getEndTime().trim()));
        }
        return List.copyOf(updates);
    }

    public List<SelfBlockCreate> creates() {
        final List<SelfBlockCreate> creates = new ArrayList<>();
        for (final CreateBinding create : blockCreates) {
            if (create == null || create.getStartTime() == null || create.getEndTime() == null) {
                continue;
            }
            creates.add(new SelfBlockCreate(
                    create.getStartTime().trim(), create.getEndTime().trim()));
        }
        return List.copyOf(creates);
    }

    @AssertTrue(message = "{publish.availability.format.invalid}")
    public boolean isBlocksFormatValid() {
        for (final BlockBinding block : blocks) {
            if (block == null || block.getStartTime() == null || block.getEndTime() == null) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(message = "{publish.availability.end.invalid}")
    public boolean isBlocksEndAfterStart() {
        for (final BlockBinding block : completeBlocks()) {
            if (!block.getEndTime().isAfter(block.getStartTime())) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(message = "{publish.availability.min.duration}")
    public boolean isBlocksMinDuration() {
        for (final BlockBinding block : completeBlocks()) {
            if (Duration.between(block.getStartTime(), block.getEndTime()).toMinutes() < MIN_RANGE_MINUTES) {
                return false;
            }
        }
        return true;
    }

    @AssertTrue(message = "{publish.availability.overlap}")
    public boolean isBlocksWithoutOverlap() {
        return blocksAreNonOverlapping(completeBlocks());
    }

    @AssertTrue(message = "{publish.availability.min.separation}")
    public boolean isBlocksMinSeparation() {
        return blocksHaveMinSeparation(completeBlocks());
    }

    private List<BlockBinding> completeBlocks() {
        if (blocks == null || blocks.isEmpty()) {
            return List.of();
        }
        final List<BlockBinding> complete = new ArrayList<>();
        for (final BlockBinding block : blocks) {
            if (block == null || block.getStartTime() == null || block.getEndTime() == null) {
                continue;
            }
            complete.add(block);
        }
        return complete;
    }

    private static boolean blocksAreNonOverlapping(final List<BlockBinding> blocks) {
        if (blocks.size() < 2) {
            return true;
        }
        final List<BlockBinding> sorted = blocks.stream()
                .sorted(Comparator.comparing(BlockBinding::getStartTime))
                .toList();
        LocalTime previousEnd = null;
        for (final BlockBinding block : sorted) {
            if (previousEnd != null && block.getStartTime().isBefore(previousEnd)) {
                return false;
            }
            previousEnd = block.getEndTime();
        }
        return true;
    }

    private static boolean blocksHaveMinSeparation(final List<BlockBinding> blocks) {
        if (blocks.size() < 2) {
            return true;
        }
        final List<BlockBinding> sorted = blocks.stream()
                .sorted(Comparator.comparing(BlockBinding::getStartTime))
                .toList();
        LocalTime previousEnd = null;
        for (final BlockBinding block : sorted) {
            if (previousEnd != null
                    && Duration.between(previousEnd, block.getStartTime()).toMinutes() < MIN_SEPARATION_MINUTES) {
                return false;
            }
            previousEnd = block.getEndTime();
        }
        return true;
    }

    @Getter
    @Setter
    public static class BlockBinding {

        @DateTimeFormat(
                iso = ISO.TIME,
                fallbackPatterns = {"H:mm", "HH:mm"})
        private LocalTime startTime;

        @DateTimeFormat(
                iso = ISO.TIME,
                fallbackPatterns = {"H:mm", "HH:mm"})
        private LocalTime endTime;
    }

    @Getter
    @Setter
    public static class UpdateBinding {
        private Integer bookingId;
        private String startTime;
        private String endTime;
    }

    @Getter
    @Setter
    public static class CreateBinding {
        private String startTime;
        private String endTime;
    }
}
