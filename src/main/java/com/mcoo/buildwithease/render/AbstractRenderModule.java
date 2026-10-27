package com.mcoo.buildwithease.render;

import dev.huskuraft.universal.api.render.RenderModule;
import dev.huskuraft.universal.api.render.RenderModuleCategory;

/**
 * 渲染模块的基类：只处理 id / 分类这些样板。
 * <p>
 * <b>每个模块都是独立的一格</b>：自己声明参数、自己实现生命周期、
 * 出了事只禁用自己（隔离由 {@code RenderModuleHost} 统一兜底）。
 */
public abstract class AbstractRenderModule implements RenderModule {

    private final String id;
    private final RenderModuleCategory category;

    protected AbstractRenderModule(String id, RenderModuleCategory category) {
        this.id = id;
        this.category = category;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public RenderModuleCategory category() {
        return category;
    }

    @Override
    public String nameKey() {
        return "mcoo_build_with_ease.render.module." + id;
    }

    /** 参数标签的语言键前缀。 */
    protected static String paramKey(String moduleId, String parameter) {
        return "mcoo_build_with_ease.render.param." + moduleId + "." + parameter;
    }

}
