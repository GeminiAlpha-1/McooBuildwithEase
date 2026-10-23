package com.mcoo.buildwithease.screen.clipboard;

import com.mcoo.buildwithease.McooBuildwithEaseClient;
import dev.huskuraft.universal.api.gui.AbstractPanelScreen;
import dev.huskuraft.universal.api.gui.button.Button;
import dev.huskuraft.universal.api.platform.Entrance;
import dev.huskuraft.universal.api.text.Text;

/**
 * 视角面板：把结构绕世界的某根轴转 90°，或者回到默认视角。
 * <p>
 * 三个轴按钮<b>左键顺时针、右键逆时针</b>；
 * 「回到默认视角」把偏航、俯仰、缩放、平移和朝向修正全部复位。
 */
public class McooBuildwithEaseViewpointScreen extends AbstractPanelScreen {

    /** 鼠标键：左键 = 顺时针。 */
    private static final int CLOCKWISE = 0;
    /** 鼠标键：右键 = 逆时针。 */
    private static final int COUNTER_CLOCKWISE = 1;

    private final StructureSnapshotWidget widget;

    public McooBuildwithEaseViewpointScreen(Entrance entrance, StructureSnapshotWidget widget) {
        super(entrance, Text.translate("mcoo_build_with_ease.clipboard.viewpoint.title"), PANEL_WIDTH_60,
                PANEL_TITLE_HEIGHT_1 + PANEL_BUTTON_ROW_HEIGHT_5);
        this.widget = widget;
    }

    @Override
    protected McooBuildwithEaseClient getEntrance() {
        return (McooBuildwithEaseClient) super.getEntrance();
    }

    @Override
    public void onCreate() {
        addWidget(new dev.huskuraft.universal.api.gui.text.TextWidget(getEntrance(),
                getLeft() + getWidth() / 2, getTop() + PANEL_TITLE_HEIGHT_1 - 10,
                getScreenTitle().withColor(AbstractPanelScreen.TITLE_COLOR),
                dev.huskuraft.universal.api.gui.text.TextWidget.Gravity.CENTER));

        addWidget(axisButton("mcoo_build_with_ease.clipboard.viewpoint.axis_x", 0, 4f));
        addWidget(axisButton("mcoo_build_with_ease.clipboard.viewpoint.axis_y", 1, 3f));
        addWidget(axisButton("mcoo_build_with_ease.clipboard.viewpoint.axis_z", 2, 2f));

        addWidget(Button.builder(getEntrance(),
                Text.translate("mcoo_build_with_ease.clipboard.viewpoint.reset"), button -> {
            this.widget.resetView();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 1f, 0f, 1f).build());

        addWidget(Button.builder(getEntrance(), Text.translate("mcoo_build_with_ease.button.cancel"), button -> {
            detach();
        }).setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), 0f, 0f, 1f).build());
    }

    /** 一个绕轴旋转 90° 的按钮（左键顺时针、右键逆时针）。 */
    private Button axisButton(String translationKey, int axis, float row) {
        return Button.builder(getEntrance(), Text.translate(translationKey), button -> {
            // 左键顺时针（右手定则的负方向），右键逆时针
            var steps = button.lastMouseButton() == COUNTER_CLOCKWISE ? 1 : -1;
            this.widget.rotateAxis(axis, steps);
        }).setRightClickable().setBoundsGrid(getLeft(), getTop(), getWidth(), getHeight(), row, 0f, 1f).build();
    }
}
