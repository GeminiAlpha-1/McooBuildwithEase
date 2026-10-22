package com.mcoo.buildwithease.screen.grid;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import com.mcoo.buildwithease.building.Option;
import dev.huskuraft.universal.api.core.AxisDirection;
import dev.huskuraft.universal.api.core.Player;
import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.gui.AbstractScreen;
import dev.huskuraft.universal.api.gui.tooltip.TooltipHelper;
import dev.huskuraft.universal.api.input.KeyBinding;
import dev.huskuraft.universal.api.math.MathUtils;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.renderer.RenderLayers;
import dev.huskuraft.universal.api.renderer.Renderer;
import dev.huskuraft.universal.api.text.ChatFormatting;
import dev.huskuraft.universal.api.text.Text;
import dev.huskuraft.universal.api.utils.ColorUtils;

/**
 * 网格界面的公共实现：中间是**方块按钮矩阵**，左右两侧是功能按钮组。
 * <p>
 * 【为什么不是轮盘】轮盘的扇形在 26.2 只能靠仿射矩阵近似拼出来（GUI 没有多边形填充），
 * 相邻小块的共享边会错开 → 扇形上出现细划痕、彩色段上出现黑点；
 * 而且轮盘再塞几个功能就会挤成一圈，点不准。
 * 改成方阵之后：每个格子都是轴对齐矩形（画得准、没有锯齿）、
 * 点选目标大、想加功能直接往后排就行。
 * <p>
 * 格子 = 一个灰色方块 + 中间的图标 + **底部的分组色条**（色条颜色 = 功能分组颜色，
 * 比如立方是橙色、斜面是紫色、圆柱是绿色）。
 */
public abstract class AbstractGridScreen<S, B> extends AbstractScreen {

    public static final int ANIMATION_OFFSET_Y = 12;
    public static final int ANIMATION_TICKS = 4;
    /** 白（带 alpha，不然 GUI 的 fill 会因为 alpha=0 整块看不见）。 */
    private static final int WHITE_TEXT_COLOR = 0xFFFFFFFF;
    /** 一行几个格子。 */
    private static final int GRID_COLUMNS = 5;
    /** 格子（方块按钮）边长。 */
    private static final int CELL_SIZE = 28;
    /** 格子底部的分组色条高度。 */
    private static final int CELL_BAR_HEIGHT = 4;
    /** 格子之间的间距。 */
    private static final int CELL_GAP = 4;
    /** 格子图标是 16x16 的贴图。 */
    private static final int ICON_SIZE = 16;
    /**
     * 格子与功能按钮的配色：默认 / 悬停 / 选中 / 选中且悬停。
     * <p>
     * 跟以前的轮盘一致 —— 都是**半透明**的深色（叠在被压暗的世界上），
     * 图标本身是浅色，压暗了才看得清。
     */
    private static final ColorState CELL_COLOR_STATE = new ColorState(
            new Color(0f, 0f, 0f, 0.42f),
            new Color(0f, 0f, 0f, 0.42f),
            new Color(0.24f, 0.24f, 0.24f, 0.50f),
            new Color(0.36f, 0.36f, 0.36f, 0.64f),
            new Color(0.42f, 0.42f, 0.42f, 0.64f));
    private static final int SECTION_OFFSET_X = 112;
    private static final int SECTION_OFFSET_Y = 0;
    private static final int BUTTON_WIDTH = 22;
    private static final int BUTTON_HEIGHT = 22;
    private static final int BUTTON_OFFSET_X = 26;
    private static final int BUTTON_OFFSET_Y = 26;
    private static final float MOUSE_SCROLL_THRESHOLD = 2;
    private BiConsumer<Slot<S>, Boolean> slotSelectResponder;
    private BiConsumer<Button<B>, Boolean> optionSelectResponder;
    private List<? extends Slot<S>> slots = List.of();
    private List<? extends ButtonSet<B>> leftButtons = List.of();
    private List<? extends ButtonSet<B>> rightButtons = List.of();
    private Slot<S> hoveredSlot;
    private Button<B> hoveredButton;
    private Collection<? extends Slot<S>> selectedSlot = new HashSet<>();
    private float lastScrollOffset = 0;
    private float animationTicks = 0;
    private float animationScaleTicks = 0;
    private boolean detached = false;

