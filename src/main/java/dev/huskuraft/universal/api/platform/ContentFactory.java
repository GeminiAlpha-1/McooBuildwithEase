package dev.huskuraft.universal.api.platform;

import java.util.Locale;
import java.util.Optional;

import dev.huskuraft.universal.api.core.Block;
import dev.huskuraft.universal.api.core.Item;
import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.core.Items;
import dev.huskuraft.universal.api.core.ResourceLocation;
import dev.huskuraft.universal.api.core.StatType;
import dev.huskuraft.universal.api.core.StatTypes;
import dev.huskuraft.universal.api.core.fluid.Fluid;
import dev.huskuraft.universal.api.core.fluid.Fluids;
import dev.huskuraft.universal.api.sound.Sound;
import dev.huskuraft.universal.api.sound.Sounds;
import dev.huskuraft.universal.api.tag.InputStreamTagReader;
import dev.huskuraft.universal.api.tag.OutputStreamTagWriter;
import dev.huskuraft.universal.api.text.Text;

public interface ContentFactory {

    static ContentFactory getInstance() {
        return PlatformLoader.getSingleton();
    }

    ResourceLocation newResourceLocation(String namespace, String path);

    Optional<Item> newOptionalItem(ResourceLocation location);

    default Item newItem(ResourceLocation location) {
        return newOptionalItem(location).orElseThrow();
    }

    /**
     * 按注册表名字找方块。
     * <p>
     * 有些方块没有对应的物品（水、岩浆就是这样），
     * 想拿到它们的状态只能走方块注册表。
     */
    Optional<Block> newOptionalBlock(ResourceLocation location);

    default Block newBlock(ResourceLocation location) {
        return newOptionalBlock(location).orElseThrow();
    }

    ItemStack newItemStack();

    ItemStack newItemStack(Item item, int count);

    Text newText();

    Text newText(String text);

    Text newTranslatableText(String text);

    Text newTranslatableText(String text, Object... args);

    InputStreamTagReader getInputStreamTagReader();

    OutputStreamTagWriter getOutputStreamTagWriter();

    OperatingSystem getOperatingSystem();

    Sound getSound(Sounds sounds);

    default Optional<Item> getOptionalItem(Items items) {
        return newOptionalItem(ResourceLocation.of("minecraft", items.name().toLowerCase(Locale.ROOT)));
    }

    default Item getItem(Items items) {
        return getOptionalItem(items).orElseThrow();
    }

    Fluid getFluid(Fluids fluids);

    <T extends PlatformReference> StatType<T> getStatType(StatTypes statTypes);

}
