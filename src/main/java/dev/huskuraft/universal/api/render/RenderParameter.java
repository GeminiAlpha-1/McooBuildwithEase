package dev.huskuraft.universal.api.render;

import java.util.List;

/**
 * 一个可调参数的<b>声明</b>。
 * <p>
 * 只描述「这个参数是什么、范围多少、默认值多少」，<b>不持有值</b> ——
 * 值统一存在 {@link RenderParameterRegistry} 里。
 * 这样参数的声明可以自由地散布在各模块里，而读写、校验、持久化只有一处实现。
 *
 * @param key         模块内唯一的键
 * @param type        类型
 * @param labelKey    界面显示用的语言键
 * @param defaultValue 默认值
 * @param min         数值下限（非数值型为 0）
 * @param max         数值上限（非数值型为 0）
 * @param choices     可选项（{@link ParameterType#CHOICE} 用）
 * @param advanced    是否是「高级」参数（UI 里可以折叠）
 */
public record RenderParameter(
        String key,
        ParameterType type,
        String labelKey,
        Object defaultValue,
        double min,
        double max,
        List<String> choices,
        boolean advanced
) {

    public static RenderParameter ofBoolean(String key, String labelKey, boolean defaultValue) {
        return new RenderParameter(key, ParameterType.BOOLEAN, labelKey, defaultValue, 0, 0, List.of(), false);
    }

    public static RenderParameter ofBoolean(String key, String labelKey, boolean defaultValue, boolean advanced) {
        return new RenderParameter(key, ParameterType.BOOLEAN, labelKey, defaultValue, 0, 0, List.of(), advanced);
    }

    public static RenderParameter ofInt(String key, String labelKey, int defaultValue, int min, int max) {
        return new RenderParameter(key, ParameterType.INT, labelKey, defaultValue, min, max, List.of(), false);
    }

    public static RenderParameter ofInt(String key, String labelKey, int defaultValue, int min, int max, boolean advanced) {
        return new RenderParameter(key, ParameterType.INT, labelKey, defaultValue, min, max, List.of(), advanced);
    }

    public static RenderParameter ofFloat(String key, String labelKey, float defaultValue, float min, float max) {
        return new RenderParameter(key, ParameterType.FLOAT, labelKey, defaultValue, min, max, List.of(), false);
    }

    public static RenderParameter ofFloat(String key, String labelKey, float defaultValue, float min, float max, boolean advanced) {
        return new RenderParameter(key, ParameterType.FLOAT, labelKey, defaultValue, min, max, List.of(), advanced);
    }

    public static RenderParameter ofString(String key, String labelKey, String defaultValue) {
        return new RenderParameter(key, ParameterType.STRING, labelKey, defaultValue, 0, 0, List.of(), false);
    }

    public static RenderParameter ofColor(String key, String labelKey, int defaultArgb) {
        return new RenderParameter(key, ParameterType.COLOR, labelKey, defaultArgb, 0, 0, List.of(), false);
    }

    public static RenderParameter ofChoice(String key, String labelKey, String defaultValue, List<String> choices) {
        return new RenderParameter(key, ParameterType.CHOICE, labelKey, defaultValue, 0, 0, List.copyOf(choices), false);
    }

    /** 带「默认值 + 范围」的完整声明，供需要自定义 advanced 标记时使用。 */
    public static RenderParameter of(RenderParameter parameter, boolean advanced) {
        return new RenderParameter(parameter.key(), parameter.type(), parameter.labelKey(), parameter.defaultValue(),
                parameter.min(), parameter.max(), parameter.choices(), advanced);
    }

    /** 把任意值转成这个参数的类型（用于从配置读回时的兜底）。 */
    public Object coerce(Object value) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return switch (type) {
                case BOOLEAN -> value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
                case INT -> value instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(value));
                case FLOAT -> value instanceof Number n ? n.floatValue() : Float.parseFloat(String.valueOf(value));
                case COLOR -> value instanceof Number n ? n.intValue() : (int) Long.parseLong(String.valueOf(value));
                case STRING, CHOICE -> String.valueOf(value);
            };
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /** 把值夹到合法范围内（数值型）。 */
    public Object clamp(Object value) {
        var coerced = coerce(value);
        if (coerced instanceof Number number) {
            double d = number.doubleValue();
            if (type == ParameterType.INT) {
                return (int) Math.max(min, Math.min(max, d));
            }
            if (type == ParameterType.FLOAT) {
                return (float) Math.max(min, Math.min(max, d));
            }
        }
        if (type == ParameterType.CHOICE && !choices.isEmpty() && !choices.contains(String.valueOf(coerced))) {
            return defaultValue;
        }
        return coerced;
    }

}
