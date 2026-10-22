package dev.huskuraft.universal.api.export;

import java.nio.file.Path;
import java.util.List;
import dev.huskuraft.universal.api.core.BlockState;

/**
 * 把结构导出成建模工具能直接读的工程文件。
 * <p>
 * 【移植新增】这是 26.2 移植时新加的平台接口（上游没有），
 * 目的是让「业务代码零 MC 依赖」这条规矩继续成立 ——
 * 具体的文件格式（NBT / 调色板 / 位打包）只有平台层才知道。
 * <p>
 * 实现通过 {@code @AutoService} 注册，用
 * {@code PlatformLoader.getSingleton(StructureExporter.class)} 拿。
 */
public interface StructureExporter {

    /**
     * 导出格式。
     */
    enum Format {

        /** 投影（Litematica）的 {@code .litematic}。 */
        LITEMATIC("litematic", ".litematic"),
        /** WorldEdit / Sponge Schematic v3 的 {@code .schem}。 */
        WORLDEDIT("worldedit", ".schem"),
        /**
         * Axiom 蓝图。
         * <p>
         * Axiom 自己的 {@code .bp} 是私有二进制格式（魔数 182827830 + 头部 NBT + 缩略图 + 压缩的区块 NBT），
         * 而且里面的调色板容器没有公开的构造入口，逆向实现风险太高。
         * 所以这里写的是 <b>Axiom 明确支持的 Sponge {@code .schem}</b>（Axiom 菜单里「导入 Schematic」就吃这个），
         * 并且直接放到 Axiom 的蓝图目录 {@code Axiom/blueprints/} 里，省得再挪一次。
         */
        AXIOM("axiom", ".schem");

        private final String id;
        private final String extension;

        Format(String id, String extension) {
            this.id = id;
            this.extension = extension;
        }

        public String id() {
            return this.id;
        }

        /** 文件扩展名（含点）。 */
        public String extension() {
            return this.extension;
        }

    }

    /**
     * 结构里的一个方块。
     *
     * @param x     相对结构左下角的 x
     * @param y     相对结构左下角的 y
     * @param z     相对结构左下角的 z
     * @param state 方块状态
     */
    record Block(int x, int y, int z, BlockState state) {
    }

    /**
     * 导出结构。
     *
     * @param directory  保存目录（由调用方决定，比如用户在文件管理器里挑的位置）
     * @param fileName   文件名（不含扩展名）
     * @param sizeX      结构在 x 方向的大小
     * @param sizeY      结构在 y 方向的大小
     * @param sizeZ      结构在 z 方向的大小
     * @param blocks     结构里的方块（坐标相对结构左下角）
     * @param format     目标格式
     * @return 写出的文件路径
     */
    Path export(Path directory, String fileName, int sizeX, int sizeY, int sizeZ, List<Block> blocks, Format format);

    /**
     * 某种格式的「默认保存目录」（在游戏目录下）。
     * <p>
     * 用来给文件管理器当起始位置 —— 玩家想直接存到对应 mod 的文件夹就存这里，
     * 想存到别处分享给别人就自己改。
     */
    Path defaultDirectory(Format format);

}
