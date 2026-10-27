package dev.huskuraft.universal.api.render;

/**
 * 渲染参数的类型。
 * <p>
 * 参数是这个框架的「通用货币」：每个渲染模块自己声明它需要哪些参数，
 * 宿主统一收集、统一校验、统一持久化、统一生成 UI —— 模块自己不碰配置文件。
 */
public enum ParameterType {

    /** 开关。 */
    BOOLEAN,
    /** 整数（带范围）。 */
    INT,
    /** 小数（带范围）。 */
    FLOAT,
    /** 文本。 */
    STRING,
    /** 颜色（ARGB 整数）。 */
    COLOR,
    /** 枚举选项。 */
    CHOICE,

}
