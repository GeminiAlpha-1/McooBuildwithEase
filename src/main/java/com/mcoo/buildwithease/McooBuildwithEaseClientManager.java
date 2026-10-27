package com.mcoo.buildwithease;

import java.util.Stack;

import dev.huskuraft.universal.api.core.Direction;
import dev.huskuraft.universal.api.core.Interaction;
import dev.huskuraft.universal.api.core.InteractionHand;
import dev.huskuraft.universal.api.core.InteractionType;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.events.EventResult;
import dev.huskuraft.universal.api.events.input.KeyRegistry;
import dev.huskuraft.universal.api.events.lifecycle.ClientTick;
import dev.huskuraft.universal.api.gui.Screen;
import dev.huskuraft.universal.api.input.InputKey;
import dev.huskuraft.universal.api.input.Keys;
import dev.huskuraft.universal.api.input.OptionKeys;
import dev.huskuraft.universal.api.platform.Client;
import dev.huskuraft.universal.api.platform.ClientManager;
import dev.huskuraft.universal.api.platform.Platform;
import dev.huskuraft.universal.api.renderer.Renderer;
import dev.huskuraft.universal.api.text.ChatFormatting;
import dev.huskuraft.universal.api.text.Text;
import com.mcoo.buildwithease.building.clipboard.SnapshotTransform;
import com.mcoo.buildwithease.renderer.opertaion.OperationsRenderer;
import com.mcoo.buildwithease.renderer.outliner.OutlineRenderer;
import com.mcoo.buildwithease.renderer.pattern.PatternRenderer;
import com.mcoo.buildwithease.renderer.tooltip.TooltipRenderer;
import com.mcoo.buildwithease.screen.clipboard.McooBuildwithEaseClipboardScreen;
import com.mcoo.buildwithease.screen.pattern.McooBuildwithEasePatternScreen;
import com.mcoo.buildwithease.screen.settings.McooBuildwithEaseSettingsScreen;
import com.mcoo.buildwithease.screen.structure.McooBuildwithEaseStructureScreen;
import com.mcoo.buildwithease.screen.test.McooBuildwithEaseTestScreen;

public final class McooBuildwithEaseClientManager implements ClientManager {

    private final Stack<Screen> screenStack = new Stack<>();

    private final McooBuildwithEaseClient entrance;
    private final TooltipRenderer tooltipRenderer;

    private final OperationsRenderer operationsRenderer;
    private final OutlineRenderer outlineRenderer;
    private final PatternRenderer patternRenderer;

    private Client client;

    private int interactionCooldown = 0;

    public McooBuildwithEaseClientManager(McooBuildwithEaseClient entrance) {
        this.entrance = entrance;
        this.tooltipRenderer = new TooltipRenderer(entrance);

        this.operationsRenderer = new OperationsRenderer(entrance);
        this.outlineRenderer = new OutlineRenderer();
        this.patternRenderer = new PatternRenderer(entrance);

        getEntrance().getEventRegistry().getRegisterKeysEvent().register(this::onRegisterKeys);
        getEntrance().getEventRegistry().getKeyInputEvent().register(this::onKeyInput);
        getEntrance().getEventRegistry().getInteractionInputEvent().register(this::onInteractionInput);

        getEntrance().getEventRegistry().getClientStartEvent().register(this::onClientStart);
        getEntrance().getEventRegistry().getClientTickEvent().register(this::onClientTick);

        getEntrance().getEventRegistry().getRenderGuiEvent().register(this::onRenderGui);
        getEntrance().getEventRegistry().getRenderWorldEvent().register(this::onRenderEnd);
    }

    private McooBuildwithEaseClient getEntrance() {
        return entrance;
    }

    private Player getPlayer() {
        return getEntrance().getClient().getPlayer();
    }

    @Override
    public Client getRunningClient() {
        return client;
    }

    @Override
    public void setRunningClient(Client client) {
        this.client = client;
    }

    public OperationsRenderer getOperationsRenderer() {
        return operationsRenderer;
    }

    public OutlineRenderer getOutlineRenderer() {
        return outlineRenderer;
    }

    public PatternRenderer getPatternRenderer() {
        return patternRenderer;
    }

    @Override
    public void pushScreen(Screen screen) {
        if (screen == null) {
            screenStack.clear();
        } else {
            screenStack.push(getRunningClient().getPanel());
        }
        getRunningClient().setPanel(screen);
    }

    @Override
    public void popScreen(Screen screen) {
        if (getRunningClient().getPanel() != screen) {
            return;
        }
        if (screenStack.isEmpty()) {
            getRunningClient().setPanel(null);
        } else {
            getRunningClient().setPanel(screenStack.pop());
        }
    }

    public TooltipRenderer getTooltipRenderer() {
        return tooltipRenderer;
    }

