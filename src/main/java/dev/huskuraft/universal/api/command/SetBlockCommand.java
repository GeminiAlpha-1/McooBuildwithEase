package dev.huskuraft.universal.api.command;

import java.util.Locale;
import java.util.stream.Collectors;

import dev.huskuraft.universal.api.core.BlockPosition;
import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.core.PropertyHolder;

public record SetBlockCommand(
        BlockState blockState,
        BlockPosition blockPosition,
        Mode mode
) implements Command {

    public static final String COMMAND = "setblock";

    @Override
    public String build() {
        return "%s %d %d %d %s %s".formatted(
                COMMAND,
                blockPosition.x(),
                blockPosition.y(),
                blockPosition.z(),
                getPropertiesString(blockState),
                mode.name().toLowerCase(Locale.ROOT)
        );
    }

    public String getPropertiesString(BlockState blockState) {
        return blockState.getItem().getId().getString() + "[" + blockState.getProperties().stream().map(PropertyHolder::getAsString).collect(Collectors.joining(",")) + "]";
    }

    public enum Mode {
        DESTROY,
        KEEP,
        REPLACE
    }

}
