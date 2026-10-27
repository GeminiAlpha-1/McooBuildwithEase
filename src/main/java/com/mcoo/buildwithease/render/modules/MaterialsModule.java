package com.mcoo.buildwithease.render.modules;

import java.util.List;

import com.google.auto.service.AutoService;

import com.mcoo.buildwithease.render.AbstractRenderModule;
import dev.huskuraft.universal.api.render.RenderModuleCategory;
import dev.huskuraft.universal.api.render.RenderParameter;

/**
 * 材质（PBR / 反射 / 法线贴图 / 自发光 / 环境光遮蔽）。
 */
@AutoService(dev.huskuraft.universal.api.render.RenderModule.class)
public class MaterialsModule extends AbstractRenderModule {

    public static final String ID = "materials";

    public static final RenderParameter SOURCE = RenderParameter.ofChoice(
            paramKey(ID, "source"), paramKey(ID, "source"), "off", List.of("off", "builtin", "labpbr"));
    public static final RenderParameter REFLECTIONS = RenderParameter.ofChoice(
            paramKey(ID, "reflections"), paramKey(ID, "reflections"), "off", List.of("off", "sky", "ssr"));
    public static final RenderParameter NORMAL_MAP = RenderParameter.ofBoolean(
            paramKey(ID, "normalMap"), paramKey(ID, "normalMap"), false);
    public static final RenderParameter EMISSIVE_MAP = RenderParameter.ofBoolean(
            paramKey(ID, "emissiveMap"), paramKey(ID, "emissiveMap"), false);
    public static final RenderParameter AO_MAP = RenderParameter.ofBoolean(
            paramKey(ID, "aoMap"), paramKey(ID, "aoMap"), false);

    public MaterialsModule() {
        super(ID, RenderModuleCategory.MATERIALS);
    }

    @Override
    public List<RenderParameter> parameters() {
        return List.of(SOURCE, REFLECTIONS, NORMAL_MAP, EMISSIVE_MAP, AO_MAP);
    }

}
