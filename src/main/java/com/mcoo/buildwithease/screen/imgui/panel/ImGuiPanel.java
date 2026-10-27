package com.mcoo.buildwithease.screen.imgui.panel;

/**
 * ImGui Shell 里的一个<b>功能面板</b>（建造 / 图案 / 剪贴板 / 渲染 …）。
 *
 * <h3>为什么是"面板"而不是"界面"</h3>
 * 目标形态是 Axiom / Photoshop 那种编辑器排布：左侧导航、中间内容、右侧详情。
 * 如果每个功能仍然各写一个"整屏界面"，那就回到"二十个界面各写各的"老路上了。
 * 所以功能被切成面板，由 {@code McooBuildwithEaseShellScreen} 统一装配进三栏。
 *
 * <h3>实现约定</h3>
 * <ul>
 *     <li>面板只画<b>自己那一栏的内容</b>，不创建窗口、不管布局、不写样式
 *         （样式统一在 {@code McooImGuiTheme}）；</li>
 *     <li>标签一律走 i18n（{@code Text.translate}），不要写死字符串；</li>
 *     <li>面板自己持有选中状态/编辑中的值，Shell 不替它保存；</li>
 *     <li>任何面板抛异常不能拖垮 Shell —— 由 Shell 逐个 try/catch（见 {@code ImGuiPanelHost}）。</li>
 * </ul>
 */
public interface ImGuiPanel {

    /** 唯一 id：用于导航选中判断，也用作 ImGui 的 ID 作用域前缀。 */
    String id();

    /** 左栏导航里显示的文案键。 */
    String titleKey();

    /** 中间栏内容（列表 / 预览 / 主操作区）。 */
    void renderContent();

    /** 右栏内容（当前选中对象的详情）。 */
    void renderDetails();

}
