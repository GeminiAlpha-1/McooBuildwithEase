# McooBuildwithEase

面向 Minecraft 26.2 的批量建造模组（仅 Fabric）。

提供 15 种建造模式、替换模式、变换器、撤销/重做与剪贴板结构管理。
服务端安装即可，未安装本模组的玩家能正常进服与游戏。

本项目是 [Effortless Structure](https://github.com/huskuraft/effortless) 的 26.2 移植版，
并在其基础上追加了结构导出与三维斜线建造模式。

## 环境要求

| 项目 | 版本 |
|---|---|
| Minecraft | 26.2 |
| 加载器 | Fabric |
| Fabric Loader | ≥ 0.19.3 |
| Fabric API | ≥ 0.153.0 |
| Java | ≥ 25 |

## 安装

将 `McooBuildwithEase-<版本号>.jar` 放入服务端的 `mods/` 目录。

客户端按需安装：

- **服务端安装**：所有玩家均可正常进服；未安装本模组的玩家无法使用建造功能。
- **客户端安装**：可使用完整功能。客户端与服务端需使用同一版本的 jar。

建造操作由客户端计算结构并下发至服务端执行，因此服务端安装本模组是使用建造功能的前提。

## 版本号

格式为 `<年份>.<累计功能数>.<累计修复数>+<目标MC版本>`，例如 `26.3.5+26.2`：

| 字段 | 含义 |
|---|---|
| `26` | 年份代号，与目标 Minecraft 大版本对应 |
| `3` | 上线以来累计新增的用户可见功能数 |
| `5` | 上线以来累计修复的问题数 |
| `+26.2` | 该 jar 适用的 Minecraft 版本 |

中间两位累计递增，不随版本重置。Minecraft 跨年时年份 +1、功能数 +1（移植本身计为一个功能），
修复数保持不变。

## 功能

### 建造模式

按 `左 Alt` 打开建造模式菜单，选中模式后自动关闭；再次按下可切换菜单开关状态。
菜单以按钮方阵呈现，按钮下方的色条标示功能分组（蓝=基础、橙=立方/平面、紫=斜面、绿=圆柱、黄=屋顶）。

| 模式 | 说明 |
|---|---|
| Disable | 恢复原版放置行为 |
| Single | 延长放置距离 |
| Line | 沿三个轴向之一绘制直线 |
| Wall | 沿 X 或 Z 轴绘制墙面 |
| Floor | 沿 Y 轴绘制地板 |
| Cuboid | 立方体 |
| Diagonal Line | 任意角度的斜线 |
| 3D Diagonal Line | 三维空间斜线，三次点击依次确定起点、水平方向与终点高度 |
| Diagonal Wall | 任意角度的斜墙 |
| Slope Floor | 任意角度的斜地板 |
| Circle | 三个轴向的圆 |
| Cylinder | 三个轴向的圆柱 |
| Sphere | 球体 |
| Pyramid | 金字塔 |
| Cone | 圆锥 |

### 替换模式

控制放置方块时如何处理目标位置已有的方块：Disable、Blocks and Air、Blocks Only、Offhand Only。

### 变换器

将多个建造操作组合为复合结构：

- **Mirror** 镜像
- **Array** 沿指定方向复制 N 次
- **Radial** 绕中心环形复制
- **Item Randomizer** 从预设列表中随机选取方块

### 剪贴板

剪贴板保存当前结构快照，可复制、粘贴、旋转、镜像与移动。
粘贴会一并复制方块实体及其容器内容。

剪贴板界面提供结构预览：按真实方块模型渲染的三维视图，支持左键拖动旋转、右键拖动平移、滚轮缩放。
方块实体（钟、箱子、陶罐、旗帜等）也按游戏里的样子渲染，含水的方块会连水一起显示。

预览区右上角的「视角」按钮可绕 X / Y / Z 轴各转 90°（左键顺时针、右键逆时针），并能一键回到默认视角。
「导出图片」按钮把当前视角导出为**透明背景**的 PNG，分辨率可选 1K / 2K / 4K，
文件存放在游戏目录的 `mcoo_build_with_ease_exports/`。

### 结构导出

剪贴板界面的「下载」可将结构导出为工程文件：

- **投影格式 `.litematic`** —— Litematica 可直接读取
- **WorldEdit 格式 `.schem`** —— Sponge Schematic v3，WorldEdit / FAWE 可直接读取
- **Axiom 蓝图 `.schem`** —— Axiom 的 Schematic 导入功能可读取

导出使用游戏内文件管理器，玩家可自行选择目录、新建文件夹或保存到任意路径。
默认目录按已安装的模组自动选择：投影 / WorldEdit 使用实例目录下的 `schematics/`，Axiom 使用 `Axiom/blueprints/`，
未安装对应模组时使用模组自己的 `mcoo_build_with_ease_exports/`。

「下载到电脑的下载文件夹」按钮会把两种格式（`.litematic` 与 `.schem`）一起导出到系统下载文件夹。

### 快捷键

| 按键 | 作用 |
|---|---|
| `左 Alt` | 打开/关闭建造模式菜单 |
| `鼠标左键` | 连续破坏方块 |
| `鼠标右键` | 放置或交互方块 |
| `[` / `]` | 撤销 / 重做 |

全部按键均可在设置中修改。

## 兼容性

- 目标版本为 Minecraft 26.2。
- 渲染使用 vanilla 管线与顶点色，不含自定义着色器，OpenGL 与 Vulkan 两种图形后端表现一致。
- 与 WorldEdit、Axiom、Syncmatica 等服务端模组共存；跨世界搬运结构建议使用这些模组。

## 许可

本模组以 **GNU Lesser General Public License v3.0** 发布。

许可证全文见 [LICENSE](LICENSE)（LGPL v3），其引用的 GNU GPL v3 全文见 [COPYING](COPYING)。

本项目基于上游项目 [Effortless Structure](https://github.com/huskuraft/effortless) 移植，
并 vendor 了 [universal-api](https://github.com/huskuraft/universal-api) 的 `common-api` 接口层。
上游代码以源码形式保留在本仓库中，修改内容与归属说明见 [NOTICE](NOTICE)。

源代码：<https://github.com/GeminiAlpha-1/McooBuildwithEase>

本程序是自由软件：您可以依据 GNU Lesser General Public License v3.0 重新分发和/或修改它。
本程序按「原样」提供，不附带任何明示或暗示的担保。
