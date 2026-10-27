package com.mcoo.buildwithease.building.config.universal;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.mcoo.buildwithease.building.config.ClientConfig;
import com.mcoo.buildwithease.building.config.ClipboardConfig;
import com.mcoo.buildwithease.building.config.PatternConfig;
import com.mcoo.buildwithease.building.config.RenderConfig;
import dev.huskuraft.universal.api.config.ConfigSerializer;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigSpec;

public class ClientConfigConfigSerializer implements ConfigSerializer<ClientConfig> {

    private static final String KEY_RENDER = "render";
    private static final String KEY_SHOW_OTHER_PLAYERS_BUILD = "showOtherPlayersBuild";
    //    private static final String KEY_SHOW_OTHER_PLAYERS_BUILD_TOOLTIPS = "showOtherPlayersBuildTooltips";
    private static final String KEY_SHOW_BLOCK_PREVIEW = "showBlockPreview";
    private static final String KEY_MAX_RENDER_VOLUME = "maxRenderVolume";
    /** 自建渲染器总开关。 */
    private static final String KEY_RENDERER_ENABLED = "rendererEnabled";
    /** 每个渲染模块的开关（键 = 模块 id）。 */
    private static final String KEY_MODULE_ENABLED = "moduleEnabled";
    /** 渲染参数值（键 = {@code 模块id.参数key}）。 */
    private static final String KEY_PARAMETERS = "parameters";

    private static final String KEY_BUILDER = "builder";
    private static final String KEY_PATTERN = "pattern";
    private static final String KEY_TRANSFORMER_PRESETS = "transformerPresets";
    private static final String KEY_CLIPBOARD = "clipboard";
    private static final String KEY_COLLECTIONS = "collections";

    private static final String KEY_RESERVED_TOOL_DURABILITY = "reservedToolDurability";


    @Override
    public ConfigSpec getSpec(Config config) {
        var spec = new ConfigSpec();

        spec.define(List.of(KEY_RENDER, KEY_SHOW_BLOCK_PREVIEW), () -> getDefault().renderConfig().showBlockPreview(), Boolean.class::isInstance);
        spec.define(List.of(KEY_RENDER, KEY_SHOW_OTHER_PLAYERS_BUILD), () -> getDefault().renderConfig().showOtherPlayersBuild(), Boolean.class::isInstance);
//        spec.define(List.of(KEY_RENDER, KEY_SHOW_OTHER_PLAYERS_BUILD_TOOLTIPS), () -> getDefault().renderConfig().showOtherPlayersBuildTooltips(), Boolean.class::isInstance);
        spec.defineInRange(List.of(KEY_RENDER, KEY_MAX_RENDER_VOLUME), getDefault().renderConfig().maxRenderVolume(), RenderConfig.MAX_RENDER_VOLUME_MIN, RenderConfig.MAX_RENDER_VOLUME_MAX);
//        spec.defineInRange(List.of(KEY_RENDER, KEY_MAX_RENDER_DISTANCE), () -> getDefault().renderConfig().maxRenderDistance(), RenderConfig.MIN_MAX_RENDER_DISTANCE, RenderConfig.MAX_MAX_RENDER_DISTANCE);
        spec.define(List.of(KEY_RENDER, KEY_RENDERER_ENABLED), () -> getDefault().renderConfig().rendererEnabled(), Boolean.class::isInstance);
        // night-config 的 ConfigSpec 没有 Map 支持，这两张表用「key=value」字符串列表存
        spec.defineList(List.of(KEY_RENDER, KEY_MODULE_ENABLED), () -> List.of(), s -> s instanceof String);
        spec.defineList(List.of(KEY_RENDER, KEY_PARAMETERS), () -> List.of(), s -> s instanceof String);
        spec.defineList(List.of(KEY_PATTERN, KEY_TRANSFORMER_PRESETS), () -> getDefault().patternConfig().itemRandomizers().stream().map(TransformerConfigSerializer.INSTANCE::serialize).toList(), Config.class::isInstance);
        spec.defineList(List.of(KEY_CLIPBOARD, KEY_COLLECTIONS), () -> getDefault().clipboardConfig().collections().stream().map(SnapshotConfigSerializer.INSTANCE::serialize).toList(), Config.class::isInstance);
//        spec.define(KEY_PASSIVE_MODE, () -> getDefault().passiveMode(), Boolean.class::isInstance);
        spec.defineInRange(List.of(KEY_BUILDER, KEY_RESERVED_TOOL_DURABILITY), getDefault().builderConfig().reservedToolDurability(), 0, 32);

        return spec;
    }