    public AbstractGridScreen(Entrance entrance, Text text) {
        super(entrance, text);
    }

    protected static <T> Slot<T> slot(Object id, Text name, ResourceLocation icon, Color tintColor, T content) {
        return new Slot<>() {

            @Override
            public Object getId() {
                return id;
            }

            @Override
            public Text getDisplayName() {
                return name;
            }

            @Override
            public Text getDisplayCategory() {
                return null;
            }

            @Override
            public ResourceLocation getIcon() {
                return icon;
            }

            @Override
            public Color getTintColor() {
                return tintColor;
            }

            @Override
            public T getContent() {
                return content;
            }

        };
    }

    protected static <T> Button<T> button(Object id, Text name, Text category, Text summary, List<Text> description, ResourceLocation icon, T content, boolean activated) {
        return new Button<>() {

            @Override
            public Object getId() {
                return id;
            }

            @Override
            public Text getName() {
                return name;
            }

            @Override
            public Text getCategory() {
                return category;
            }

            @Override
            public Text getSummary() {
                return summary;
            }

            @Override
            public List<Text> getDescriptions() {
                return description;
            }

            @Override
            public ResourceLocation getIcon() {
                return icon;
            }

            @Override
            public Color getTintColor() {
                return null;
            }

            @Override
            public T getContent() {
                return content;
            }

            @Override
            public boolean isActivated() {
                return activated;
            }
        };
    }

    @SafeVarargs
    protected static <T> ButtonSet<T> buttonSet(Button<T>... entries) {
        return buttonSet(List.of(entries));
    }

    protected static <T> ButtonSet<T> buttonSet(List<? extends Button<T>> entries) {
        return new ButtonSet<>() {
            @Override
            public Text getDisplayName() {
                return null;
            }

            @Override
            public List<? extends Button<T>> getButtons() {
                return entries;
            }
        };
    }

    public static <T extends Option> Button<T> button(T option) {
        return button(option, false);
    }

    public static <T extends Option> Button<T> lazyButton(Supplier<Button<T>> supplier) {
        return new Button<>() {

            @Override
            public Object getId() {
                return supplier.get().getId();
            }

            @Override
            public Text getName() {
                return supplier.get().getName();
            }

            @Override
            public Text getCategory() {
                return supplier.get().getCategory();
            }

            @Override
            public Text getSummary() {
                return supplier.get().getSummary();
            }

            @Override
            public List<Text> getDescriptions() {
                return supplier.get().getDescriptions();
            }

            @Override
            public ResourceLocation getIcon() {
                return supplier.get().getIcon();
            }

            @Override
            public Color getTintColor() {
                return supplier.get().getTintColor();
            }

            @Override
            public T getContent() {
                return supplier.get().getContent();
            }

            @Override
            public boolean isActivated() {
                return supplier.get().isActivated();
            }
        };
    }

    public static <T extends Option> Button<T> button(T option, boolean activated) {
        return button(option, option.getNameText(), option.getCategoryText(), option.getTooltipText(), List.of(), option.getIcon(), option, activated);
    }

    public static <T extends Option> Button<T> button(T option, boolean activated, Text name, List<Text> description) {
        return button(option, name, option.getCategoryText(), option.getTooltipText(), description, option.getIcon(), option, activated);
    }

    protected abstract KeyBinding getAssignedKeyBinds();

    @Override
    public void init(int width, int height) {
        super.init(width, height);
        if (detached) {
            super.detach();
        }
    }

    @Override
    public boolean isPauseGame() {
        return false;
    }

