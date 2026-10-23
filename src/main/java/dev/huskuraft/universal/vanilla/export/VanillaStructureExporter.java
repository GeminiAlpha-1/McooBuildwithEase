package dev.huskuraft.universal.vanilla.export;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.auto.service.AutoService;

import dev.huskuraft.universal.api.core.BlockState;
import dev.huskuraft.universal.api.export.StructureExporter;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

/**
 * 把结构写成各种建模工具能读的工程文件。
 * <p>
 * 三种格式都是「gzip 压缩的大端 NBT」：
 * <ul>
 *     <li><b>投影 / Litematica</b>（{@code .litematic}）：
 *     根标签 {@code Version / MinecraftDataVersion / Metadata / Regions}，
 *     每个 region 是一份调色板 + 位打包的方块索引。
 *     索引顺序是 x 变化最快（{@code x + z*sizeX + y*sizeX*sizeZ}），
 *     位宽 {@code max(2, ceil(log2(调色板大小)))}，低位在前、允许跨 64 位边界。</li>
 *     <li><b>WorldEdit / Sponge Schematic v3</b>（{@code .schem}）：
 *     根标签 {@code Schematic}，{@code Palette} 是「方块状态字符串 → 序号」的复合标签，
 *     {@code Data} 是 varint 编码的序号流（顺序 y → z → x）。</li>
 *     <li><b>Axiom 蓝图</b>（{@code .bp}）：Axiom 自己的蓝图格式。</li>
 * </ul>
 */
@AutoService(StructureExporter.class)
public class VanillaStructureExporter implements StructureExporter {

    /** 导出目录（在游戏目录下）。 */
    public static final String EXPORT_DIR = "mcoo_build_with_ease_exports";
    /** Axiom 的蓝图目录。 */
    public static final String AXIOM_BLUEPRINT_DIR = "Axiom/blueprints";
    /** 投影 / WorldEdit 的工程文件目录（社区惯例放在实例目录下）。 */
    public static final String SCHEMATICS_DIR = "schematics";
    /** 投影（Malte's Litematica）的 mod id。 */
    private static final String MOD_LITEMATICA = "litematica";
    /** WorldEdit 的 mod id。 */
    private static final String MOD_WORLDEDIT = "worldedit";
    /** Axiom 的 mod id。 */
    private static final String MOD_AXIOM = "axiom";

    @Override
    public Path export(Path directory, String fileName0, int sizeX, int sizeY, int sizeZ, List<Block> blocks, Format format) {
        var baseName = sanitize(fileName0);
        // 用户可能自己敲了扩展名，别加成 foo.litematic.litematic
        var fileName = baseName.toLowerCase().endsWith(format.extension())
                ? baseName
                : baseName + format.extension();
        var root = switch (format) {
            case LITEMATIC -> writeLitematic(baseName, sizeX, sizeY, sizeZ, blocks);
            case WORLDEDIT, AXIOM -> writeWorldEdit(baseName, sizeX, sizeY, sizeZ, blocks);
        };
        try {
            Files.createDirectories(directory);
            var path = directory.resolve(fileName);
            NbtIo.writeCompressed(root, path);
            return path;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to export structure to " + format, e);
        }
    }

    @Override
    public Path defaultDirectory(Format format) {
        var gameDir = Minecraft.getInstance().gameDirectory.toPath();
        // 目标目录：**装了就用它自己的目录，没装就放模组自己的导出目录**。
        // 这样玩家点「下载投影」时，文件直接落在投影的 schematics 目录里，投影立刻就能看到；
        // 没装投影的话也不会丢在一个没人认识的地方。
        var platform = dev.huskuraft.universal.api.platform.Platform.getInstance();
        return switch (format) {
            case LITEMATIC -> platform.findMod(MOD_LITEMATICA).isPresent()
                    ? gameDir.resolve(SCHEMATICS_DIR)
                    : gameDir.resolve(EXPORT_DIR);
            case WORLDEDIT -> platform.findMod(MOD_WORLDEDIT).isPresent()
                    ? gameDir.resolve(SCHEMATICS_DIR)
                    : gameDir.resolve(EXPORT_DIR);
            case AXIOM -> platform.findMod(MOD_AXIOM).isPresent()
                    ? gameDir.resolve(AXIOM_BLUEPRINT_DIR)
                    : gameDir.resolve(EXPORT_DIR);
        };
    }

    // ------------------------------------------------------------------
    // 投影 / Litematica
    // ------------------------------------------------------------------

