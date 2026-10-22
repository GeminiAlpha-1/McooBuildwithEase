package dev.huskuraft.universal.api.renderer;

import java.util.List;

/**
 * 一个已经做好剔除的三维预览网格。
 * <p>
 * 剔除是<b>相机无关</b>的：哪些面被相邻方块挡住，只取决于结构本身，
 * 不取决于从哪个角度看。所以剔除结果可以缓存起来反复用，
 * 每帧只需要按新的相机参数重新投影。
 *
 * @param faces  可见的面（已剔除内部面与背对相机的面）
 * @param center 结构中心（世界坐标，用于旋转）
 * @param bounds 旋转前的包围盒（世界坐标）
 */
public record PreviewMesh(
        List<PreviewFace> faces,
        float centerX,
        float centerY,
        float centerZ,
        float minX,
        float minY,
        float minZ,
        float maxX,
        float maxY,
        float maxZ
) {

    /** 空网格（没有方块，或平台不支持预览时）。 */
    public static final PreviewMesh EMPTY = new PreviewMesh(
            List.of(), 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

    public boolean isEmpty() {
        return faces.isEmpty();
    }

    /**
     * 预览网格里的一个面。
     *
     * @param positions 4 个顶点的局部坐标（相对方块原点的 0~1）
     * @param uvs       4 个顶点的贴图内坐标（0~1）
     * @param u0,v0,u1,v1 这个面用的贴图在方块图集里的范围
     * @param tint      顶点色（含逐面明暗）
     * @param nx,ny,nz 面的法线（未旋转，世界轴向）
     */
    public record PreviewFace(
            float[] positions,
            float[] uvs,
            float u0,
            float v0,
            float u1,
            float v1,
            int tint,
            float nx,
            float ny,
            float nz
    ) {
    }
}
