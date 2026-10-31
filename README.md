# McooBuildwithEase

面向 Minecraft 26.2 的批量建造模组（Fabric）。

A bulk-building modification for Minecraft 26.2 (Fabric).

提供 15 种建造模式、4 种替换模式、4 种变换器、撤销/重做以及剪贴板结构管理。服务端安装后即可使用；未安装本模组的玩家仍可正常进入服务器并参与游戏。

The mod provides 15 build modes, 4 replacement modes, 4 transformers, undo/redo, and clipboard-based structure management. Installing it on the server is sufficient for use; players without the mod can still join the server and play normally.

本项目是 [Effortless Structure](https://github.com/huskuraft/effortless) 的 Minecraft 26.2 移植版本，并在其基础上增加了结构导出与三维斜线建造模式。

This project is a Minecraft 26.2 port of [Effortless Structure](https://github.com/huskuraft/effortless), with structure export and three-dimensional diagonal building added on top of the upstream feature set.

## 1. 环境要求 / Requirements

| 项目 / Item | 版本 / Version |
|---|---|
| Minecraft | 26.2 |
| 加载器 / Mod loader | Fabric |
| Fabric Loader | ≥ 0.19.3 |
| Fabric API | ≥ 0.153.0 |
| Java | ≥ 25 |

## 2. 安装 / Installation

将 `McooBuildwithEase-<版本号>.jar` 放入服务端的 `mods/` 目录。

Place `McooBuildwithEase-<version>.jar` into the server's `mods/` directory.

客户端安装为可选项：

Client installation is optional:

- **仅服务端安装**：所有玩家均可正常进入服务器；未安装本模组的玩家无法使用建造功能。<br>**Server-only installation**: all players can join the server normally; players without the mod cannot use building features.
- **客户端安装**：可使用完整功能；客户端与服务端必须使用同一版本的 jar。<br>**Client installation**: enables the full feature set; the client and server must use the same jar version.

建造操作由客户端计算结构并下发至服务端执行，因此服务端安装本模组是使用建造功能的前提条件。

Building operations are computed on the client and sent to the server for execution. Installing the mod on the server is therefore a prerequisite for using building features.

## 3. 下载 / Downloads

本项目维护两条相互独立的版本线，均通过 GitHub Releases 分发。

The project maintains two independent release lines, both distributed through GitHub Releases.

| 版本线 / Channel | 分支 / Branch | 标签 / Tag | 发行版类型 / Release type | 适用对象 / Intended for |
|---|---|---|---|---|
| 正式版 / Stable | `master` | `26.<功能>.<修复>+26.2` | 普通 Release / Regular release | 服务器与玩家日常使用 / Servers and regular players |
| 测试版 / Testing | `dev` | `snapshot.<功能>.<修复>+26.2` | **Pre-release** | 愿意测试新功能或参与联调的玩家 / Players willing to test new features or assist with integration testing |

- 正式版：从 [最新非预发布版本](https://github.com/GeminiAlpha-1/McooBuildwithEase/releases/latest) 获取。<br>Stable: download from the [latest non-prerelease release](https://github.com/GeminiAlpha-1/McooBuildwithEase/releases/latest).
- 测试版：在 [Releases](https://github.com/GeminiAlpha-1/McooBuildwithEase/releases) 页面选择标记为 **Pre-release** 且版本号以 `snapshot.` 开头的条目。<br>Testing: on the [Releases](https://github.com/GeminiAlpha-1/McooBuildwithEase/releases) page, select an entry marked **Pre-release** whose version starts with `snapshot.`.
- ⚠️ 客户端与服务端必须安装同一个 jar；版本不一致会导致连接失败或会话无效。<br>⚠️ The client and server must install the same jar; a version mismatch will cause connection failure or an invalid session.

## 4. 版本号 / Versioning

版本号格式为 `<年份>.<累计功能数>.<累计修复数>+<目标MC版本>`，例如 `26.3.5+26.2`。

The version format is `<year>.<cumulative features>.<cumulative fixes>+<target MC version>`, for example `26.3.5+26.2`.

| 字段 / Field | 含义 / Meaning |
|---|---|
| `26` | 年份代号，与目标 Minecraft 大版本对应。<br>Year identifier corresponding to the target Minecraft major version. |
| `3` | 上线以来累计新增的用户可见功能数。<br>Cumulative number of user-visible features added since release. |
| `5` | 上线以来累计修复的问题数。<br>Cumulative number of issues fixed since release. |
| `+26.2` | 该 jar 适用的 Minecraft 版本。<br>Minecraft version supported by the jar. |

中间两位为累计计数，不随版本发布而重置。目标 Minecraft 跨年时，年份位加一、功能数加一（移植本身计为一个功能），修复数保持不变。

The two middle components are cumulative and are never reset between releases. When the target Minecraft version crosses into a new year, the year component increases by one and the feature count increases by one (the port itself counts as one feature), while the fix count remains unchanged.

### 4.1 测试版版本线 / Testing Line

测试版以 `snapshot` 前缀取代年份位，从 `0.0.1` 起独立计数，例如 `snapshot.0.0.1+26.2`。

The testing line replaces the year component with the `snapshot` prefix and counts independently from `0.0.1`, for example `snapshot.0.0.1+26.2`.

| 字段 / Field | 含义 / Meaning |
|---|---|
| `snapshot` | 测试版标记，取代年份位。<br>Testing-line marker replacing the year component. |
| `0` | 该测试线上累计新增的用户可见功能数。<br>Cumulative user-visible features added on the testing line. |
| `1` | 该测试线上累计修复的问题数。<br>Cumulative issues fixed on the testing line. |
| `+26.2` | 该 jar 适用的 Minecraft 版本。<br>Minecraft version supported by the jar. |

累计规则与正式版完全一致（新增功能加一、修复问题加一，均不重置），但两条版本线互不影响：正式版继续使用 `26.x.x+26.2`，测试版使用 `snapshot.x.y.z+26.2`。

The counting rules are identical to the stable line (one feature adds one, one fix adds one, and neither is reset), but the two lines are independent: the stable line continues with `26.x.x+26.2`, while the testing line uses `snapshot.x.y.z+26.2`.

## 5. 功能 / Features

### 5.1 建造模式 / Build Modes

按 `左 Alt` 打开建造模式菜单，选中模式后菜单自动关闭；再次按下可切换菜单的显示状态。菜单以按钮方阵呈现，按钮下方的色条标示功能分组（蓝：基础；橙：立方/平面；紫：斜面；绿：圆柱；黄：屋顶）。

Press `Left Alt` to open the build mode menu; the menu closes automatically after a mode is selected. Pressing the key again toggles the menu. The menu is presented as a square grid of buttons, and the color bar below each button indicates its functional group (blue: basic; orange: cuboid/plane; purple: diagonal; green: cylindrical; yellow: roof).

| 模式 / Mode | 说明 / Description |
|---|---|
| Disable | 恢复原版放置行为。<br>Restores vanilla placement behavior. |
| Single | 延长放置距离。<br>Extends placement range. |
| Line | 沿三个轴向之一绘制直线。<br>Draws a straight line along one of the three axes. |
| Wall | 沿 X 轴或 Z 轴绘制墙面。<br>Draws a wall along the X or Z axis. |
| Floor | 沿 Y 轴绘制地板。<br>Draws a floor along the Y axis. |
| Cuboid | 绘制长方体。<br>Draws a cuboid. |
| Diagonal Line | 绘制任意角度的斜线。<br>Draws a diagonal line at an arbitrary angle. |
| 3D Diagonal Line | 通过分步点击确定起点、水平方向与终点高度，绘制三维空间斜线。<br>Draws a line in three-dimensional space; the start point, horizontal direction, and end height are determined through step-by-step clicks. |
| Diagonal Wall | 绘制任意角度的斜墙。<br>Draws a diagonal wall at an arbitrary angle. |
| Slope Floor | 绘制任意角度的斜地板。<br>Draws a sloped floor at an arbitrary angle. |
| Circle | 绘制沿三个轴向之一的圆。<br>Draws a circle along one of the three axes. |
| Cylinder | 绘制沿三个轴向之一的圆柱。<br>Draws a cylinder along one of the three axes. |
| Sphere | 绘制球体。<br>Draws a sphere. |
| Pyramid | 绘制金字塔。<br>Draws a pyramid. |
| Cone | 绘制圆锥。<br>Draws a cone. |

### 5.2 替换模式 / Replacement Modes

替换模式控制放置方块时如何处理目标位置已有的方块，可选值包括 Disable、Blocks and Air、Blocks Only 与 Offhand Only。

Replacement modes control how existing blocks at the target position are handled during placement. Available options are Disable, Blocks and Air, Blocks Only, and Offhand Only.

### 5.3 变换器 / Transformers

变换器可将多个建造操作组合为复合结构：

Transformers combine multiple building operations into a composite structure:

- **Mirror（镜像）**：生成镜像副本。<br>**Mirror**: generates a mirrored copy.
- **Array（阵列）**：沿指定方向重复复制指定次数。<br>**Array**: repeats the structure a specified number of times along a given direction.
- **Radial（径向）**：绕中心进行环形复制。<br>**Radial**: replicates the structure around a center point.
- **Item Randomizer（物品随机化器）**：从预设列表中随机选取方块。<br>**Item Randomizer**: selects blocks randomly from a predefined list.

### 5.4 剪贴板 / Clipboard

剪贴板保存当前结构的快照，支持复制、粘贴、旋转、镜像与移动。粘贴时会一并复制方块实体及其容器内容。

The clipboard stores a snapshot of the current structure and supports copying, pasting, rotating, mirroring, and moving. Pasting also copies block entities and their container contents.

剪贴板界面提供结构预览：基于真实方块模型渲染的三维视图，支持左键拖动旋转、右键拖动平移与滚轮缩放。方块实体（钟、箱子、陶罐、旗帜等）按其游戏内外观渲染；含水方块会连同水一起显示。

The clipboard screen provides a structure preview: a three-dimensional view rendered from actual block models, supporting left-drag rotation, right-drag panning, and scroll-wheel zoom. Block entities (bells, chests, decorated pots, banners, etc.) are rendered as they appear in game, and waterlogged blocks are displayed together with their water.

「视角」按钮可分别绕 X / Y / Z 轴旋转 90°（左键顺时针、右键逆时针），并可一键恢复默认视角。「导出图片」按钮将当前视角导出为透明背景的 PNG，分辨率可选 1K / 2K / 4K，文件保存在游戏目录的 `mcoo_build_with_ease_exports/` 下。

The "View" buttons rotate the preview by 90° around the X, Y, or Z axis (left-click for clockwise, right-click for counterclockwise), and a single action restores the default view. The "Export Image" button exports the current view as a transparent-background PNG at a resolution of 1K, 2K, or 4K; files are saved under `mcoo_build_with_ease_exports/` in the game directory.

### 5.5 结构导出 / Structure Export

剪贴板界面的「下载」功能可将结构导出为工程文件：

The "Download" function on the clipboard screen exports the structure as a project file:

- **投影格式 `.litematic`**：Litematica 可直接读取。<br>**Litematica projection `.litematic`**: directly readable by Litematica.
- **WorldEdit 格式 `.schem`**：Sponge Schematic v3，WorldEdit / FAWE 可直接读取。<br>**WorldEdit format `.schem`**: Sponge Schematic v3, directly readable by WorldEdit / FAWE.
- **Axiom 蓝图 `.schem`**：Axiom 的 Schematic 导入功能可读取。<br>**Axiom blueprint `.schem`**: readable through Axiom's schematic import.

导出通过游戏内文件管理器完成，玩家可自行选择目录、新建文件夹或保存至任意路径。默认目录依据已安装的模组自动选择：投影与 WorldEdit 使用实例目录下的 `schematics/`，Axiom 使用 `Axiom/blueprints/`，未安装对应模组时使用本模组的 `mcoo_build_with_ease_exports/`。

Exporting is performed through the in-game file manager, allowing players to choose a directory, create a folder, or save to an arbitrary path. The default directory is selected automatically based on installed mods: `schematics/` in the instance directory for Litematica and WorldEdit, `Axiom/blueprints/` for Axiom, and the mod's own `mcoo_build_with_ease_exports/` when the corresponding mod is not installed.

导出浏览器提供「跳转到系统下载目录」按钮；导出按所选格式逐个执行。

The export browser provides a "Go to system Downloads folder" button; exports are performed one format at a time.

### 5.6 快捷键 / Key Bindings

| 按键 / Key | 作用 / Action |
|---|---|
| `左 Alt` / `Left Alt` | 打开或关闭建造模式菜单。<br>Opens or closes the build mode menu. |
| `鼠标左键` / `Left Mouse Button` | 连续破坏方块。<br>Continuously breaks blocks. |
| `鼠标右键` / `Right Mouse Button` | 放置方块或与方块交互。<br>Places blocks or interacts with blocks. |
| `[` / `]` | 撤销 / 重做。<br>Undo / redo. |

全部按键均可在设置中修改。

All key bindings can be changed in the settings.

## 6. 兼容性 / Compatibility

- 目标版本为 Minecraft 26.2。<br>Target version: Minecraft 26.2.
- 渲染仅使用 vanilla 渲染层与顶点色，不包含自定义着色器；OpenGL 与 Vulkan 两种图形后端表现一致。<br>Rendering uses only vanilla render layers and vertex colors, with no custom shaders; behavior is identical on the OpenGL and Vulkan backends.
- 可与 Iris / Sodium 等接管渲染管线的模组同时安装且不产生冲突。但光影会接管画面，启用光影时剪贴板预览的「导出透明背景 PNG」将不再保持透明背景。<br>Can be installed alongside pipeline-replacing mods such as Iris / Sodium without conflict. However, shader packs take over rendering; when shaders are enabled, the clipboard's transparent-background PNG export will no longer have a transparent background.
- 可与 WorldEdit、Axiom、Syncmatica 等服务端模组共存；跨世界搬运结构建议使用上述模组。<br>Coexists with server-side mods such as WorldEdit, Axiom, and Syncmatica; for moving structures across worlds, those mods are recommended.

## 7. 许可 / License

本模组以 **GNU Lesser General Public License v3.0** 发布。

This mod is released under the **GNU Lesser General Public License v3.0**.

许可证全文见 [LICENSE](LICENSE)（LGPL v3），其引用的 GNU GPL v3 全文见 [COPYING](COPYING)。

The full license text is available in [LICENSE](LICENSE) (LGPL v3); the referenced GNU GPL v3 text is available in [COPYING](COPYING).

本项目基于上游项目 [Effortless Structure](https://github.com/huskuraft/effortless) 移植，并 vendor 了 [universal-api](https://github.com/huskuraft/universal-api) 的 `common-api` 接口层。上游代码以源码形式保留在本仓库中，修改内容与归属说明见 [NOTICE](NOTICE)。

This project is a port of the upstream project [Effortless Structure](https://github.com/huskuraft/effortless) and vendors the `common-api` interface layer from [universal-api](https://github.com/huskuraft/universal-api). Upstream source code is retained in this repository; details of modifications and attribution are provided in [NOTICE](NOTICE).

源代码 / Source code: <https://github.com/GeminiAlpha-1/McooBuildwithEase>

本程序为自由软件：您可以依据 GNU Lesser General Public License v3.0 的条款重新分发和/或修改它。

This program is free software: you may redistribute and/or modify it under the terms of the GNU Lesser General Public License v3.0.

本程序按「原样」提供，不附带任何明示或暗示的担保。

This program is distributed "as is", without any express or implied warranty.
