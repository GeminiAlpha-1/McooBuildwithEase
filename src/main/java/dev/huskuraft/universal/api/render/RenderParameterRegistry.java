package dev.huskuraft.universal.api.render;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 渲染参数表。
 * <p>
 * <b>全工程只有这一处存参数值</b>：模块声明、宿主收集、UI 改、配置文件读写，
 * 都走这里。好处是：
 * <ul>
 *     <li>参数不会散落在各模块里互相踩；</li>
 *     <li>加一个参数只要在模块里加一行声明，UI 和持久化自动就有了；</li>
 *     <li>范围校验只有一处实现，不会出现「A 模块以为值是 0~1、B 模块写进去 100」这种不一致。</li>
 * </ul>
 * 键的格式是 {@code <模块id>.<参数key>}。
 */
public class RenderParameterRegistry {

    private final Map<String, RenderParameter> definitions = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, Object> values = new ConcurrentHashMap<>();

    private static String key(String moduleId, RenderParameter parameter) {
        return moduleId + "." + parameter.key();
    }

    /** 登记一个参数声明（重复登记以最后一次为准）。 */
    public void register(String moduleId, RenderParameter parameter) {
        var k = key(moduleId, parameter);
        definitions.put(k, parameter);
        values.putIfAbsent(k, parameter.defaultValue());
    }

    /** 这个模块是否声明过这个参数。 */
    public boolean isRegistered(String moduleId, RenderParameter parameter) {
        return definitions.containsKey(key(moduleId, parameter));
    }

    /** 读参数值（未登记时返回声明里的默认值）。 */
    public Object get(String moduleId, RenderParameter parameter) {
        var k = key(moduleId, parameter);
        var value = values.get(k);
        if (value == null) {
            return parameter.defaultValue();
        }
        return parameter.clamp(value);
    }

    /** 写参数值（会做类型转换与范围钳制）。 */
    public Object set(String moduleId, RenderParameter parameter, Object value) {
        var k = key(moduleId, parameter);
        var clamped = parameter.clamp(value);
        values.put(k, clamped);
        return clamped;
    }

    /** 把参数值恢复成默认值。 */
    public void reset(String moduleId, RenderParameter parameter) {
        values.put(key(moduleId, parameter), parameter.defaultValue());
    }

    /** 全部参数声明（键 = {@code 模块id.参数key}）。 */
    public Map<String, RenderParameter> definitions() {
        synchronized (definitions) {
            return Collections.unmodifiableMap(new LinkedHashMap<>(definitions));
        }
    }

    /** 某模块声明的全部参数（键 = {@code 模块id.参数key}）。 */
    public Map<String, RenderParameter> definitions(String moduleId) {
        var result = new LinkedHashMap<String, RenderParameter>();
        synchronized (definitions) {
            definitions.forEach((k, v) -> {
                if (k.startsWith(moduleId + ".")) {
                    result.put(k, v);
                }
            });
        }
        return Collections.unmodifiableMap(result);
    }

    /** 当前所有参数值（用于持久化）。 */
    public Map<String, Object> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    /** 从持久化数据恢复（忽略没登记过的键，值会被钳制到合法范围）。 */
    public void restore(Map<String, Object> data) {
        if (data == null) {
            return;
        }
        data.forEach((k, v) -> {
            var definition = definitions.get(k);
            if (definition != null) {
                values.put(k, definition.clamp(v));
            }
        });
    }

}