    private void tickCooldown() {
        if (OptionKeys.KEY_ATTACK.getKeyBinding().isDown() || OptionKeys.KEY_USE.getKeyBinding().isDown() || OptionKeys.KEY_PICK_ITEM.getKeyBinding().isDown()) {
            return;
        }
        this.interactionCooldown = Math.max(0, this.interactionCooldown - 1);
    }

    private boolean isInteractionCooldown() {
        return this.interactionCooldown == 0;
    }

    private void setInteractionCooldown(int tick) {
        this.interactionCooldown = tick; // for single build speed
    }

    private void resetInteractionCooldown() {
        setInteractionCooldown(1);
    }


    public void onRegisterKeys(KeyRegistry keyRegistry) {
        for (var key : McooBuildwithEaseKeys.values()) {
            keyRegistry.register(key);
        }
    }

    public void onKeyInput(InputKey key) {

        if (getRunningClient() == null) {
            return;
        }

        if (getRunningClient().getPlayer() == null) {
            return;
        }

        if (Keys.KEY_ESCAPE.isDown()) {
            getEntrance().getStructureBuilder().resetInteractions(getRunningClient().getPlayer());
        }

        // 【交互变更】轮盘改成「按一下打开、再按一下关闭」的切换式（见 AbstractWheelScreen#onAnimateTick）。
        // 这里只在「轮盘没开」的时候消费按键：轮盘开着时要把这次点击留给轮盘自己处理（用来关闭），
        // 否则第二次按键会被这里吃掉，轮盘就再也关不掉了。
        if (!(getRunningClient().getPanel() instanceof McooBuildwithEaseStructureScreen)) {
            if (McooBuildwithEaseKeys.BUILD_MODE_RADIAL.getKeyBinding().consumeClick()) {
                new McooBuildwithEaseStructureScreen(getEntrance(), McooBuildwithEaseKeys.BUILD_MODE_RADIAL.getKeyBinding()).attach();
            }
        }
        if (McooBuildwithEaseKeys.UNDO.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            getEntrance().getStructureBuilder().undo(getRunningClient().getPlayer());
        }
        if (McooBuildwithEaseKeys.REDO.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            getEntrance().getStructureBuilder().redo(getRunningClient().getPlayer());
        }
        if (McooBuildwithEaseKeys.SETTINGS.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            new McooBuildwithEaseSettingsScreen(getEntrance()).attach();
        }
        McooBuildwithEaseDebug.openScreenIfRequested(this);
        if (McooBuildwithEaseKeys.TOGGLE_CLIPBOARD.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            getEntrance().getStructureBuilder().setClipboard(getRunningClient().getPlayer(), getEntrance().getStructureBuilder().getContext(getRunningClient().getPlayer()).clipboard().toggled());
            getEntrance().getClient().getPlayer().sendMessage(McooBuildwithEase.getSystemMessage(Text.translate("mcoo_build_with_ease.message.building.server.toggle_clipboard", getEntrance().getStructureBuilder().getContext(getRunningClient().getPlayer()).clipboard().getNameText().withStyle(ChatFormatting.GOLD))));
        }
        if (McooBuildwithEaseKeys.TOGGLE_PATTERN.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            getEntrance().getStructureBuilder().setPattern(getRunningClient().getPlayer(), getEntrance().getStructureBuilder().getContext(getRunningClient().getPlayer()).pattern().toggled());
            getEntrance().getClient().getPlayer().sendMessage(McooBuildwithEase.getSystemMessage(Text.translate("mcoo_build_with_ease.message.building.server.toggle_pattern", getEntrance().getStructureBuilder().getContext(getRunningClient().getPlayer()).pattern().getNameText().withStyle(ChatFormatting.GOLD))));
        }
        if (McooBuildwithEaseKeys.TOGGLE_REPLACE.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            getEntrance().getStructureBuilder().setReplace(getRunningClient().getPlayer(), getEntrance().getStructureBuilder().getContext(getRunningClient().getPlayer()).replace().next());
            getEntrance().getClient().getPlayer().sendMessage(McooBuildwithEase.getSystemMessage(Text.translate("mcoo_build_with_ease.message.building.server.toggle_replace", getEntrance().getStructureBuilder().getContext(getRunningClient().getPlayer()).replace().getNameText().withStyle(ChatFormatting.GOLD))));
        }

        if (McooBuildwithEaseKeys.ROTATE_X.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.ROTATE_X);
        }