    private static CompoundTag writeLitematic(String name, int sizeX, int sizeY, int sizeZ, List<Block> blocks) {
        var palette = new ArrayList<StateKey>();
        // 调色板的 0 号必须是空气（Litematica 靠它表示空格子）
        palette.add(new StateKey("minecraft:air", Map.of()));
        var paletteIndex = new HashMap<StateKey, Integer>();
        paletteIndex.put(palette.get(0), 0);

        var indices = new int[Math.max(1, sizeX * sizeY * sizeZ)];
        for (var block : blocks) {
            if (block.x() < 0 || block.y() < 0 || block.z() < 0
                    || block.x() >= sizeX || block.y() >= sizeY || block.z() >= sizeZ) {
                continue;
            }
            var key = toKey(block.state());
            var index = paletteIndex.get(key);
            if (index == null) {
                index = palette.size();
                palette.add(key);
                paletteIndex.put(key, index);
            }
            indices[indexOf(block.x(), block.y(), block.z(), sizeX, sizeZ)] = index;
        }

        var paletteTag = new ListTag();
        for (var entry : palette) {
            var compound = new CompoundTag();
            compound.putString("Name", entry.id());
            if (!entry.properties().isEmpty()) {
                var properties = new CompoundTag();
                entry.properties().forEach(properties::putString);
                compound.put("Properties", properties);
            }
            paletteTag.add(compound);
        }

        var region = new CompoundTag();
        region.putInt("DataVersion", dataVersion());
        region.put("BlockStatePalette", paletteTag);
        region.putLongArray("BlockStates", packBits(indices, bitWidth(palette.size())));
        region.put("Position", vec3(0, 0, 0));
        region.put("Size", vec3(sizeX, sizeY, sizeZ));
        region.put("TileEntities", new ListTag());

        var regions = new CompoundTag();
        regions.put("region_0", region);

        var metadata = new CompoundTag();
        metadata.putString("Name", name);
        metadata.putString("Author", "McooBuildwithEase");
        metadata.putString("Description", "");
        metadata.putInt("RegionCount", 1);
        metadata.putInt("TotalVolume", sizeX * sizeY * sizeZ);
        metadata.putInt("TotalBlocks", blocks.size());
        metadata.putLong("TimeCreated", System.currentTimeMillis());
        metadata.putLong("TimeModified", System.currentTimeMillis());
        metadata.put("EnclosingSize", vec3(sizeX, sizeY, sizeZ));

        var root = new CompoundTag();
        root.putInt("Version", 6);
        root.putInt("MinecraftDataVersion", dataVersion());
        root.put("Metadata", metadata);
        root.put("Regions", regions);
        return root;
    }

    /** Litematica 的方块索引：x 变化最快。 */
    private static int indexOf(int x, int y, int z, int sizeX, int sizeZ) {
        return x + z * sizeX + y * sizeX * sizeZ;
    }

