package com.mcoo.buildwithease.building;

import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;

import dev.huskuraft.universal.api.core.Player;
import com.mcoo.buildwithease.building.clipboard.Clipboard;
import com.mcoo.buildwithease.building.history.OperationResultStack;
import com.mcoo.buildwithease.building.pattern.Pattern;
import com.mcoo.buildwithease.building.replace.Replace;
import com.mcoo.buildwithease.building.structure.builder.Structure;

public abstract class StructureBuilder {

    public abstract BuildResult updateContext(Player player, UnaryOperator<Context> updater);

    public abstract Context getDefaultContext(Player player);

    public abstract Context getContext(Player player);

    public abstract Context getContextTraced(Player player);

    public abstract Map<UUID, Context> getAllContexts();

    public abstract boolean setContext(Player player, Context context);

    public abstract boolean setStructure(Player player, Structure structure);

    public abstract boolean setClipboard(Player player, Clipboard clipboard);

    public abstract boolean setPattern(Player player, Pattern pattern);

    public abstract boolean setReplace(Player player, Replace replace);

    public abstract void resetAll();

    public final void resetContext(Player player) {
        setContext(player, getDefaultContext(player));
    }

    public final void resetInteractions(Player player) {
        setContext(player, getContext(player).newInteraction());
    }

    public abstract void onContextReceived(Player player, Context context);

    public abstract OperationResultStack getOperationResultStack(Player player);

    public abstract void undo(Player player);

    public abstract void redo(Player player);

}