        if (McooBuildwithEaseKeys.ROTATE_Y.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.ROTATE_Y);
        }

        if (McooBuildwithEaseKeys.ROTATE_Z.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.ROTATE_Z);
        }

        if (McooBuildwithEaseKeys.MOVE_BACKWARD.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            switch (Direction.fromYRot(getPlayer().getYRot())) {
                case NORTH ->
                        getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.INCREASE_Z);
                case SOUTH ->
                        getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.DECREASE_Z);
                case WEST ->
                        getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.INCREASE_X);
                case EAST ->
                        getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.DECREASE_X);
            }
        }

        if (McooBuildwithEaseKeys.MOVE_FORWARD.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            switch (Direction.fromYRot(getPlayer().getYRot())) {
                case NORTH ->
                        getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.DECREASE_Z);
                case SOUTH ->
                        getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.INCREASE_Z);
                case WEST ->
                        getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.DECREASE_X);
                case EAST ->
                        getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.INCREASE_X);
            }
        }

        if (McooBuildwithEaseKeys.MOVE_LEFT.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            switch (Direction.fromYRot(getPlayer().getYRot())) {
                case NORTH ->
                        getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.DECREASE_X);
                case SOUTH ->
                        getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.INCREASE_X);
                case WEST ->
                        getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.INCREASE_Z);
                case EAST ->
                        getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.DECREASE_Z);
            }
        }

        if (McooBuildwithEaseKeys.MOVE_RIGHT.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            switch (Direction.fromYRot(getPlayer().getYRot())) {
                case NORTH ->
                        getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.INCREASE_X);
                case SOUTH ->
                        getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.DECREASE_X);
                case WEST ->
                        getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.DECREASE_Z);
                case EAST ->
                        getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.INCREASE_Z);
            }
        }

        if (McooBuildwithEaseKeys.MOVE_UP.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.INCREASE_Y);
        }

        if (McooBuildwithEaseKeys.MOVE_DOWN.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.DECREASE_Y);
        }

        if (McooBuildwithEaseKeys.MIRROR_X.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.MIRROR_X);
        }

        if (McooBuildwithEaseKeys.MIRROR_Y.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.MIRROR_Y);
        }

        if (McooBuildwithEaseKeys.MIRROR_Z.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            getEntrance().getStructureBuilder().updateClipboard(getPlayer(), SnapshotTransform.MIRROR_Z);
        }

        if (McooBuildwithEaseKeys.EDIT_CLIPBOARD.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            new McooBuildwithEaseClipboardScreen(getEntrance()).attach();
        }

        if (McooBuildwithEaseKeys.EDIT_PATTERN.getKeyBinding().consumeClick()) {
            getEntrance().getClient().getSoundManager().playButtonClickSound();
            new McooBuildwithEasePatternScreen(getEntrance()).attach();
        }

//        if (McooBuildwithEaseKeys.EDIT_REPLACE.getKeyBinding().consumeClick()) {
//            getEntrance().getClient().getSoundManager().playButtonClickSound();
//        }

        if (Platform.getInstance().isDevelopment()) {
            if (Keys.KEY_LEFT_CONTROL.isDown() && Keys.KEY_ENTER.isDown()) {
                getEntrance().getClient().getSoundManager().playButtonClickSound();
                new McooBuildwithEaseTestScreen(getEntrance()).attach();
            }
        }
    }

    public EventResult onInteractionInput(InteractionType type, InteractionHand hand) {

        if (getEntrance().getStructureBuilder().getContext(getRunningClient().getPlayer()).isDisabled()) {
            return EventResult.pass();
        }

        if (!isInteractionCooldown()) {
            return EventResult.interruptFalse();
        } else {
            resetInteractionCooldown();
        }

        var interaction = getEntrance().getClient().getLastInteraction();
        if (interaction != null && interaction.getTarget() == Interaction.Target.ENTITY) {
            return EventResult.interruptFalse();
        }

        return switch (type) {
            case ATTACK, USE_ITEM -> {
                yield getEntrance().getStructureBuilder().onPlayerInteract(getRunningClient().getPlayer(), type, hand);
            }
            case UNKNOWN -> EventResult.pass();
        };

    }

    public synchronized void onClientStart(Client client) {
        setRunningClient(client);
    }

    public void onClientTick(Client client, ClientTick.Phase phase) {
        switch (phase) {
            case START -> {
                tickCooldown();

                tooltipRenderer.tick();

                operationsRenderer.tick();
                outlineRenderer.tick();
                patternRenderer.tick();
            }
            case END -> {
            }
        }
    }

    public void onRenderGui(Renderer renderer, float deltaTick) {
        if (getRunningClient().getPanel() != null && !(getRunningClient().getPanel() instanceof McooBuildwithEaseStructureScreen)) {
            return;
        }

        getTooltipRenderer().renderGuiOverlay(renderer, deltaTick);
    }

    public void onRenderEnd(Renderer renderer, float deltaTick) {
        patternRenderer.render(renderer, deltaTick);
        outlineRenderer.render(renderer, deltaTick);
        operationsRenderer.render(renderer, deltaTick);
    }
}
