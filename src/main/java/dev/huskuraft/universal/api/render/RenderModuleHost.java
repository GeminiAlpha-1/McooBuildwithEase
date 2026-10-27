package dev.huskuraft.universal.api.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import dev.huskuraft.universal.api.platform.PlatformLoader;

/**
 * 渲染模块宿主。
 * <p>
 * <b>这个类是「任意功能坏了都不破坏游戏」这条要求的实现处</b>：
 * <ol>
 *     <li>模块的每个生命周期回调都被单独包在 try/catch 里 →
 *         抛异常只禁用<b>那一个</b>模块，其余模块和游戏本体完全不受影响；</li>
 *     <li>模块可以单独开关，也可以被总开关整体关掉；</li>
 *     <li>模块的注册失败不影响其它模块注册；</li>
 *     <li>失败原因被记录下来（{@link #failures()}），界面能显示「哪个模块挂了、为什么」；
 *         玩家也可以在配置里把它关掉。</li>
 * </ol>
 * <p>
 * 模块通过 {@code @AutoService(RenderModule.class)} 注册，宿主启动时自动发现。
 */
public class RenderModuleHost {

    private final List<RenderModule> modules = new ArrayList<>();
    private final Map<String, Boolean> enabled = new ConcurrentHashMap<>();
    private final Map<String, Throwable> failures = new ConcurrentHashMap<>();
    private final RenderParameterRegistry parameters = new RenderParameterRegistry();
    private final BiConsumer<String, Throwable> logger;

    private volatile boolean masterEnabled = true;
    private volatile boolean started = false;

    public RenderModuleHost(BiConsumer<String, Throwable> logger) {
        this.logger = logger != null ? logger : (m, t) -> {
        };
    }

    // ------------------------------------------------------------------
    // 发现与注册
    // ------------------------------------------------------------------

    /**
     * 发现所有模块（不注册）。
     * <p>
     * 发现本身也要隔离：某个模块的实现类加载失败（比如依赖缺失）不能拖垮整个宿主。
     */
    public void discover() {
        for (var loader : PlatformLoader.load(RenderModule.class)) {
            try {
                var module = loader.get();
                if (module == null) {
                    continue;
                }
                if (modules.stream().anyMatch(m -> m.id().equals(module.id()))) {
                    logger.accept("[render] 模块 id 重复，跳过：" + module.id(), null);
                    continue;
                }
                modules.add(module);
            } catch (Throwable t) {
                logger.accept("[render] 模块加载失败：" + loader.className(), t);
            }
        }
    }

    /**
     * 注册所有已启用的模块（调用它们的 {@code onRegister}）。
     * <p>
     * 参数声明在这里统一收集 —— 模块的参数一定在它被注册前就登记好，
     * 这样即使模块自己注册失败，它的参数也还留在表里（用户能看到并关闭它）。
     */
    public void registerAll() {
        for (var module : modules) {
            for (var parameter : safeParameters(module)) {
                parameters.register(module.id(), parameter);
            }
        }
        started = true;
        for (var module : modules) {
            if (!isModuleEnabled(module.id())) {
                continue;
            }
            safe(module, "注册", ctx -> module.onRegister(ctx));
        }
    }

    private List<RenderParameter> safeParameters(RenderModule module) {
        try {
            return module.parameters() != null ? module.parameters() : List.of();
        } catch (Throwable t) {
            logger.accept("[render] 模块 " + module.id() + " 声明参数时异常", t);
            return List.of();
        }
    }

    // ------------------------------------------------------------------
    // 调度（全部隔离）
    // ------------------------------------------------------------------

    public void tick(float deltaTick) {
        for (var module : modules) {
            safe(module, "tick", ctx -> module.onClientTick(ctx, deltaTick));
        }
    }

    public void renderWorld(float deltaTick) {
        for (var module : modules) {
            safe(module, "世界渲染", ctx -> module.onRenderWorld(ctx, deltaTick));
        }
    }

    public void renderGui(float deltaTick) {
        for (var module : modules) {
            safe(module, "GUI 渲染", ctx -> module.onRenderGui(ctx, deltaTick));
        }
    }

    /**
     * 跑一次回调，出异常就只禁用这一个模块。
     */
    private void safe(RenderModule module, String phase, java.util.function.Consumer<RenderModuleContext> action) {
        if (!isActive(module.id())) {
            return;
        }
        try {
            action.accept(contextFor(module));
        } catch (Throwable t) {
            failures.put(module.id(), t);
            logger.accept("[render] 模块 " + module.id() + " 在「" + phase + "」阶段异常，已自动禁用该模块", t);
        }
    }

    // ------------------------------------------------------------------
    // 开关
    // ------------------------------------------------------------------

    /** 总开关。关掉时所有模块都不再执行。 */
    public boolean isMasterEnabled() {
        return masterEnabled;
    }

    public void setMasterEnabled(boolean enabled) {
        this.masterEnabled = enabled;
    }

    /** 单个模块的开关（默认开）。 */
    public boolean isModuleEnabled(String moduleId) {
        return enabled.getOrDefault(moduleId, Boolean.TRUE);
    }

    /**
     * 开关单个模块。
     * <p>
     * 关闭时会把模块注销（让它释放资源），开启时重新注册。
     */
    public void setModuleEnabled(String moduleId, boolean value) {
        var module = find(moduleId);
        if (module == null) {
            enabled.put(moduleId, value);
            return;
        }
        if (value == isModuleEnabled(moduleId)) {
            return;
        }
        enabled.put(moduleId, value);
        failures.remove(moduleId);
        if (!value) {
            safeUnregister(module);
        } else if (started) {
            for (var parameter : safeParameters(module)) {
                parameters.register(module.id(), parameter);
            }
            safe(module, "注册", ctx -> module.onRegister(ctx));
        }
    }

    private void safeUnregister(RenderModule module) {
        try {
            module.onUnregister(contextFor(module));
        } catch (Throwable t) {
            logger.accept("[render] 模块 " + module.id() + " 注销时异常", t);
        }
    }

    /** 模块当前是否会被执行（总开关 + 单模块开关 + 没失败）。 */
    public boolean isActive(String moduleId) {
        return masterEnabled && isModuleEnabled(moduleId) && !failures.containsKey(moduleId);
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    public List<RenderModule> modules() {
        return Collections.unmodifiableList(modules);
    }

    public RenderModule find(String moduleId) {
        for (var module : modules) {
            if (module.id().equals(moduleId)) {
                return module;
            }
        }
        return null;
    }

    public RenderParameterRegistry parameters() {
        return parameters;
    }

    /** 失败过的模块 → 异常。界面上可以据此提示。 */
    public Map<String, Throwable> failures() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(failures));
    }

    public Set<String> failedModules() {
        return Collections.unmodifiableSet(failures.keySet());
    }

    // ------------------------------------------------------------------
    // 上下文
    // ------------------------------------------------------------------

    private final Map<String, RenderModuleContext> contexts = new ConcurrentHashMap<>();

    /**
     * 拿到某个模块的上下文。
     * <p>
     * 上下文由平台层提供（{@link RenderModuleContextFactory}），
     * 这样 API 层不需要知道 Minecraft 的任何东西。
     */
    public RenderModuleContext contextFor(RenderModule module) {
        return contexts.computeIfAbsent(module.id(), id -> {
            var factory = RenderModuleContextFactory.get();
            if (factory == null) {
                throw new IllegalStateException("渲染模块上下文工厂未初始化（平台层没接上）");
            }
            return factory.create(id, parameters);
        });
    }

}
