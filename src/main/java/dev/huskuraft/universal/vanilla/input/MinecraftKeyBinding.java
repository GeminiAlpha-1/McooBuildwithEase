package dev.huskuraft.universal.vanilla.input;

import dev.huskuraft.universal.api.input.Key;
import dev.huskuraft.universal.api.input.KeyBinding;
import net.minecraft.client.KeyMapping;

public record MinecraftKeyBinding(
        KeyMapping refs
) implements KeyBinding {

    @Override
    public String getName() {
        return refs.getName();
    }

    /**
     * 26.2 起 {@code KeyMapping#getCategory()} 返回的是 {@link KeyMapping.Category}（记录类型）而不是字符串。
     * 这里还原成原来的「分类翻译键」形态：{@code key.category.<namespace>.<path>}。
     */
    @Override
    public String getCategory() {
        return refs.getCategory().id().toLanguageKey("key.category");
    }

    @Override
    public Key getDefaultKey() {
        return new MinecraftKey(refs.getDefaultKey());
    }

    /**
     * {@code KeyMapping#key} 在 26.2 变成 protected，靠 access widener 打开
     * （见 src/main/resources/mcoo_build_with_ease.accesswidener）。
     */
    @Override
    public Key getKey() {
        return new MinecraftKey(refs.key);
    }

    @Override
    public boolean consumeClick() {
        return refs.consumeClick();
    }

    @Override
    public boolean isDown() {
        return refs.isDown();
    }

}
