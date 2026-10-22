package dev.huskuraft.universal.vanilla.renderer;

import com.google.auto.service.AutoService;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.renderer.RenderLayer;
import dev.huskuraft.universal.api.renderer.RenderStateFactory;
import dev.huskuraft.universal.api.renderer.Shader;
import dev.huskuraft.universal.api.renderer.Shaders;
import dev.huskuraft.universal.api.renderer.VertexFormat;
import dev.huskuraft.universal.api.renderer.VertexFormats;
import dev.huskuraft.universal.api.renderer.programs.CompositeRenderState;
import dev.huskuraft.universal.api.renderer.programs.RenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * 26.2 没有 {@code RenderStateShard} / {@code CompositeState} 了，渲染状态被拆成
 * 「后端无关的 {@link com.mojang.blaze3d.pipeline.RenderPipeline}」+
 * 「{@link net.minecraft.client.renderer.rendertype.RenderSetup}」+
 * 「{@link RenderType}（二者的封装）」。
 * <p>
 * <b>关键限制</b>：26.2 的 {@code RenderType} 构造器是 private，静态工厂
 * {@code RenderType.create(String, RenderSetup)} 是包内可见 —— 外部（也就是本工程）
 * <b>无法自建 RenderType</b>，只能复用 {@link RenderTypes} 里的现成条目。
 * <p>
 * 因此这里的做法是：把 common-api 的 {@code RenderState} 各分量先解析成「语义」，
 * 再在 {@link #createCompositeRenderLayer} 里连同顶点格式 / 拓扑一起映射到最接近的
 * vanilla {@code RenderType}。映射不精确的地方（自定义纹理丢失、深度函数被忽略等）
 * 会在交付报告里逐条列出。
 */
@AutoService(RenderStateFactory.class)
public final class MinecraftRenderStateFactory implements RenderStateFactory {

    // ------------------------------------------------------------------
    // common-api 的 RenderState 各分量在 26.2 的「语义替身」
    // ------------------------------------------------------------------

    public record TextureStateSpec(Identifier location, boolean blur, boolean mipmap) implements RenderState.TextureState {
        @Override
        public Object refs() {
            return location;
        }
    }

    public record ShaderStateSpec(Shader shader) implements RenderState.ShaderState {
        @Override
        public Object refs() {
            return shader;
        }
    }

    public record TransparencyStateSpec(RenderState.TransparencyState.Type type) implements RenderState.TransparencyState {
        @Override
        public Object refs() {
            return type;
        }
    }

    public record DepthTestStateSpec(int function) implements RenderState.DepthTestState {
        @Override
        public Object refs() {
            return function;
        }
    }

    public record CullStateSpec(boolean cull) implements RenderState.CullState {
        @Override
        public Object refs() {
            return cull;
        }
    }

    public record LightmapStateSpec(boolean lightmap) implements RenderState.LightmapState {
        @Override
        public Object refs() {
            return lightmap;
        }
    }

    public record OverlayStateSpec(boolean overlay) implements RenderState.OverlayState {
        @Override
        public Object refs() {
            return overlay;
        }
    }

    public record LayeringStateSpec(RenderState.LayeringState.Type type) implements RenderState.LayeringState {
        @Override
        public Object refs() {
            return type;
        }
    }

    public record OutputStateSpec(RenderState.OutputState.Target target) implements RenderState.OutputState {
        @Override
        public Object refs() {
            return target;
        }
    }

    public record TexturingStateSpec(Runnable setup, Runnable clear) implements RenderState.TexturingState {
        @Override
        public Object refs() {
            return this;
        }
    }

    public record OffsetTexturingStateSpec(float offsetX, float offsetY) implements RenderState.OffsetTexturingState {
        @Override
        public Object refs() {
            return this;
        }
    }

    public record WriteMaskStateSpec(boolean writeColor, boolean writeDepth) implements RenderState.WriteMaskState {
        @Override
        public Object refs() {
            return this;
        }
    }

    public record LineStateSpec(Double width) implements RenderState.LineState {
        @Override
        public Object refs() {
            return width;
        }
    }

    public record ColorLogicStateSpec(RenderState.ColorLogicState.Op op) implements RenderState.ColorLogicState {
        @Override
        public Object refs() {
            return op;
        }
    }

    public record RenderStateSpec(String name) implements RenderState {
        @Override
        public Object refs() {
            return name;
        }
    }

    /** 合并后的渲染状态。 */
    public record CompositeStateSpec(
            Identifier texture,
            boolean blur,
            boolean mipmap,
            RenderState.TransparencyState.Type transparency,
            int depthFunction,
            boolean cull,
            boolean lightmap,
            boolean overlay,
            RenderState.LayeringState.Type layering,
            RenderState.OutputState.Target output,
            boolean writeColor,
            boolean writeDepth,
            Double lineWidth,
            RenderState.ColorLogicState.Op colorLogic,
            boolean affectOutline
    ) implements CompositeRenderState {
        @Override
        public Object refs() {
            return this;
        }

        public boolean translucent() {
            return switch (transparency) {
                case NO -> false;
                case ADDITIVE, LIGHTNING, GLINT, CRUMBLING, TRANSLUCENT -> true;
            };
        }
    }

    // ------------------------------------------------------------------
    // 工厂方法
    // ------------------------------------------------------------------

    @Override
    public RenderLayer createCompositeRenderLayer(String name,
                                                  VertexFormat vertexFormat,
                                                  VertexFormat.Mode vertexFormatMode,
                                                  int bufferSize,
                                                  boolean affectsCrumbling,
                                                  boolean sortOnUpload,
                                                  CompositeRenderState state) {
        var format = (com.mojang.blaze3d.vertex.VertexFormat) vertexFormat.reference();
        var topology = (PrimitiveTopology) vertexFormatMode.reference();
        var spec = state instanceof CompositeStateSpec composite ? composite : defaultState();
        var renderType = resolveRenderType(format, topology, spec);
        return new MinecraftRenderLayer(name, renderType, spec.texture());
    }

    @Override
    public CompositeRenderState createCompositeState(RenderState.TextureState textureState,
                                                     RenderState.ShaderState shaderState,
                                                     RenderState.TransparencyState transparencyState,
                                                     RenderState.DepthTestState depthTestState,
                                                     RenderState.CullState cullState,
                                                     RenderState.LightmapState lightmapState,
                                                     RenderState.OverlayState overlayState,
                                                     RenderState.LayeringState layeringState,
                                                     RenderState.OutputState outputState,
                                                     RenderState.TexturingState texturingState,
                                                     RenderState.WriteMaskState writeMaskState,
                                                     RenderState.LineState lineState,
                                                     RenderState.ColorLogicState colorLogicState,
                                                     boolean affectOutline) {
        var texture = textureState instanceof TextureStateSpec spec ? spec : null;
        return new CompositeStateSpec(
                texture != null ? texture.location() : null,
                texture != null && texture.blur(),
                texture != null && texture.mipmap(),
                transparencyState instanceof TransparencyStateSpec spec ? spec.type() : RenderState.TransparencyState.Type.NO,
                depthTestState instanceof DepthTestStateSpec spec ? spec.function() : 0,
                !(cullState instanceof CullStateSpec spec) || spec.cull(),
                lightmapState instanceof LightmapStateSpec spec && spec.lightmap(),
                overlayState instanceof OverlayStateSpec spec && spec.overlay(),
                layeringState instanceof LayeringStateSpec spec ? spec.type() : RenderState.LayeringState.Type.NO,
                outputState instanceof OutputStateSpec spec ? spec.target() : RenderState.OutputState.Target.NO,
                !(writeMaskState instanceof WriteMaskStateSpec spec) || spec.writeColor(),
                !(writeMaskState instanceof WriteMaskStateSpec spec) || spec.writeDepth(),
                lineState instanceof LineStateSpec spec ? spec.width() : null,
                colorLogicState instanceof ColorLogicStateSpec spec ? spec.op() : RenderState.ColorLogicState.Op.NO_LOGIC,
                affectOutline
        );
    }

    @Override
    public RenderState createRenderState(String name, Runnable setupState, Runnable clearState) {
        return new RenderStateSpec(name);
    }

    @Override
    public RenderState.TextureState createTextureState(String name, RenderState.TextureState.Texture texture) {
        if (texture == null) {
            return new TextureStateSpec(null, false, false);
        }
        return new TextureStateSpec(toIdentifier(texture.location()), texture.blur(), texture.mipmap());
    }

    @Override
    public RenderState.ShaderState createShaderState(String name, Shader shader) {
        return new ShaderStateSpec(shader);
    }

    @Override
    public RenderState.TransparencyState createTransparencyState(String name, RenderState.TransparencyState.Type type) {
        return new TransparencyStateSpec(type);
    }

    @Override
    public RenderState.DepthTestState createDepthTestState(String name, int function) {
        return new DepthTestStateSpec(function);
    }

    @Override
    public RenderState.CullState createCullState(String name, boolean cull) {
        return new CullStateSpec(cull);
    }

    @Override
    public RenderState.LightmapState createLightmapState(String name, boolean lightmap) {
        return new LightmapStateSpec(lightmap);
    }

    @Override
    public RenderState.OverlayState createOverlayState(String name, boolean overlay) {
        return new OverlayStateSpec(overlay);
    }

    @Override
    public RenderState.LayeringState createLayeringState(String name, RenderState.LayeringState.Type type) {
        return new LayeringStateSpec(type);
    }

    @Override
    public RenderState.OutputState createOutputState(String name, RenderState.OutputState.Target target) {
        return new OutputStateSpec(target);
    }

    @Override
    public RenderState.TexturingState createTexturingState(String name, Runnable setupState, Runnable clearState) {
        return new TexturingStateSpec(setupState, clearState);
    }

    @Override
    public RenderState.OffsetTexturingState createOffsetTexturingState(String name, float offsetX, float offsetY) {
        return new OffsetTexturingStateSpec(offsetX, offsetY);
    }

    @Override
    public RenderState.WriteMaskState createWriteMaskState(String name, boolean writeColor, boolean writeDepth) {
        return new WriteMaskStateSpec(writeColor, writeDepth);
    }

    @Override
    public RenderState.LineState createLineState(String name, Double width) {
        return new LineStateSpec(width);
    }

    @Override
    public RenderState.ColorLogicState createColorLogicState(String name, RenderState.ColorLogicState.Op op) {
        return new ColorLogicStateSpec(op);
    }

    @Override
    public Shader getShader(Shaders shaders) {
        var pipeline = switch (shaders) {
            case POSITION_COLOR_LIGHTMAP -> RenderPipelines.LIGHTMAP;
            case POSITION -> RenderPipelines.DEBUG_FILLED_BOX;
            case POSITION_COLOR_TEX -> RenderPipelines.GUI_TEXTURED;
            case POSITION_TEX -> RenderPipelines.GUI_TEXTURED;
            case POSITION_COLOR_TEX_LIGHTMAP -> RenderPipelines.LIGHTMAP;
            case POSITION_COLOR -> RenderPipelines.DEBUG_FILLED_BOX;
            case SOLID -> RenderPipelines.SOLID_BLOCK;
            case CUTOUT_MIPPED -> RenderPipelines.CUTOUT_BLOCK;
            case CUTOUT -> RenderPipelines.CUTOUT_BLOCK;
            case TRANSLUCENT -> RenderPipelines.TRANSLUCENT_BLOCK;
            case TRANSLUCENT_MOVING_BLOCK -> RenderPipelines.TRANSLUCENT_BLOCK;
            case TRANSLUCENT_NO_CRUMBLING -> RenderPipelines.TRANSLUCENT_BLOCK;
            case ARMOR_CUTOUT_NO_CULL -> RenderPipelines.ARMOR_CUTOUT_NO_CULL;
            case ENTITY_SOLID -> RenderPipelines.ENTITY_SOLID;
            case ENTITY_CUTOUT -> RenderPipelines.ENTITY_CUTOUT;
            case ENTITY_CUTOUT_NO_CULL -> RenderPipelines.ENTITY_CUTOUT;
            case ENTITY_CUTOUT_NO_CULL_Z_OFFSET -> RenderPipelines.ENTITY_CUTOUT_Z_OFFSET;
            case ITEM_ENTITY_TRANSLUCENT_CULL -> RenderPipelines.ITEM_TRANSLUCENT;
            case ENTITY_TRANSLUCENT_CULL -> RenderPipelines.ENTITY_TRANSLUCENT_CULL;
            case ENTITY_TRANSLUCENT -> RenderPipelines.ENTITY_TRANSLUCENT;
            case ENTITY_TRANSLUCENT_EMISSIVE -> RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE;
            case ENTITY_SMOOTH_CUTOUT -> RenderPipelines.ENTITY_CUTOUT;
            case BEACON_BEAM -> RenderPipelines.BEACON_BEAM_TRANSLUCENT;
            case ENTITY_DECAL -> RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL;
            case ENTITY_NO_OUTLINE -> RenderPipelines.ENTITY_SOLID;
            case ENTITY_SHADOW -> RenderPipelines.ENTITY_SHADOW;
            case ENTITY_ALPHA -> RenderPipelines.ENTITY_TRANSLUCENT;
            case EYES -> RenderPipelines.EYES;
            case ENERGY_SWIRL -> RenderPipelines.ENERGY_SWIRL;
            case LEASH -> RenderPipelines.LEASH;
            case WATER_MASK -> RenderPipelines.WATER_MASK;
            case OUTLINE -> RenderPipelines.OUTLINE_NO_CULL;
            case ARMOR_GLINT -> RenderPipelines.GLINT;
            case ARMOR_ENTITY_GLINT -> RenderPipelines.GLINT;
            case GLINT_TRANSLUCENT -> RenderPipelines.GLINT;
            case GLINT -> RenderPipelines.GLINT;
            case GLINT_DIRECT -> RenderPipelines.GLINT;
            case ENTITY_GLINT -> RenderPipelines.GLINT;
            case ENTITY_GLINT_DIRECT -> RenderPipelines.GLINT;
            case CRUMBLING -> RenderPipelines.CRUMBLING;
            case TEXT -> RenderPipelines.TEXT;
            case TEXT_BACKGROUND -> RenderPipelines.TEXT_BACKGROUND;
            case TEXT_INTENSITY -> RenderPipelines.TEXT;
            case TEXT_SEE_THROUGH -> RenderPipelines.TEXT_SEE_THROUGH;
            case TEXT_BACKGROUND_SEE_THROUGH -> RenderPipelines.TEXT_BACKGROUND_SEE_THROUGH;
            case TEXT_INTENSITY_SEE_THROUGH -> RenderPipelines.TEXT_GRAYSCALE_SEE_THROUGH;
            case LIGHTNING -> RenderPipelines.LIGHTNING;
            case TRIPWIRE -> RenderPipelines.WIREFRAME;
            case END_PORTAL -> RenderPipelines.END_PORTAL;
            case END_GATEWAY -> RenderPipelines.END_GATEWAY;
            case LINES -> RenderPipelines.LINES;
            case GUI -> RenderPipelines.GUI;
            case GUI_OVERLAY -> RenderPipelines.GUI_INVERT;
            case GUI_TEXT_HIGHLIGHT -> RenderPipelines.GUI_TEXT_HIGHLIGHT;
            case GUI_GHOST_RECIPE_OVERLAY -> RenderPipelines.GUI_TEXTURED;
        };
        return new MinecraftShader(pipeline);
    }

    @Override
    public VertexFormat getVertexFormat(VertexFormats formats) {
        var format = switch (formats) {
            // 26.2 没有 BLIT_SCREEN，用 POSITION_TEX 顶替
            case BLIT_SCREEN -> DefaultVertexFormat.POSITION_TEX;
            case BLOCK -> DefaultVertexFormat.BLOCK;
            // 26.2 把 NEW_ENTITY 改名为 ENTITY
            case NEW_ENTITY -> DefaultVertexFormat.ENTITY;
            case PARTICLE -> DefaultVertexFormat.PARTICLE;
            case POSITION -> DefaultVertexFormat.POSITION;
            case POSITION_COLOR -> DefaultVertexFormat.POSITION_COLOR;
            case POSITION_COLOR_NORMAL -> DefaultVertexFormat.POSITION_COLOR_NORMAL;
            case POSITION_COLOR_LIGHTMAP -> DefaultVertexFormat.POSITION_COLOR_LIGHTMAP;
            case POSITION_TEX -> DefaultVertexFormat.POSITION_TEX;
            // 26.2 没有 POSITION_COLOR_TEX，用 POSITION_COLOR_TEX_LIGHTMAP 顶替
            case POSITION_COLOR_TEX -> DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP;
            case POSITION_TEX_COLOR -> DefaultVertexFormat.POSITION_TEX_COLOR;
            case POSITION_COLOR_TEX_LIGHTMAP -> DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP;
            case POSITION_TEX_LIGHTMAP_COLOR -> DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR;
            case POSITION_TEX_COLOR_NORMAL -> DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL;
        };
        return () -> format;
    }

    @Override
    public VertexFormat.Mode getVertexFormatMode(VertexFormats.Modes modes) {
        // 26.2 用 com.mojang.blaze3d.PrimitiveTopology 取代了 VertexFormat.Mode
        var mode = switch (modes) {
            case LINES -> PrimitiveTopology.LINES;
            case LINE_STRIP -> PrimitiveTopology.DEBUG_LINE_STRIP;
            case DEBUG_LINES -> PrimitiveTopology.DEBUG_LINES;
            case DEBUG_LINE_STRIP -> PrimitiveTopology.DEBUG_LINE_STRIP;
            case TRIANGLES -> PrimitiveTopology.TRIANGLES;
            case TRIANGLE_STRIP -> PrimitiveTopology.TRIANGLE_STRIP;
            case TRIANGLE_FAN -> PrimitiveTopology.TRIANGLE_FAN;
            case QUADS -> PrimitiveTopology.QUADS;
        };
        return () -> mode;
    }

    // ------------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------------

    private static CompositeStateSpec defaultState() {
        return new CompositeStateSpec(
                null, false, false,
                RenderState.TransparencyState.Type.NO, 0, true, false, false,
                RenderState.LayeringState.Type.NO, RenderState.OutputState.Target.NO,
                true, true, null, RenderState.ColorLogicState.Op.NO_LOGIC, false
        );
    }

    /**
     * 把「顶点格式 + 拓扑 + 渲染状态」映射到最接近的 vanilla {@link RenderType}。
     * <p>
     * 26.2 的 {@code RenderType} 无法自建，所以这是一次<b>降级映射</b>：
     * 顶点格式 / 拓扑 / 混合方式会被尽量保住，深度函数、剔除、颜色逻辑、输出目标等
     * 无法一一对应，只能取最接近的现成条目。
     */
    private static RenderType resolveRenderType(com.mojang.blaze3d.vertex.VertexFormat format, PrimitiveTopology topology, CompositeStateSpec state) {
        var texture = state.texture() != null ? state.texture() : Identifier.withDefaultNamespace("textures/block/stone.png");
        var translucent = state.translucent();

        if (topology == PrimitiveTopology.LINES || topology == PrimitiveTopology.DEBUG_LINES
                || topology == PrimitiveTopology.DEBUG_LINE_STRIP) {
            return translucent ? RenderTypes.LINES_TRANSLUCENT : RenderTypes.LINES;
        }
        if (topology == PrimitiveTopology.POINTS) {
            return RenderTypes.debugPoint();
        }
        if (topology == PrimitiveTopology.TRIANGLE_FAN) {
            return RenderTypes.debugTriangleFan();
        }
        if (topology == PrimitiveTopology.TRIANGLE_STRIP) {
            return RenderTypes.leash();
        }
        if (topology == PrimitiveTopology.TRIANGLES) {
            return RenderTypes.dragonRays();
        }

        // QUADS
        if (has(format, DefaultVertexFormat.NORMAL_SEMANTIC_NAME) && has(format, DefaultVertexFormat.LINE_WIDTH_SEMANTIC_NAME)) {
            // 选区线框 / 包围盒：LINES 家族的格式是 [Position, Color, Normal, LineWidth]
            return translucent ? RenderTypes.LINES_TRANSLUCENT : RenderTypes.LINES;
        }
        if (has(format, DefaultVertexFormat.UV1_SEMANTIC_NAME) && has(format, DefaultVertexFormat.NORMAL_SEMANTIC_NAME)) {
            // ENTITY 格式 [Position, Color, UV0, UV1, UV2, Normal]
            if (translucent) {
                return RenderTypes.entityTranslucent(texture);
            }
            return RenderTypes.entitySolid(texture);
        }
        if (has(format, DefaultVertexFormat.UV0_SEMANTIC_NAME) && has(format, DefaultVertexFormat.UV2_SEMANTIC_NAME)
                && !has(format, DefaultVertexFormat.UV1_SEMANTIC_NAME)) {
            // BLOCK 格式 [Position, Color, UV0, UV2] —— 绑定方块图集
            if (translucent) {
                return RenderTypes.translucentMovingBlock();
            }
            return RenderTypes.solidMovingBlock();
        }
        if (has(format, DefaultVertexFormat.UV0_SEMANTIC_NAME) && !has(format, DefaultVertexFormat.UV2_SEMANTIC_NAME)
                && !has(format, DefaultVertexFormat.UV1_SEMANTIC_NAME)) {
            // 纯纹理 [Position, UV0] —— glint 家族
            return RenderTypes.glint();
        }
        if (has(format, DefaultVertexFormat.UV0_SEMANTIC_NAME) && has(format, DefaultVertexFormat.UV2_SEMANTIC_NAME)) {
            // TEXT 家族 [Position, UV0, UV2, Color]
            return RenderTypes.text(texture);
        }
        // [Position, Color] —— GUI 矩形 / 调试方块
        return RenderTypes.debugFilledBox();
    }

    private static boolean has(com.mojang.blaze3d.vertex.VertexFormat format, String semantic) {
        return format.contains(semantic);
    }

    private static Identifier toIdentifier(ResourceLocation location) {
        if (location == null) {
            return null;
        }
        return Identifier.fromNamespaceAndPath(location.getNamespace(), location.getPath());
    }

}
