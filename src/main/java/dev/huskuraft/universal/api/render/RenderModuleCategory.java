package dev.huskuraft.universal.api.render;

/**
 * 渲染模块的分类。
 * <p>
 * 分类只是给 UI 分组用的（面板里按分类折叠），不影响模块的行为。
 * 新增分类时记得在语言文件里补一条 {@code render.category.*}。
 */
public enum RenderModuleCategory {

    /** 天空 / 太阳 / 月亮。 */
    SKY,
    /** 光照 / 阴影。 */
    LIGHTING,
    /** 辉光 / 泛光。 */
    BLOOM,
    /** 体积光 / 上帝光。 */
    GODRAYS,
    /** 云。 */
    CLOUDS,
    /** 水 / 流体。 */
    WATER,
    /** 天气 / 雾。 */
    WEATHER,
    /** 色调映射 / 色彩分级。 */
    TONEMAP,
    /** 抗锯齿。 */
    ANTIALIAS,
    /** 材质（PBR / 反射 / 法线贴图）。 */
    MATERIALS,
    /** 自定义光源。 */
    LIGHTS,
    /** 植被摆动 / 粒子。 */
    FOLIAGE,
    /** 画质档位 / 性能。 */
    QUALITY,
    /** 其它。 */
    GENERAL,

}
