package com.mcoo.buildwithease;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;

import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.core.World;
import dev.huskuraft.universal.api.platform.Server;
import dev.huskuraft.universal.api.text.ChatFormatting;
import dev.huskuraft.universal.api.text.Text;
import com.mcoo.buildwithease.building.BuildResult;
import com.mcoo.buildwithease.building.Context;
import com.mcoo.buildwithease.building.StructureBuilder;
import com.mcoo.buildwithease.building.clipboard.Clipboard;
import com.mcoo.buildwithease.building.history.OperationResultStack;
import com.mcoo.buildwithease.building.pattern.Pattern;
import com.mcoo.buildwithease.building.replace.Replace;
import com.mcoo.buildwithease.building.session.BatchBuildSession;
import com.mcoo.buildwithease.building.structure.builder.Structure;
import com.mcoo.buildwithease.networking.packets.player.PlayerBuildPacket;
import com.mcoo.buildwithease.networking.packets.player.PlayerBuildTooltipPacket;

public final class McooBuildwithEaseStructureBuilder extends StructureBuilder {

    private final McooBuildwithEase entrance;

    private final Map<UUID, Context> contexts = new HashMap<>();
    private final Map<UUID, OperationResultStack> undoRedoStacks = new HashMap<>();

    public McooBuildwithEaseStructureBuilder(McooBuildwithEase entrance) {
        this.entrance = entrance;

        getEntrance().getEventRegistry().getPlayerChangeWorldEvent().register(this::onPlayerChangeWorld);
        getEntrance().getEventRegistry().getPlayerRespawnEvent().register(this::onPlayerRespawn);
        getEntrance().getEventRegistry().getPlayerLoggedInEvent().register(this::onPlayerLoggedIn);
        getEntrance().getEventRegistry().getPlayerLoggedOutEvent().register(this::onPlayerLoggedOut);

        getEntrance().getEventRegistry().getServerStartedEvent().register(this::onServerStarted);
        getEntrance().getEventRegistry().getServerStoppedEvent().register(this::onServerStopped);
    }

    public McooBuildwithEase getEntrance() {
        return entrance;
    }


    @Override
    public BuildResult updateContext(Player player, UnaryOperator<Context> updater) {
        return null;
    }

    @Override
    public Context getDefaultContext(Player player) {
        return null;
    }

    @Override
    public Context getContext(Player player) {
        return null;
    }

    @Override
    public Context getContextTraced(Player player) {
        return null;
    }

    @Override
    public Map<UUID, Context> getAllContexts() {
        return Map.of();
    }

    public void onTick() {
    }

    @Override
    public boolean setContext(Player player, Context context) {
        return false;
    }

    @Override
    public boolean setStructure(Player player, Structure structure) {
        return false;
    }

    @Override
    public boolean setClipboard(Player player, Clipboard clipboard) {
        return false;
    }

    @Override
    public boolean setPattern(Player player, Pattern pattern) {
        return false;
    }

    @Override
    public boolean setReplace(Player player, Replace replace) {
        return false;
    }

    @Override
    public void resetAll() {
        contexts.clear();
        undoRedoStacks.clear();
    }

    @Override
    public void onContextReceived(Player player, Context context) {
        if (!checkPermission(player, context)) {
            if (context.isBuildType()) {
                player.sendMessage(McooBuildwithEase.getSystemMessage(Text.text("Your session config is outdated. Please try to rejoin the server!")));
                McooBuildwithEase.LOGGER.warn("%s has an outdated session config".formatted(player.getProfile().getName()));
            }
            return;
        }

        var server = player.getServer();

        if (context.isBuildClientType()) {
            McooBuildwithEase.LOGGER.debug("Received BUILD_CLIENT request from %s".formatted(player.getProfile().getName()));
            return;
        }

        if (context.isBuildType()) {
            McooBuildwithEase.LOGGER.debug("Received BUILD request from %s".formatted(player.getProfile().getName()));
            var result = new BatchBuildSession(getEntrance(), player, context).commit();
            getOperationResultStack(player).push(result);
            getEntrance().getChannel().sendPacket(PlayerBuildTooltipPacket.build(result), player);
            return;
        }

        for (var otherPlayer : server.getPlayerList().getPlayers()) {
            if (otherPlayer.getId().equals(player.getId()) || otherPlayer.getPosition().distance(player.getPosition()) > 128) {
                continue;
            }
            McooBuildwithEase.LOGGER.debug("Received PREVIEW request from %s".formatted(player.getProfile().getName()));
            getEntrance().getChannel().sendPacket(PlayerBuildPacket.by(player, context), otherPlayer);
//            getEntrance().getChannel().sendPacket(PlayerBuildTooltipPacket.build(result), player);
        }
    }

    @Override
    public OperationResultStack getOperationResultStack(Player player) {
        return undoRedoStacks.computeIfAbsent(player.getId(), uuid -> new OperationResultStack());
    }

