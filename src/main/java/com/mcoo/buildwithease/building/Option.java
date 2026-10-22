package com.mcoo.buildwithease.building;

import com.mcoo.buildwithease.McooBuildwithEase;
import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.lang.Lang;
import dev.huskuraft.universal.api.text.Text;

public interface Option {

    String getName();

    String getCategory();

    default String getNameKey() {
        return Lang.asKey(McooBuildwithEase.MOD_ID, "action.%s".formatted(getName()));
    }

    default String getCategoryKey() {
        return Lang.asKey(McooBuildwithEase.MOD_ID, "option.%s".formatted(getCategory()));
    }

    default String getTooltipKey() {
        return Lang.asKey(McooBuildwithEase.MOD_ID, "action.%s.tooltip".formatted(getName()));
    }

    default Text getNameText() {
        return Text.translate(getNameKey());
    }

    default Text getCategoryText() {
        return Text.translate(getCategoryKey());
    }

    default Text getTooltipText() {
        if (Lang.hasKey(getTooltipKey())) {
            return Text.translate(getTooltipKey());
        } else {
            return Text.empty();
        }
    }

    default ResourceLocation getIcon() {
        return ResourceLocation.of(McooBuildwithEase.MOD_ID, "textures/option/%s.png".formatted(getName()));
    }

}
