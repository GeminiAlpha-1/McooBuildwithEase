# 界面字体（ImGui 用）—— 生成流程

ImGui 的界面字体**必须自己带**：宿主库 `fabric-gui-imgui` 的默认字体是 ProggyClean（纯 ASCII），
而 26.2 的客户端 jar 里**没有任何 `.ttf/.otf`**（字体只有 bitmap / unihex provider，喂不进 ImGui）。
不带字体的直接后果是中文界面全是方块（2026-10-27 实测确认过）。

## 一条命令重建

```powershell
pip install fonttools
python tools/font/make_subset.py    # 下载变量字体 → 实例化 Medium → 子集
python tools/font/build_font.py     # 改名 McooUI SC → 落到 src/main/resources/.../fonts/
```

产物（**这两个要提交**）：

| 文件 | 说明 |
|---|---|
| `src/main/resources/assets/mcoo_build_with_ease/fonts/McooUI-SC-Medium.ttf` | 运行时用的子集字体，1.27MB / 4878 字形 |
| `src/main/resources/assets/mcoo_build_with_ease/fonts/OFL.txt` | OFL 1.1 要求随字体分发许可证全文 |

中间产物落在 `tools/font/.cache/`（**不提交**，已进 `.gitignore`）。

## 为什么是"子集 + 粗范围"

宿主后端从不设置 `ImGuiBackendFlags.RendererHasTextures`，所以走的是 ImGui 的
**legacy 静态图集**：`GlyphRanges` 是硬要求，图集 build 完即 locked、之后不能改字号。

因此：

- **字体文件**做子集（只要用得到的字形），体积从 10MB 降到 1.3MB；
- **运行时**给粗粒度范围（整个 CJK 基本区 `0x4E00..0x9FFF` 等），
  ImGui 只把字体里真有的字形烘进图集，缺的自动跳过
  ⇒ 不需要在运行时解析 lang 文件，也不会出现"字体里有、范围没给"的漏字。
- 结果：图集 **2048×2048**（≈16MB VRAM），首次构建几百毫秒。

## 字符集档位（实测体积）

| 字符集 | 字符数 | 字体大小 |
|---|---:|---:|
| 仅 lang 文件 | 1620 | 0.33 MB |
| **lang + GB2312 一级**（当前） | 4848 | **1.27 MB** |
| lang + GB2312 全集 | 7851 | 2.23 MB |

选中间档：UI 文案精确覆盖 + 3755 个最常用汉字，足够应付玩家名、结构名、方块名。
子集之外的生僻字会退化成方块——要补就改 `make_subset.py` 里的 `CHARSET_NAME` 重跑。

## 许可证

字体是 **Noto Sans SC**（SIL Open Font License 1.1）的子集，改名 `McooUI SC`。
- 来源：<https://fonts.google.com/noto/specimen/Noto+Sans+SC>（Google Fonts 仓库的变量 TTF）
- 版权：`(c) 2014-2021 Adobe, with Reserved Font Name 'Source'`
- OFL 允许再分发（含商用），要求随附许可证全文与版权声明 → `OFL.txt` 已随字体进 `resources`

⚠️ **不要**从 Axiom 的 jar 里抠字体：即使 Noto 本身是 OFL，从 ARR 制品里提取资产不干净，
而且版本与子集范围都不可控。