    @Override
    public void undo(Player player) {
        McooBuildwithEase.LOGGER.debug("Received undo request from %s".formatted(player.getProfile().getName()));
        var stack = getOperationResultStack(player);
        try {
            var result = stack.undo();
            var context = result.getOperation().getContext();

            getEntrance().getChannel().sendPacket(PlayerBuildTooltipPacket.undo(result), player);
            var countText = Text.text("[").append(String.valueOf(stack.undoSize())).append("/").append(String.valueOf(stack.redoSize())).append("]").withStyle(ChatFormatting.WHITE);
            var buildStateText = Text.text("[").append(context.buildState().getDisplayName(context.buildMode())).append("]").withStyle(switch (context.buildState()) {
                case IDLE -> ChatFormatting.RESET;
                case BREAK_BLOCK -> ChatFormatting.RED;
                case PLACE_BLOCK -> ChatFormatting.WHITE;
                case INTERACT_BLOCK -> ChatFormatting.YELLOW;
                case COPY_STRUCTURE -> ChatFormatting.GREEN;
                case PASTE_STRUCTURE -> ChatFormatting.WHITE;
            }).withStyle(ChatFormatting.GOLD);
            var affectedText = Text.text("[").append(String.valueOf(result.getAffectedBlockCount())).append("]").withStyle(ChatFormatting.AQUA);
            player.sendMessage(McooBuildwithEase.getMessage(countText.append(" ").append(Text.translate("mcoo_build_with_ease.message.history.server.undo", buildStateText, affectedText))));
        } catch (EmptyStackException e) {
            getEntrance().getChannel().sendPacket(PlayerBuildTooltipPacket.nothingToUndo(), player);
            var countText = Text.text("[").append(String.valueOf(stack.undoSize())).append("/").append(String.valueOf(stack.redoSize())).append("]").withStyle(ChatFormatting.WHITE);
            player.sendMessage(McooBuildwithEase.getMessage(countText.append(" ").append(Text.translate("mcoo_build_with_ease.history.nothing_to_undo"))));
        }
    }

    @Override
    public void redo(Player player) {
        McooBuildwithEase.LOGGER.debug("Received redo request from %s".formatted(player.getProfile().getName()));
        var stack = getOperationResultStack(player);
        try {
            var result = stack.redo();
            var context = result.getOperation().getContext();

            getEntrance().getChannel().sendPacket(PlayerBuildTooltipPacket.undo(result), player);
            var countText = Text.text("[").append(String.valueOf(stack.undoSize())).append("/").append(String.valueOf(stack.redoSize())).append("]").withStyle(ChatFormatting.WHITE);
            var buildStateText = Text.text("[").append(context.buildState().getDisplayName(context.buildMode())).append("]").withStyle(switch (context.buildState()) {
                case IDLE -> ChatFormatting.RESET;
                case BREAK_BLOCK -> ChatFormatting.RED;
                case PLACE_BLOCK -> ChatFormatting.WHITE;
                case INTERACT_BLOCK -> ChatFormatting.YELLOW;
                case COPY_STRUCTURE -> ChatFormatting.GREEN;
                case PASTE_STRUCTURE -> ChatFormatting.WHITE;
            }).withStyle(ChatFormatting.GOLD);
            var affectedText = Text.text("[").append(String.valueOf(result.getAffectedBlockCount())).append("]").withStyle(ChatFormatting.AQUA);
            player.sendMessage(McooBuildwithEase.getMessage(countText.append(" ").append(Text.translate("mcoo_build_with_ease.message.history.server.redo", buildStateText, affectedText))));
        } catch (EmptyStackException e) {
            getEntrance().getChannel().sendPacket(PlayerBuildTooltipPacket.nothingToRedo(), player);
            var countText = Text.text("[").append(String.valueOf(stack.undoSize())).append("/").append(String.valueOf(stack.redoSize())).append("]").withStyle(ChatFormatting.WHITE);
            player.sendMessage(McooBuildwithEase.getMessage(countText.append(" ").append(Text.translate("mcoo_build_with_ease.history.nothing_to_redo"))));
        }
    }

    private void onPlayerChangeWorld(Player player, World origin, World destination) {
    }

    private void onPlayerRespawn(Player oldPlayer, Player newPlayer, boolean alive) {
    }

    private void onPlayerLoggedIn(Player player) {
    }

    private void onPlayerLoggedOut(Player player) {
    }

    private void onServerStarted(Server server) {
        resetAll();
    }

    private void onServerStopped(Server server) {
        resetAll();
    }

    private boolean checkPermission(Player player, Context context) {
        var config = getEntrance().getSessionManager().getLastSessionConfig().getByPlayer(player);
        return Objects.equals(context.configs().constraintConfig(), config);
    }

}
