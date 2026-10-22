# McooBuildwithEase

批量建造模组：13 种建造模式、4 种替换模式、4 种变换器（含物品随机化器）、撤销/重做、剪贴板。

基于 [Effortless Structure](https://github.com/huskuraft/effortless)（LGPLv3）的 26.2 移植版。

## 环境

| 项 | 版本 |
|---|---|
| Minecraft | 26.2 |
| 加载器 | Fabric（**仅 Fabric**） |
| Fabric Loader | ≥ 0.19.3 |
| Fabric API | ≥ 0.153.0 |
| Java | ≥ 25 |

## 安装

把 `McooBuildwithEase-26.1.1+26.2.jar` 放进服务端的 `mods/` 目录。

**客户端可以不装** —— 服务端装有本 mod 时，未安装 mod 的玩家能正常进服、正常游戏，
只是用不了批量建造功能（建造需要客户端计算结构再下发，服务端只负责执行）。

装了 mod 的客户端才能使用建造功能。

### 版本号

`<年份>.<功能数>.<修复数>+<目标MC版本>`，例如 `26.1.1+26.2`：

- `26` 年份代（跟目标 MC 走）
- `1` 本代 MC 上的新功能累计数
- `1` 自上次新功能以来的修复数
- `+26.2` 这个 jar 是给哪个 MC 用的

客户端和服务端要用**同一个 jar**（版本号不一致会连不上）。

## 功能

### 建造模式

按一下 `左 Alt` 打开建造模式菜单切换模式（再按一下关闭；选中模式后自动关闭）。
菜单中间是**方块按钮方阵**（一行 5 个），每个按钮底下有一条颜色条标出功能分组
（蓝=基础、橙=立方/平面、紫=斜面、绿=圆柱、黄=屋顶）。
左侧按钮：撤销/重做、替换、设置、图案、剪贴板。

- **Disable** 按原版方式放置
- **Single** 加长放置距离
- **Line** 沿三个轴之一画线
- **Wall** 沿 X 或 Z 轴画墙
- **Floor** 沿 Y 轴画地板
- **Diagonal Line** 任意角度的斜线
- **Diagonal Wall** 任意角度的斜墙
- **Slope Floor** 任意角度的斜地板
- **Cube** 立方体
- **Circle** 三个轴向的圆
- **Cylinder** 三个轴向的圆柱
- **Sphere** 球体
- **Pyramid** 金字塔
- **Cone** 圆锥

### 替换模式

控制放置新方块时如何替换已有方块：Disable / Blocks and Air / Blocks Only / Offhand Only。

### 变换器

把多个建造组合成复杂形状：

- **Mirror** 镜像
- **Array** 沿指定方向复制 N 次
- **Radial** 绕中心环形复制
- **Item Randomizer** 从预设列表里随机取方块

### 快捷键

| 键 | 作用 |
|---|---|
| `左 Alt` | 打开/关闭建造模式菜单 |
| `鼠标左键` | 连续破坏方块 |
| `鼠标右键` | 放置/交互方块 |
| `[` / `]` | 撤销 / 重做 |

所有按键都能在设置里改。

## 兼容性

- 面向 **Minecraft 26.2**。
- 渲染走 vanilla 自带管线 + 顶点色，**OpenGL 与 Vulkan 两个后端都支持**
  （26.2 内置 Vulkan 后端；本 mod 不自带着色器，因此不受后端差异影响）。
- 服务端已有 **WorldEdit / Axiom / Syncmatica** 时，跨世界搬运结构请用它们。

## 许可

LGPLv3。包含上游 [Effortless Structure](https://github.com/huskuraft/effortless) 的
`common-api`（原样 vendor，保留其许可声明）与业务逻辑代码。