    @Override
    public ClientConfig deserialize(Config config) {
        validate(config);
        return new ClientConfig(
                new RenderConfig(
                        config.get(List.of(KEY_RENDER, KEY_SHOW_BLOCK_PREVIEW)),
                        config.get(List.of(KEY_RENDER, KEY_SHOW_OTHER_PLAYERS_BUILD)),
                        false,
//                        config.get(List.of(KEY_RENDER, KEY_SHOW_OTHER_PLAYERS_BUILD_TOOLTIPS)),
                        config.get(List.of(KEY_RENDER, KEY_MAX_RENDER_VOLUME)),
                        128,
                        config.get(List.of(KEY_RENDER, KEY_RENDERER_ENABLED)),
                        parseModuleEnabled(config.get(List.of(KEY_RENDER, KEY_MODULE_ENABLED))),
                        parseParameters(config.get(List.of(KEY_RENDER, KEY_PARAMETERS)))
                ),
                new PatternConfig(
                        config.<List<Config>>get(List.of(KEY_PATTERN, KEY_TRANSFORMER_PRESETS)).stream().map(TransformerConfigSerializer.INSTANCE::deserialize).toList()
                ),
                new ClipboardConfig(
                        config.<List<Config>>get(List.of(KEY_CLIPBOARD, KEY_COLLECTIONS)).stream().map(SnapshotConfigSerializer.INSTANCE::deserialize).toList(),
                        List.of()
                )
        );
    }

    @Override
    public Config serialize(ClientConfig settings) {
        var config = CommentedConfig.inMemory();
        config.set(List.of(KEY_RENDER, KEY_SHOW_BLOCK_PREVIEW), settings.renderConfig().showBlockPreview());
        config.set(List.of(KEY_RENDER, KEY_SHOW_OTHER_PLAYERS_BUILD), settings.renderConfig().showOtherPlayersBuild());
//        config.set(List.of(KEY_RENDER, KEY_SHOW_OTHER_PLAYERS_BUILD_TOOLTIPS), settings.renderConfig().showOtherPlayersBuildTooltips());
        config.set(List.of(KEY_RENDER, KEY_MAX_RENDER_VOLUME), settings.renderConfig().maxRenderVolume());
        config.set(List.of(KEY_RENDER, KEY_RENDERER_ENABLED), settings.renderConfig().rendererEnabled());
        config.set(List.of(KEY_RENDER, KEY_MODULE_ENABLED), formatModuleEnabled(settings.renderConfig().moduleEnabled()));
        config.set(List.of(KEY_RENDER, KEY_PARAMETERS), formatParameters(settings.renderConfig().parameters()));
        config.set(List.of(KEY_PATTERN, KEY_TRANSFORMER_PRESETS), settings.patternConfig().itemRandomizers().stream().map(TransformerConfigSerializer.INSTANCE::serialize).filter(Objects::nonNull).toList());
        config.set(List.of(KEY_CLIPBOARD, KEY_COLLECTIONS), settings.clipboardConfig().collections().stream().map(SnapshotConfigSerializer.INSTANCE::serialize).filter(Objects::nonNull).toList());
        validate(config);
        return config;
    }

    @Override
    public ClientConfig getDefault() {
        return ClientConfig.DEFAULT;
    }

    // ------------------------------------------------------------------
    // 渲染模块的开关 / 参数：night-config 没有 Map 支持，用「key=value」字符串列表存
    // ------------------------------------------------------------------

    private static List<String> formatModuleEnabled(Map<String, Boolean> moduleEnabled) {
        return moduleEnabled.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .sorted()
                .toList();
    }

    private static Map<String, Boolean> parseModuleEnabled(List<String> values) {
        var result = new java.util.LinkedHashMap<String, Boolean>();
        if (values == null) {
            return result;
        }
        for (var value : values) {
            var index = value.indexOf('=');
            if (index <= 0) {
                continue;
            }
            result.put(value.substring(0, index), Boolean.parseBoolean(value.substring(index + 1)));
        }
        return result;
    }

    private static List<String> formatParameters(Map<String, Object> parameters) {
        return parameters.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .sorted()
                .toList();
    }

    /**
     * 解析参数表。值一律先按字符串存 ——
     * 真正的类型转换由渲染框架按参数声明来做（{@code RenderParameter#coerce}），
     * 这样配置里写错类型也不会炸。
     */
    private static Map<String, Object> parseParameters(List<String> values) {
        var result = new java.util.LinkedHashMap<String, Object>();
        if (values == null) {
            return result;
        }
        for (var value : values) {
            var index = value.indexOf('=');
            if (index <= 0) {
                continue;
            }
            result.put(value.substring(0, index), value.substring(index + 1));
        }
        return result;
    }

}