    @Override
    public void onAnimateTick(float partialTick) {
        // 【交互变更】界面改成「按一下打开、再按一下关闭」的切换式。
        // 上游 1.21.3 是「按住显示、松开关闭」，实际用起来别扭：想点一个建造模式得一直按着，
        // 自动化测试也没法稳定复现（松开就没了）。
        // 现在的语义：
        //   - 第一次按键由客户端管理器消费并打开界面（见 McooBuildwithEaseClientManager）；
        //   - 界面开着的时候再按一次，这里 consumeClick() 命中 → 关闭（走淡出动画）；
        //   - 选中一个建造模式后由业务层调用 close() 关闭。
        // detached 是「粘」的：一旦置位就不会被后续帧改回去。
        if (getAssignedKeyBinds().consumeClick()) {
            this.detached = true;
        }
        this.animationTicks = Math.min(Math.max(animationTicks + (detached ? -1 : 1) * partialTick, 0), MAX_ANIMATION_TICKS);
        this.animationScaleTicks = Math.min(Math.max(animationScaleTicks + (detached ? -1 : 1) * partialTick, 0), MAX_ANIMATION_TICKS);
        if (detached && animationTicks == 0) {
            detach();
        }

    }

    /**
     * 请求关闭界面（走淡出动画）。
     * <p>
     * 用于「选中一个建造模式后自动关闭」这类场景 —— 上游靠松开按键来关闭，
     * 改成切换式之后没有「松开」这个时机了。
     */
    public void close() {
        this.detached = true;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void renderWidgetBackground(Renderer renderer, int mouseX, int mouseY, float deltaTick) {
        if (isTransparentBackground() && getEntrance().getClient().isLoaded()) {
            renderer.renderGradientRect(0, 0, super.getWidth(), super.getHeight(), ColorUtils.setAlpha(0x101010, (int) (0xC0 * getAnimationFactor())), ColorUtils.setAlpha(0x101010, (int) (0xD0 * getAnimationFactor())));
        } else {
            renderer.setRsShaderColor(0.25F, 0.25F, 0.25F, getAnimationFactor());
            renderer.renderPanelBackgroundTexture(0, 0, 0F, 0F, getWidth(), getHeight());
            renderer.setRsShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    @Override
    public void renderWidget(Renderer renderer, int mouseX, int mouseY, float deltaTick) {

        renderer.pushPose();
        renderer.translate(0, 0 * getAnimationScaleFactor() * ANIMATION_OFFSET_Y, 0);
        renderer.translate(getX() + getWidth() / 2f, getY() + getHeight() / 2f, 0);
        renderer.scale(MathUtils.lerp(getAnimationScaleFactor(), 0.92, 1));
        renderer.translate(-getX() - getWidth() / 2f, -getY() - getHeight() / 2f, 0);
        renderer.setRsShaderColor(1.0F, 1.0F, 1.0F, getAnimationFactor());
        super.renderWidget(renderer, mouseX, mouseY, deltaTick);
        hoveredSlot = null;
        hoveredButton = null;
        renderButtonSets(renderer, mouseX, mouseY, leftButtons, AxisDirection.NEGATIVE);
        renderButtonSets(renderer, mouseX, mouseY, rightButtons, AxisDirection.POSITIVE);
        renderSlots(renderer, mouseX, mouseY, slots);
        renderer.popPose();
    }

    @Override
    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        var result = false;
        if (isActive() && isVisible()) {
            if (slotSelectResponder != null && hoveredSlot != null) {
                slotSelectResponder.accept(hoveredSlot, button == 0);
                playMenuSound();
                result = true;
            }

            if (optionSelectResponder != null && hoveredButton != null) {
                optionSelectResponder.accept(hoveredButton, button == 0);
                playMenuSound();
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean onMouseScrolled(double mouseX, double mouseY, double amountX, double amountY) {
        var sign = lastScrollOffset * amountY;
        if (sign < 0) {
            lastScrollOffset = 0;
        }
        lastScrollOffset += amountY;
        if (lastScrollOffset > MOUSE_SCROLL_THRESHOLD) {
            playMenuSound();
            getEntrance().getStructureBuilder().setStructure(getPlayer(), getEntrance().getConfigStorage().getStructure(getEntrance().getStructureBuilder().getContext(getPlayer()).buildMode().previous()));
            lastScrollOffset = 0;
        } else if (lastScrollOffset < -MOUSE_SCROLL_THRESHOLD) {
            playMenuSound();
            getEntrance().getStructureBuilder().setStructure(getPlayer(), getEntrance().getConfigStorage().getStructure(getEntrance().getStructureBuilder().getContext(getPlayer()).buildMode().next()));
            lastScrollOffset = 0;
        }
        return true;
    }

    public final void setSlotSelectResponder(BiConsumer<Slot<S>, Boolean> consumer) {
        this.slotSelectResponder = consumer;
    }

    public final void setOptionSelectResponder(BiConsumer<Button<B>, Boolean> consumer) {
        this.optionSelectResponder = consumer;
    }

    public final void setSlots(List<Slot<S>> slots) {
        this.slots = slots;
    }

    @SafeVarargs
    public final void setLeftButtons(ButtonSet<B>... options) {
        setLeftButtons(List.of(options));
    }

    public final void setLeftButtons(List<? extends ButtonSet<B>> options) {
        this.leftButtons = options;
    }

    @SafeVarargs
    public final void setRightButtons(ButtonSet<B>... options) {
        setRightButtons(List.of(options));
    }

    public final void setRightButtons(List<? extends ButtonSet<B>> options) {
        this.rightButtons = options;
    }

    @SafeVarargs
    public final void setSelectedSlots(Slot<S>... slots) {
        setSelectedSlots(Set.of(slots));
    }

    public final void setSelectedSlots(Collection<? extends Slot<S>> slots) {
        this.selectedSlot = slots;
    }

    public void setScaleAnimation(float partialTicks) {
        this.animationScaleTicks = partialTicks;
    }

    /**
     * 画中间那一片方块按钮矩阵。
     * <p>
     * 每个格子 = 灰色方块（图标区）+ 底部的分组色条；点谁就选谁。
     */
    private void renderSlots(Renderer renderer, int mouseX, int mouseY, List<? extends Slot<S>> slots) {
        if (slots.isEmpty()) {
            return;
        }

        var columns = Math.min(GRID_COLUMNS, slots.size());
        var rows = (slots.size() + columns - 1) / columns;
        var gridWidth = columns * CELL_SIZE + (columns - 1) * CELL_GAP;
        var gridHeight = rows * (CELL_SIZE + CELL_BAR_HEIGHT) + (rows - 1) * CELL_GAP;
        var startX = (getWidth() - gridWidth) / 2;
        var startY = (getHeight() - gridHeight) / 2;

        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            var x = startX + (i % columns) * (CELL_SIZE + CELL_GAP);
            var y = startY + (i / columns) * (CELL_SIZE + CELL_BAR_HEIGHT + CELL_GAP);
            var barY = y + CELL_SIZE;

            var isActivated = selectedSlot.stream().anyMatch(obj -> Objects.equals(obj.getId(), slot.getId()));
            var isHovered = mouseX >= x && mouseX < x + CELL_SIZE && mouseY >= y && mouseY < barY + CELL_BAR_HEIGHT;

            var color = CELL_COLOR_STATE.defaultColor();
            if (isActivated) {
                color = CELL_COLOR_STATE.activedColor();
            }
            if (isHovered) {
                color = CELL_COLOR_STATE.hoveredColor();
            }
            if (isActivated && isHovered) {
                color = CELL_COLOR_STATE.activedHoveredColor();
            }

            if (isHovered) {
                hoveredSlot = slot;
            }

            // 方块按钮
            renderer.renderRect(RenderLayers.GUI, x, y, x + CELL_SIZE, y + CELL_SIZE, color.getRGB(), 0);

            // 当前生效的模式：套一圈白边
            if (isActivated) {
                renderer.renderRect(RenderLayers.GUI, x, y, x + CELL_SIZE, y + 1, WHITE_TEXT_COLOR, 0);
                renderer.renderRect(RenderLayers.GUI, x, y + CELL_SIZE - 1, x + CELL_SIZE, y + CELL_SIZE, WHITE_TEXT_COLOR, 0);
                renderer.renderRect(RenderLayers.GUI, x, y, x + 1, y + CELL_SIZE, WHITE_TEXT_COLOR, 0);
                renderer.renderRect(RenderLayers.GUI, x + CELL_SIZE - 1, y, x + CELL_SIZE, y + CELL_SIZE, WHITE_TEXT_COLOR, 0);
            }

            // 底部的分组色条：颜色自带透明度，跟以前轮盘上的分组弧一样
            var tint = slot.getTintColor();
            if (tint != null) {
                renderer.renderRect(RenderLayers.GUI, x, barY, x + CELL_SIZE, barY + CELL_BAR_HEIGHT, tint.getRGB(), 0);
            }

            // 图标
            if (slot.getIcon() != null) {
                renderer.pushPose();
                renderer.translate(x + (CELL_SIZE - ICON_SIZE) / 2, y + (CELL_SIZE - ICON_SIZE) / 2, 300);
                renderer.renderTexture(slot.getIcon(), 0, 0, ICON_SIZE, ICON_SIZE, 0f, 0f, 18, 18, 18, 18);
                renderer.popPose();
            }
        }
    }

    /**
     * 画左右两侧的功能按钮组（撤销/重做/替换/设置/图案/剪贴板，以及当前模式的可选特性）。
     */
    private void renderButtonSets(Renderer renderer, int mouseX, int mouseY, List<? extends ButtonSet<B>> buttonSets, AxisDirection direction) {

        double middleX = getWidth() / 2.0;
        double middleY = getHeight() / 2.0;

        var mouseCenterX = mouseX - middleX;
        var mouseCenterY = mouseY - middleY;

        for (var row = 0; row < buttonSets.size(); row++) {
            var buttonSet = buttonSets.get(row);
            var buttons = buttonSet.getButtons();

            for (int col = 0; col < buttons.size(); col++) {
                var button = buttons.get(col);

                var x = (SECTION_OFFSET_X + BUTTON_OFFSET_X * col) * direction.getStep();
                var y = (SECTION_OFFSET_Y + BUTTON_OFFSET_Y * (row - (buttonSets.size() - 1) / 2f)) * 1;

                var x1 = x - BUTTON_WIDTH / 2d;
                var y1 = y - BUTTON_HEIGHT / 2d;

                var x2 = x + BUTTON_WIDTH / 2d;
                var y2 = y + BUTTON_HEIGHT / 2d;

                var isActivated = button.isActivated();
                var isHovered = x1 <= mouseCenterX && x2 >= mouseCenterX && y1 <= mouseCenterY && y2 >= mouseCenterY;

                var color = CELL_COLOR_STATE.defaultColor();
                if (isActivated) {
                    color = CELL_COLOR_STATE.activedColor();
                }
                if (isHovered) {
                    color = CELL_COLOR_STATE.hoveredColor();
                }
                if (isActivated && isHovered) {
                    color = CELL_COLOR_STATE.activedHoveredColor();
                }

                if (isHovered) {
                    hoveredButton = button;
                }

                renderer.renderRect(RenderLayers.GUI, (int) (middleX + x1), (int) (middleY + y1), (int) (middleX + x2), (int) (middleY + y2), color.getRGB(), 0);
                // icon
                if (button.getIcon() != null) {
                    renderer.pushPose();
                    var iconX = x;
                    var iconY = y;
                    renderer.translate((int) MathUtils.round(middleX + iconX - 8), (int) MathUtils.round(middleY + iconY - 8), 0);
                    renderer.renderTexture(button.getIcon(), 0, 0, 16, 16, 0f, 0f, 18, 18, 18, 18);
                    renderer.popPose();
                }

            }
        }
    }

    @Override
    public void renderWidgetOverlay(Renderer renderer, int mouseX, int mouseY, float deltaTick) {
        super.renderWidgetOverlay(renderer, mouseX, mouseY, deltaTick);

        if (hoveredSlot != null) {
            var tooltip = new ArrayList<Text>();
            if (hoveredSlot.getDisplayCategory() != null) {
                tooltip.add(hoveredSlot.getDisplayCategory().withStyle(ChatFormatting.WHITE));
            }
            tooltip.add(hoveredSlot.getDisplayName().withStyle(ChatFormatting.GOLD));
            renderer.renderTooltip(getTypeface(), tooltip, mouseX, mouseY);
        }

        if (hoveredButton != null) {
            var tooltip = new ArrayList<Text>();
            tooltip.add(hoveredButton.getCategory().withStyle(ChatFormatting.WHITE));
            tooltip.add(hoveredButton.getName().withStyle(ChatFormatting.GOLD));

            if (!hoveredButton.getDescriptions().isEmpty()) {
                tooltip.addAll(hoveredButton.getDescriptions());
            }
            if (!hoveredButton.getSummary().getString().isEmpty()) {
//                tooltip.add(Text.text("Click [Left Button] for Switch").withStyle(ChatFormatting.DARK_GRAY));
//                tooltip.add(Text.text("Click [Right Button] for More Configs").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Text.empty());
                tooltip.add(TooltipHelper.holdShiftForSummary());
                if (TooltipHelper.isSummaryButtonDown()) {
                    tooltip.add(Text.empty());
//                    tooltip.add(hoveredButton.getTooltip());
                    tooltip.addAll(TooltipHelper.wrapLines(getTypeface(), hoveredButton.getSummary().withStyle(ChatFormatting.GRAY)));
                }
            }
            renderer.renderTooltip(getTypeface(), tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected McooBuildwithEaseClient getEntrance() {
        return (McooBuildwithEaseClient) super.getEntrance();
    }

    private Player getPlayer() {
        return getEntrance().getClient().getPlayer();
    }

    public static final int MAX_ANIMATION_TICKS = 4;

    private float getAnimationFactor() {
        var fac = 1f - Math.min(animationTicks, MAX_ANIMATION_TICKS) / MAX_ANIMATION_TICKS;
        if (detached) {
            return 1 - MathUtils.lerp((1 - fac) * (1 - fac), 1f, 0f);
        }
        return 1 - MathUtils.lerp(fac * fac, 0f, 1f);
    }

    private float getAnimationScaleFactor() {
        var fac = 1f - Math.min(animationScaleTicks, MAX_ANIMATION_TICKS) / MAX_ANIMATION_TICKS;
        if (detached) {
            return 1 - MathUtils.lerp((1 - fac) * (1 - fac), 1f, 0f);
        }
        return 1 - MathUtils.lerp(fac * fac, 0f, 1f);
    }

    private void playMenuSound() {
        getEntrance().getClient().getSoundManager().playButtonClickSound();
    }

    public interface Slot<T> {

        Object getId();

        Text getDisplayName();

        Text getDisplayCategory();

        ResourceLocation getIcon();

        Color getTintColor();

        T getContent();

    }

    public interface Button<T> {

        Object getId();

        Text getName();

        Text getCategory();

        Text getSummary();

        List<Text> getDescriptions();

        ResourceLocation getIcon();

        Color getTintColor();

        T getContent();

        boolean isActivated();

    }

    public interface ButtonSet<T> {

        Text getDisplayName();

        List<? extends Button<T>> getButtons();

    }

    record ColorState(Color disabledColor, Color defaultColor, Color hoveredColor, Color activedColor, Color activedHoveredColor) {
    }

}
