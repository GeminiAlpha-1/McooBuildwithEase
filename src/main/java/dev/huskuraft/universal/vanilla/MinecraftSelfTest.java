package dev.huskuraft.universal.vanilla;

import java.util.Arrays;

import dev.huskuraft.universal.api.networking.NetByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NBT 序列化的自检。
 * <p>
 * 用 {@code -Dmcoo.selfTest=nbt} 触发：构造一个含三种数组标签的 CompoundTag，
 * 走一遍 {@link NetByteBuf} 的序列化 → 反序列化，比对结果。
 * <p>
 * 存在的意义：NBT 的数组标签（byte array / int array / long array）曾经在
 * 网络序列化里没有实现，带这类标签的方块实体一复制就抛
 * {@code IllegalArgumentException: Unsupported tag!} —— 服务端复制成功、
 * 但快照发不出去，表现为「提示复制成功，历史列表却是空的，右键也放不出来」。
 */
public final class MinecraftSelfTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftSelfTest.class);
    private static boolean ran = false;

    private MinecraftSelfTest() {
    }

    public static void runIfRequested() {
        if (ran) {
            return;
        }
        var kind = System.getProperty("mcoo.selfTest");
        if (kind == null || kind.isBlank()) {
            return;
        }
        ran = true;
        try {
            if ("nbt".equals(kind)) {
                testNbtArrays();
            } else {
                LOGGER.warn("[self-test] 未知的自检类型: {}", kind);
            }
        } catch (Throwable throwable) {
            LOGGER.error("[self-test] 失败: {}", kind, throwable);
        }
    }

    private static void testNbtArrays() {
        var original = new CompoundTag();
        original.put("byteArray", new ByteArrayTag(new byte[]{1, 2, 3, -4, 5}));
        original.put("intArray", new IntArrayTag(new int[]{10, -20, 30, -40}));
        original.put("longArray", new LongArrayTag(new long[]{100L, -200L, 300L}));
        original.putString("text", "hello");
        var nested = new CompoundTag();
        nested.putString("inner", "value");
        original.put("nested", nested);
        var list = new ListTag();
        list.add(StringTag.valueOf("a"));
        list.add(StringTag.valueOf("b"));
        original.put("list", list);

        var buffer = Unpooled.buffer();
        var byteBuf = new NetByteBuf(buffer);
        byteBuf.writeRecordTag(dev.huskuraft.universal.vanilla.tag.MinecraftRecordTag.ofNullable(original));

        var readBack = new NetByteBuf(buffer);
        var result = readBack.readRecordTag();

        var byteArray = result.getTag("byteArray").asByteArray().getAsByteArray();
        var intArray = result.getTag("intArray").asIntArray().getAsIntArray();
        var longArray = result.getTag("longArray").asLongArray().getAsLongArray();
        var text = result.getString("text");
        var inner = result.getTag("nested").asRecord().getString("inner");
        var listSize = result.getTag("list").asList().size();

        var ok = Arrays.equals(byteArray, new byte[]{1, 2, 3, -4, 5})
                && Arrays.equals(intArray, new int[]{10, -20, 30, -40})
                && Arrays.equals(longArray, new long[]{100L, -200L, 300L})
                && "hello".equals(text)
                && "value".equals(inner)
                && listSize == 2;

        LOGGER.info("[self-test] NBT 数组标签往返: {} (byte={} int={} long={} text={} inner={} list={})",
                ok ? "通过" : "失败",
                Arrays.toString(byteArray),
                Arrays.toString(intArray),
                Arrays.toString(longArray),
                text, inner, listSize);
    }
}
