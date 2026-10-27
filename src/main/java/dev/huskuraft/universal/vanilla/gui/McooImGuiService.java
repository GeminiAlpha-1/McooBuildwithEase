package dev.huskuraft.universal.vanilla.gui;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.enaium.fabric.imgui.DefaultImGui;
import imgui.ImFont;
import imgui.ImFontConfig;
import imgui.ImFontGlyphRangesBuilder;
import imgui.ImGuiIO;

/**
 * 本模组自己的 ImGui 服务：<b>字体与主题的唯一落脚点</b>。
 *
 * <h3>为什么要有这个类</h3>
 * ImGui 宿主（fabric-gui-imgui）通过 {@code ServiceLoader.load(ImGuiService.class)} 找实现，
 * 找不到就反射兜底实例化它自己的 {@code DefaultImGui}（26.2-1.2.0 字节码实证）。
 * 那个兜底实现的 {@code configure()} <b>只调 {@code addFontDefault()} + {@code build()}</b> ——
 * 默认字体 ProggyClean 是<b>纯 ASCII</b>，所以中文界面全是方块/问号（实测截图确认）。
 *
 * 宿主<b>没有</b>提供任何字体 / 主题扩展点，但 {@code ImGuiService.configure(ImGuiIO)}
 * 是 <b>public 非 final</b>，且宿主 jar 里**没有** {@code META-INF/services} 条目
 * ⇒ 我们自己注册一个 provider，在 {@code configure()} 里装字体，<b>零 mixin</b>。
 * （做法与 Axiom 一致，但走的是公开 API，不需要动渲染后端。）
 *
 * <h3>字体为什么是"子集 + 粗范围"</h3>
 * 宿主从不设置 {@code ImGuiBackendFlags.RendererHasTextures}（全 jar 搜不到该字符串），
 * 所以走的是 ImGui 的 <b>legacy 静态图集</b>：{@code imgui_draw.cpp} 里
 * {@code if (atlas->RendererHasTextures == false) ImFontAtlasBuildLegacyPreloadAllGlyphRanges(atlas)}
 * ⇒ <b>{@code GlyphRanges} 是硬要求</b>，不给就只有 0x20..0xFF；且图集 build 完即 locked，
 * 之后<b>不能</b>动态改字号（{@code PushFont(font, 别的 px)} 会 assert）。
 *
 * 因此策略是：
 * <ul>
 *     <li>字体文件是<b>子集</b>（{@code McooUI-SC-Medium.ttf}，1.27MB / 4878 字形：
 *         本模组 7 个 lang 文件用到的全部字符 + GB2312 一级 3755 常用汉字 + 拉丁 / 西里尔 / 标点）；</li>
 *     <li>运行时给<b>粗粒度范围</b>（整个 CJK 基本区 0x4E00..0x9FFF 等），
 *         ImGui 只会把字体里真有的字形烘进图集，缺的自动跳过
 *         ⇒ 不需要在运行时解析 lang 文件，也不会出现"字体里有、范围没给"的漏字。</li>
 * </ul>
 *
 * <h3>字号口径</h3>
 * 跟 Axiom 一致：{@code base = (int)(16 * uiScale)}，中文与拉丁同字号（Axiom 给中文放大到 ×5/4，
 * 那是因为它要先加拉丁字体再 merge；我们单一字体，统一尺寸反而更整齐）。
 * <b>绝不用 {@code io.setFontGlobalScale()}</b>：那是"小图集放大"，中文必糊；
 * 要改字号只能重建图集（见 {@link #uiScale()} 的说明）。
 */
public final class McooImGuiService extends DefaultImGui {

    private static final Logger LOGGER = LoggerFactory.getLogger(McooImGuiService.class);

    /** 字体资源路径（跟着本模组的 resources 走，不依赖资源包重载时机）。 */
    private static final String FONT_PATH = "/assets/mcoo_build_with_ease/fonts/McooUI-SC-Medium.ttf";

    /** 基准字号（1 单位 = MC 的屏幕坐标 1 像素；16 与 MC 默认 GUI 缩放下正文观感相当）。 */
    private static final float BASE_FONT_SIZE = 16.0f;