    /** 每一位占多少 bit（和 Litematica 的 getRequiredBitWidth 一致）。 */
    private static int bitWidth(int paletteSize) {
        return Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1));
    }

    /**
     * Litematica 的位打包：低位在前、允许一个条目跨两个 long。
     * <p>
     * 注意它和原版 ChunkSection 的 BitArray 不一样 ——
     * 原版每个 long 独立位宽（会浪费位），Litematica 是紧凑连续的。
     */
    private static long[] packBits(int[] values, int bits) {
        var maxEntryValue = (1L << bits) - 1L;
        var length = (values.length * bits + 63L) / 64L;
        var array = new long[(int) length];
        for (int i = 0; i < values.length; i++) {
            long startOffset = (long) i * bits;
            int startArrIndex = (int) (startOffset >> 6);
            int endArrIndex = (int) (((i + 1L) * bits - 1L) >> 6);
            int startBitOffset = (int) (startOffset & 0x3F);
            long value = values[i] & maxEntryValue;
            array[startArrIndex] = array[startArrIndex] & ~(maxEntryValue << startBitOffset) | value << startBitOffset;
            if (startArrIndex != endArrIndex) {
                int endOffset = 64 - startBitOffset;
                int j1 = bits - endOffset;
                array[endArrIndex] = array[endArrIndex] >>> j1 << j1 | value >> endOffset;
            }
        }
        return array;
    }

    // ------------------------------------------------------------------
    // WorldEdit / Sponge Schematic v3
    // ------------------------------------------------------------------

    private static CompoundTag writeWorldEdit(String name, int sizeX, int sizeY, int sizeZ, List<Block> blocks) {
        var palette = new ArrayList<StateKey>();
        var paletteIndex = new HashMap<StateKey, Integer>();
        var air = new StateKey("minecraft:air", Map.of());

        // 先把真正的方块按位置收进来，剩下的格子留空（空气）
        var real = new HashMap<Integer, StateKey>();
        for (var block : blocks) {
            if (block.x() < 0 || block.y() < 0 || block.z() < 0
                    || block.x() >= sizeX || block.y() >= sizeY || block.z() >= sizeZ) {
                continue;
            }
            real.put((block.y() * sizeZ + block.z()) * sizeX + block.x(), toKey(block.state()));
        }

        // WorldEdit 的数据顺序是 y → z → x（x 变化最快）
        var data = new java.io.ByteArrayOutputStream();
        for (int y = 0; y < sizeY; y++) {
            for (int z = 0; z < sizeZ; z++) {
                for (int x = 0; x < sizeX; x++) {
                    var key = real.getOrDefault((y * sizeZ + z) * sizeX + x, air);
                    var index = paletteIndex.get(key);
                    if (index == null) {
                        index = palette.size();
                        palette.add(key);
                        paletteIndex.put(key, index);
                    }
                    writeVarInt(data, index);
                }
            }
        }

        var paletteTag = new CompoundTag();
        for (int i = 0; i < palette.size(); i++) {
            paletteTag.putInt(palette.get(i).asString(), i);
        }

        var blocksTag = new CompoundTag();
        blocksTag.put("Palette", paletteTag);
        blocksTag.putByteArray("Data", data.toByteArray());
        blocksTag.put("BlockEntities", new ListTag());

        var metadata = new CompoundTag();
        metadata.putLong("Date", System.currentTimeMillis());
        metadata.putString("Name", name);
        metadata.putString("Author", "McooBuildwithEase");
        var worldEdit = new CompoundTag();
        worldEdit.putString("Version", "McooBuildwithEase");
        worldEdit.putString("EditingPlatform", "Minecraft");
        worldEdit.putIntArray("Origin", new int[]{0, 0, 0});
        metadata.put("WorldEdit", worldEdit);

        var schematic = new CompoundTag();
        schematic.putInt("Version", 3);
        schematic.putInt("DataVersion", dataVersion());
        schematic.put("Metadata", metadata);
        schematic.putShort("Width", (short) sizeX);
        schematic.putShort("Height", (short) sizeY);
        schematic.putShort("Length", (short) sizeZ);
        schematic.putIntArray("Offset", new int[]{0, 0, 0});
        schematic.put("Blocks", blocksTag);

        var root = new CompoundTag();
        root.put("Schematic", schematic);
        return root;
    }

    /** Minecraft 的 VarInt：每字节 7 位，高位为 1 表示还有后续。 */
    private static void writeVarInt(java.io.ByteArrayOutputStream out, int value) {
        while ((value & -128) != 0) {
            out.write((value & 127) | 128);
            value >>>= 7;
        }
        out.write(value);
    }

    // ------------------------------------------------------------------
    // 公共
    // ------------------------------------------------------------------

    /** 一个方块状态：方块 id + 属性。 */
    private record StateKey(String id, Map<String, String> properties) {

        /** WorldEdit 用的方块状态字符串，例如 {@code minecraft:oak_stairs[facing=east]}。 */
        String asString() {
            if (this.properties.isEmpty()) {
                return this.id;
            }
            var builder = new StringBuilder(this.id).append('[');
            var first = true;
            for (var entry : this.properties.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                first = false;
                builder.append(entry.getKey()).append('=').append(entry.getValue());
            }
            return builder.append(']').toString();
        }

    }

    private static StateKey toKey(BlockState state) {
        var vanilla = (net.minecraft.world.level.block.state.BlockState) state.reference();
        // 26.2 里 ResourceKey#location() 改名成了 identifier()
        var id = vanilla.getBlock().builtInRegistryHolder().key().identifier().toString();
        var properties = new LinkedHashMap<String, String>();
        vanilla.getValues().forEach(value -> properties.put(value.property().getName(), value.valueName()));
        return new StateKey(id, properties);
    }

    private static CompoundTag vec3(int x, int y, int z) {
        var compound = new CompoundTag();
        compound.putInt("x", x);
        compound.putInt("y", y);
        compound.putInt("z", z);
        return compound;
    }

    private static int dataVersion() {
        return SharedConstants.getCurrentVersion().dataVersion().version();
    }

    /** 文件名里不能有的字符。 */
    private static String sanitize(String name) {
        var cleaned = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (cleaned.isEmpty()) {
            cleaned = "structure";
        }
        return cleaned;
    }

}
