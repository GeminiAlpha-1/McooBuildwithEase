package dev.huskuraft.universal.vanilla.platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.auto.service.AutoService;

import dev.huskuraft.universal.api.core.ItemStack;
import dev.huskuraft.universal.api.input.KeyBinding;
import dev.huskuraft.universal.api.platform.ClientContentFactory;
import dev.huskuraft.universal.api.platform.SearchBy;
import dev.huskuraft.universal.api.platform.SearchTree;
import dev.huskuraft.universal.api.text.Text;
import dev.huskuraft.universal.vanilla.core.MinecraftItemStack;
import dev.huskuraft.universal.vanilla.input.MinecraftKeyBinding;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
//import net.minecraft.client.searchtree.PlainTextSearchTree;
//import net.minecraft.client.searchtree.SearchRegistry;
//import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTabs;

@AutoService(ClientContentFactory.class)
public class MinecraftClientContentFactory extends MinecraftContentFactory implements ClientContentFactory {

    private static final String NAMESPACE = "mcoo_build_with_ease";
    private static final Map<String, KeyMapping.Category> CATEGORIES = new ConcurrentHashMap<>();

    @Override
    public SearchTree<ItemStack> searchItemStack(SearchBy searchBy) {
        var minecraft = Minecraft.getInstance();
        var minecraftPlayer = minecraft.player;
        // 26.2 起 LocalPlayer 不再持有 clientLevel，当前世界统一从 Minecraft.level 取
        CreativeModeTabs.tryRebuildTabContents(minecraftPlayer.connection.enabledFeatures(), true, minecraft.level.registryAccess());

        var minecraftSearchTrees = minecraft.getConnection().searchTrees();
        var minecraftSearchTree = switch (searchBy) {
            case NAME -> minecraftSearchTrees.creativeNameSearch();
            case TAG -> minecraftSearchTrees.creativeTagSearch();
        };

        return query -> minecraftSearchTree.search(query).stream().map(itemStack -> new MinecraftItemStack(itemStack)).collect(Collectors.toList());
    }

    @Override
    public <T> SearchTree<T> search(List<T> list, Function<T, Stream<Text>> keyExtractor) {
        return null;
    }

    @Override
    public KeyBinding newKeyBinding(String name, String category, int code) {
        return new MinecraftKeyBinding(new KeyMapping(name, code, category(category)));
    }

    // 26.2 的 KeyMapping 分类不再是翻译键字符串，而是 KeyMapping.Category（内部包一个 Identifier），
    // 显示名由 id.toLanguageKey("key.category") 生成，即 "key.category.<命名空间>.<路径>"。
    // 这里把上游传入的分类翻译键（key.mcoo_build_with_ease.category.xxx）转成 26.2 的 Category。
    private static KeyMapping.Category category(String category) {
        var path = category.substring(category.lastIndexOf('.') + 1);
        return CATEGORIES.computeIfAbsent(path, key -> KeyMapping.Category.register(Identifier.fromNamespaceAndPath(NAMESPACE, key)));
    }

}