    /**
     * ImGui 的 {@code addFontFromMemoryTTF(byte[], ...)} 在 Java 侧只是把数组丢给 native
     * （字节码：{@code nAddFontFromMemoryTTF([BFJ[S)J}），<b>是否拷贝取决于 native 实现</b>。
     * 为免踩"字体数据被 GC 后 native 读到野指针"这种崩法，这里持一个进程级强引用。
     * 1.27MB，代价可以忽略。
     */
    private static byte[] fontDataRef;

    /**
     * {@link ImFontConfig} 与字形范围数组也必须是**进程级**强引用，且在
     * {@code super.configure()} 里的 {@code atlas.build()} 跑完之前不能 {@code destroy()}。
     * 原因：ImGui 的 {@code AddFont} 只把 {@code ImFontConfig} 复制进 {@code atlas->ConfigData}，
     * 而 {@code GlyphRanges} 是<b>指针</b>——复制的是指针本身，指向的数组由 imgui-java 放在 native 侧。
     * 提前 destroy / 让 Java 侧数组被回收，build 阶段就会读到野指针，
     * 表现是"字体已装载"但图集里只有默认范围（中文全是方块）。**这个坑实测踩过一次。**
     */
    private static ImFontConfig fontConfigRef;
    private static short[] glyphRangesRef;
    private static ImFont defaultFontRef;

    /** UI 缩放档位：1.0 时正文 16px。目前固定 1.0，接配置项后改这里即可（改后需重建图集）。 */
    private static float uiScale() {
        return 1.0f;
    }

    public McooImGuiService() {
        // ini 文件名：ImGui 会把它存在 MC 的工作目录（run/ 或 .minecraft/）
        super("mcoo_build_with_ease");
    }

    /**
     * 在 {@code super.configure(io)} <b>之前</b>把我们的字体排进队列。
     * <p>
     * 顺序很关键：{@code super.configure()} 里会 {@code addFontDefault()} 然后立刻
     * {@code build()}；build 之后再 addFont 不会进图集（legacy 模式下图集已 locked）。
     * 我们先 add → 我们的字体成为 {@code Fonts[0]}（= ImGui 默认字体），
     * 宿主随后加的 ProggyClean 退化成 {@code Fonts[1]} 兜底。
     */
    @Override
    public void configure(ImGuiIO io) {
        try {
            installFont(io);
        } catch (Throwable error) {
            // 字体装不上不能让游戏崩：退回宿主的默认行为（英文照常，中文显示为方块）
            LOGGER.error("[imgui] 装载界面字体失败，退回默认字体（中文将无法显示）", error);
        }

        super.configure(io);

        try {
            if (defaultFontRef != null) {
                // 显式指定默认字体：不依赖「Fonts[0] 就是默认字体」这条隐式约定
                io.setFontDefault(defaultFontRef);
            }
        } catch (Throwable error) {
            LOGGER.error("[imgui] 指定默认字体失败", error);
        }

        try {
            // 主题放在 super 之后：宿主的 configure 只管字体与 ini/configFlags，不碰样式，
            // 但顺序上让它先跑完更保险（将来宿主版本若加样式默认值，我们的仍然生效）。
            McooImGuiTheme.apply(uiScale());
        } catch (Throwable error) {
            LOGGER.error("[imgui] 套用界面主题失败，将使用 ImGui 默认样式", error);
        }
    }

    private void installFont(ImGuiIO io) {
        byte[] data = fontDataRef;
        if (data == null) {
            data = readFont();
            fontDataRef = data;
        }

        var atlas = io.getFonts();
        int px = Math.max(8, Math.round(BASE_FONT_SIZE * uiScale()));

        var ranges = new ImFontGlyphRangesBuilder();
        ranges.addRanges(atlas.getGlyphRangesDefault());                 // 0x20..0xFF（拉丁）
        ranges.addRanges(new short[]{0x0400, 0x04FF, 0});                // 西里尔（ru_ru）
        ranges.addRanges(new short[]{0x2000, 0x206F, 0});                // 通用标点（— … “ ”）
        ranges.addRanges(new short[]{0x3000, 0x303F, 0});                // CJK 标点（、。“」）
        // ⚠️ 0x7FFF 以上的码点**不能**走 addRanges(short[])：
        //    imgui-java 里 addRanges 是纯 Java 实现（字段 usedChars），用 Java 的**有符号** short
        //    做 `for (c = start; c <= end; c++)` 遍历。0x9FFF 作为 short 是负数，
        //    于是 `19968 <= -24577` 不成立 → 整个汉字区间被<b>静默丢弃</b>：
        //    不报错、字体也 loaded=true，但图集只有 512×512（只烘了拉丁），中文全显示成方块。
        //    **这个坑实测排查了三轮，别再改回 addRanges。**
        //    addChar(char) 收的是无符号 16 位，可以安全表达这些码点。
        addRangeUnsigned(ranges, 0xFF00, 0xFFEF);                        // 全角
        addRangeUnsigned(ranges, 0x4E00, 0x9FFF);                        // CJK 统一汉字（字体里的子集会被烘进去）
        // 界面里会直接用到的符号：修饰键 / 状态图标 / 方向箭头
        ranges.addText("⌘⌃⎇⇧❖⚠✔✖⬆⬇➡⬅☠☢❤✏←↑→↓");

        // 范围数组要一直活到 build()：见字段注释（native 侧存的是指针，不是副本）
        glyphRangesRef = ranges.buildRanges();

        fontConfigRef = new ImFontConfig();
        fontConfigRef.setOversampleH(2);
        fontConfigRef.setOversampleV(2);
        fontConfigRef.setName("McooUI SC, " + px + "px");
        // 两处都给：不同 imgui-java 版本对「范围从哪个入参生效」的处理不一致，
        // 而 config 上的 GlyphRanges 是 native 侧真正读的那一份。
        fontConfigRef.setGlyphRanges(glyphRangesRef);

        defaultFontRef = atlas.addFontFromMemoryTTF(data, px, fontConfigRef, glyphRangesRef);
        if (defaultFontRef == null) {
            LOGGER.error("[imgui] addFontFromMemoryTTF 返回 null，界面字体未生效");
            return;
        }
        var head = new StringBuilder();
        for (int i = 0; i < Math.min(12, glyphRangesRef.length); i++) {
            head.append(i == 0 ? "" : ",").append(glyphRangesRef[i] & 0xFFFF);
        }
        LOGGER.debug("[imgui] 字形范围 {} 个 short，前段 [{}]", glyphRangesRef.length, head);

        // 立刻烘一次图集：GlyphRanges 在 native 侧是**指针**，越晚 build 越可能指到失效内存。
        // 在这里 build，范围数组与 config 都还是热的（见字段注释里的踩坑记录）。
        // 宿主随后还会在 super.configure() 里 addFontDefault + build，此时图集已构建完成，那一步是空转。
        boolean built = atlas.build();
        var width = new imgui.type.ImInt();
        var height = new imgui.type.ImInt();
        atlas.getTexDataAsRGBA32(width, height);
        // INFO 只留一行：图集尺寸是最有用的自检信号 ——
        // 只有 512×512 就说明汉字范围没进去（范围被静默丢弃的经典症状），2048×2048 才是正常的。
        LOGGER.info("[imgui] 界面字体：McooUI SC {}px，字形图集 {}x{}（build={}）",
                px, width.get(), height.get(), built);
    }

    /**
     * 把 {@code [from, to]} 逐码点加进 builder。
     * <p>
     * 存在的唯一理由：{@link ImFontGlyphRangesBuilder#addRanges(short[])} 在 imgui-java 里是
     * 纯 Java 实现、用有符号 short 遍历，{@code to > 0x7FFF} 的区间会被静默丢弃（详见调用处注释）。
     * {@code char} 是无符号 16 位，所以这里走 {@link ImFontGlyphRangesBuilder#addChar(char)}。
     */
    private static void addRangeUnsigned(ImFontGlyphRangesBuilder builder, int from, int to) {
        for (int codepoint = from; codepoint <= to; codepoint++) {
            builder.addChar((char) codepoint);
        }
    }

    private static byte[] readFont() {
        try (InputStream stream = McooImGuiService.class.getResourceAsStream(FONT_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("字体资源缺失: " + FONT_PATH);
            }
            return stream.readAllBytes();
        } catch (IOException error) {
            throw new IllegalStateException("读取字体资源失败: " + FONT_PATH, error);
        }
    }

}
