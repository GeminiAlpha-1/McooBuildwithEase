# McooBuildwithEase — 工程约束

> 每次开工前必读。这里写的是**规则**，不是建议。

## 1. 这是什么

`Effortless Structure`（上游 `github.com/huskuraft/effortless`，3.4.0，最高 MC 1.21.3）移植到
**Minecraft 26.2 / Fabric only**，改名 **McooBuildwithEase**。

上游与本工程的关系：上游是 LGPLv3；本工程 vendor 了他的 `common-api`（纯接口层，270 文件），
并重写了 MC 适配层。**vendor 的代码不要改接口**——它是业务层与 MC 之间的契约。

## 2. 分层铁律（最重要的一条）

```
com.mcoo.buildwithease/**            业务层：零 MC 依赖
dev.huskuraft.universal.api/**       common-api（vendor，接口契约）
dev.huskuraft.universal.vanilla/**   MC 适配层：所有 net.minecraft 调用都在这里
dev.huskuraft.universal.fabric/**    Fabric 适配层：事件 / mixin / networking / platform
```

- **业务层不得出现任何 `net.minecraft` / `com.mojang.blaze3d` 的 import。**
  跟版时如果业务层报编译错，说明分层被破坏了 —— 要修的是分层，不是业务。
- common-api 是 vendor 的上游代码，**接口名一律保持上游原样**（例如 `ResourceLocation`，
  不要改名成 `Identifier`，会和 MC 自己的 `net.minecraft.resources.Identifier` 撞名）。

## 3. 版本坐标（唯一出处：`gradle.properties`）

| 项 | 值 | 备注 |
|---|---|---|
| minecraft_version | 26.2 | 跟随服务器 |
| loader_version | 0.19.3 | **必须与服务器一致** |
| fabric_api_version | 0.153.0+26.2 | **必须与服务器一致** |
| loom_version | 1.18-SNAPSHOT | |
| mod_version | 26.0.0+26.2 | 规则见下面「版本号规则」 |

⚠️ 编译版本必须 ≤ 服务器版本：用更新的 Fabric API 编译会引入服务器没有的类/方法 → 运行时 `NoSuchMethodError`。

### 3.1 版本号规则

```
<年份>.<功能数>.<修复数>+<目标MC版本>
        26      0       0      +26.2
```

| 段 | 含义 | 什么时候变 |
|---|---|---|
| `26` | 年份 = 目标 MC 的年份代 | 目标 MC 跨年（26.x → 27.x）才变 |
| `0` | 本代 MC 上的**新功能累计数** | 每加一个**用户可见的新功能** +1（新建造模式 / 新界面 / 新设置项…） |
| `0` | 自上次功能 bump 以来的**修复数** | 每修一个 BUG +1；功能 bump 时归零 |
| `+26.2` | 目标 MC 版本 | 服务器升 MC 小版本就改这里（功能/修复计数**不清零**） |

规则细节：

- **功能 vs 修复的界线**：只有**用户能感知的新东西**才算功能（多一个建造模式、多一个界面、多一个选项）；
  修 BUG、改文案、适配新 MC、修渲染观感都算**修复**。
- **功能 bump 时修复数归零**：`26.1.3` → 加功能 → `26.2.0`。
- **MC 小版本升级**（26.2 → 26.3）：只改 `+26.2` → `+26.3`，功能/修复计数**继续累计**（不清零），
  这样「同一个版本号对应两个 jar」的歧义就不会出现。
- **MC 跨年**（26.x → 27.x）：年份位 +1（27），功能数、修复数**都归零**。
- **预发布版**用 `-beta.1` / `-rc.2` 后缀：`26.1.0-beta.1`。
- **必须 bump 版本号**的情况：改了网络协议（客户端/服务器版本不一致会连不上）、
  改了配置格式、改了资源路径 —— 这些都要重新发版，不能只靠「文件改了」。
- 每发一版同步更新 `CHANGELOG.md`。

> 为什么不用「年份.月份.功能数」：修 BUG 时版本号不变 → 同一个版本号会对应两个不同的 jar，
> 用户和服务器都判断不出装的是新是旧。功能/修复分两位是 SemVer 的成熟做法，行业里到处都在用。
>
> 为什么 MC 版本放 `+` 后面：Fabric API 自己就是 `0.153.0+26.2`、Sodium 是 `0.6.5+mc1.21.4`，
> SemVer 的 build metadata 不参与版本比较，正好用来挂「这个 jar 是给哪个 MC 用的」。

## 4. 构建

```powershell
$env:GRADLE_USER_HOME="D:\WorkSpace\McooServerFeatures\.gradle-home"   # 共享缓存，已含 MC 26.2 / Loom / 依赖
.\gradlew.bat build --console=plain
```

- Gradle 9.7.1（wrapper 指向腾讯云镜像）、Java 25、Loom 1.18.4。
- 26.2 起 **Minecraft 不再混淆**，所以 `build.gradle` 里**没有 `mappings` 那一行** ——
  直接对官方未混淆的 jar 编译，API 名称就是 Mojang 官方名。
- **access widener 在 26.2 改名叫 class tweaker**，格式：
  ```
  classTweaker	v1	official          ← Tab 分隔；命名空间必须是 official
  accessible	method	net/minecraft/world/item/BlockItem	getPlacementState	(Lnet/...;)Lnet/...;
  ```
  并且**必须在 build.gradle 里显式声明**，否则编译期不生效（只在 fabric.mod.json 写字段是没用的）：
  ```groovy
  loom { accessWidenerPath = file("src/main/resources/mcoo_build_with_ease.classtweaker") }
  ```
- 自测服务器：`testserver/`（不入库），脚本 `tools/self-test.ps1` 会构建 → 装 jar → 起服 → 抓日志。
- 查 26.2 API：类索引 + javap，不要凭 1.21 记忆猜。

### 4.1 自测脚本（`tools/`，都在仓库里）

| 脚本 | 作用 |
|---|---|
| `self-test.ps1` | 构建 → 装 jar → 起服 → 抓日志 |
| `e2e-test.ps1` | 起服 + 客户端 quickPlay 连服 → 检查双方日志 |
| `gui-verify.ps1` | 起服 + 客户端自动打开指定界面（`-PmcDebugScreen=settings`）→ 截图 |
| `radial-shot.ps1` | 抓径向菜单截图（会自动把鼠标悬停到某个槽位上） |
| `behavior-test.ps1` | **自动破坏回归测试**：俯视地面 → 按左 Alt 开轮盘 → 点第二个槽位切到 Single → 按住左键 → 松开，判定「按住能破坏、松开不再自动破坏」 |
| `screenshot.ps1` | 用 PrintWindow 抓 Minecraft 窗口 |

路径约定（可用环境变量覆盖）：

| 变量 | 默认 | 用途 |
|---|---|---|
| `MCOO_TEST_SERVER` | 仓库同级的 `testserver/` | 测试服目录 |
| `MCOO_GRADLE_HOME` | `$env:GRADLE_USER_HOME` | Gradle 缓存（避免重复下载 MC / Fabric API） |
| `MCOO_SHOTS` | 仓库同级的 `_shots/` | 截图输出 |

> 脚本是 UTF-8 **带 BOM** 的 `.ps1`（PowerShell 5.1 按 ANSI 读无 BOM 的文件，
> 中文注释会把 `param()` 解析坏，表现为「参数变成 null」）。改脚本后要保持 BOM。

## 5. mixin 纪律

**mixin 是跟版成本的主要来源**（vanilla 内部结构每版都动）。当前只保留一个：

| mixin | 为什么必须留 |
|---|---|
| `dev.huskuraft/universal/fabric/mixin/MinecraftMixin` | 客户端攻击 / 使用 / 选取方块的拦截，Fabric 没有官方 API |

其余一律用官方 Fabric API 替代（已删：`ShaderManagerMixin` / `PlayerListMixin` / `GuiMixin` / `KeyboardMixin`）。

### 5.1 注入点必须还原上游语义，不能改成方法头

**这条踩过坑，代价是世界被铲平。**

上游把事件挂在「真的在攻击 / 真的在用物品」的具体调用点上，事件因此只在**确实存在目标**时触发。
改到 `@At("HEAD")` 之后，事件会在「按住鼠标的每一 tick」都触发
（26.2 的 `continueAttack` 在按住时每 tick 都会被调用），
而业务层 `Context.Interactions#put` 若纯追加不去重，交互列表就会无限增长 →
客户端每 tick 把越来越大的交互集合发给服务端 → 服务端每 tick 破坏 N 个方块（N 还在涨）。

当前四个锚点（26.2 字节码里都还在，已逐一核对）：

| 方法 | 锚点 |
|---|---|
| `startAttack` | `INVOKE HitResult#getType()` |
| `continueAttack` | `INVOKE BlockState#isAir()` |
| `startUseItem` | `INVOKE LocalPlayer#getItemInHand(InteractionHand)` |
| `pickBlockOrEntity` | `INVOKE LocalPlayer#getAbilities()`（26.2 从 `pickBlock` 改名） |

**加新 mixin 前先问：这个事件是不是会在每 tick 触发？**

### 5.2 交互列表按位置去重

`Context.Interactions#put` 现在是「同位置替换、不同位置追加」。
交互列表的语义是「一组目标位置」，重复指向同一个方块不应该让数量增长。

## 6. GUI 纪律（26.2）

26.2 的 GUI 是 2D 的（`Matrix3x2fStack`），绘制走 `GuiGraphicsExtractor`。
这一节的每一条都是 26.2 与 1.21.3 的**行为差异**，踩错一个就是「整个界面变形 / 图标全没了 / 文字隐形」。

### 6.1 坐标与矩阵：两条路径各应用一次

common-api 的 GUI 绘制有两条路径，矩阵只能被应用一次：

| 路径 | 入口 | 矩阵怎么用 |
|---|---|---|
| 顶点路径 | `renderRect` / `renderQuad` / `renderTexture` → `MinecraftBufferSource` | 矩阵**烘焙进顶点**，所以 extractor 的 `pose()` 必须是单位阵 |
| 直接路径 | `graphics.text` / `item` / `setTooltipForNextFrame` | extractor 自己应用它的 `pose()`，所以坐标要**手工**按我们的矩阵变换一次 |

因此 `MinecraftRenderer` 里维护的是**自建的** `matrix2d`（不是 `graphics.pose()`）：
`MinecraftMatrixStack` 烘焙进顶点，`transformGui()` 手工变换文字/物品/tooltip 的坐标。
把自建矩阵塞进 extractor 的 pose 会让顶点路径被应用两次 → 整个界面变大。

### 6.2 `blitSprite` 的参数是 (x, y, 宽, 高)

26.2：`blitSprite(pipeline, spriteId, x, y, width, height, alpha)`
（对照原版 `AbstractButton#extractDefaultSprite` 的调用）。

传成 `(x1, y1, x2, y2)` 会让宽度变成 `x + width`、高度变成 `y + height` → 面板被撑爆。

### 6.3 `blit` 的四个浮点是 (uMin, uMax, vMin, vMax)

26.2：`blit(texture, x0, y0, x1, y1, uMin, uMax, vMin, vMax)` —— **u 在前、v 在后**
（对应 `BlitRenderState` 的 `u0/u1/v0/v1` 字段）。

传成 `(uMin, vMin, uMax, vMax)` 会把 u 区间压成 0 宽、v 区间压成 0 高
→ 整张贴图只采到左上角一个像素 → **所有图标都画不出来**（纯色面板底因为刚好是那个颜色，看着「正常」）。

### 6.4 文字颜色：必须传 ARGB，且 alpha = 0xFF

26.2 的 `TextColor` 内部把值截成 24 位（纯 RGB），字形的 **alpha 取自「传给 Font 的那个颜色」**
（`Font$PreparedTextBuilder#getTextColor`：alpha 取自传入色，rgb 取自 `style.getColor()`）。

1.21.3 的字体 alpha 是独立字段，调用方习惯直接传 RGB（`0xFFFFFF` / `0x404040`）；
照搬到 26.2 会因为 alpha=0 让整段文字隐形（面板标题、轮盘标签都是这样没的）。
→ 渲染层统一给文字颜色补 `| 0xFF000000`。

### 6.5 GUI 没有多边形填充：任意四边形要自己提交 GUI 元素

26.2 的 `GuiGraphicsExtractor` 只剩轴对齐矩形 + 贴图 blit，**没有多边形填充**；
1.21.3 靠 `MultiBufferSource` 直接画四边形（轮盘菜单的扇形就靠它）。

**仿射近似是错的**：`fill` 会把 `pose()` 应用到矩形四角，于是「单位矩形 + 2D 仿射矩阵」= **平行四边形**。
轮盘的扇形是**梯形**（内外两条弦不等长），仿射变换做不出来，只能再按双线性插值切小块近似 ——
但每块的对角点仍有误差（4×4 切分时约 0.9 逻辑像素），
**相邻小块的共享边因此错开** → 扇形上出现一道道细划痕、彩色分段上出现黑点。

**正确做法**（`MinecraftBufferSource#drawQuad` → `QuadRenderState`）：
自己实现 `GuiElementRenderState`，把四边形按**三角形带**发进 GUI 渲染状态：

- GUI 管线用的是 **triangle strip** 拓扑（原版矩形填充就是发 4 个顶点凑成 2 个三角形）
  → 四边形发 **4 个**顶点，别发 6 个（发 6 个会被解释成 4 个三角形 → 图形炸开）。
- 绕序要和原版矩形填充一致（屏幕坐标 y 向下，有向面积为负），否则被背面剔除、什么都不画。
- `GuiRenderState` 在 `GuiGraphicsExtractor` 里是 **private 字段**、原版没有公开 getter →
  用反射取（`GuiRenderStateAccess`），取不到就退回仿射近似（不能崩）。

⚠️ `Matrix3x2fStack`（JOML 1.10.8）的 `pushMatrix()` 只是**保存当前矩阵**，
`translate` / `scale` / `mul` 都是继承自 `Matrix3x2f` 的原地修改；
`mul(m)` 是**右乘**（`this = this * m`，即「先 m 后 this」）。

### 6.6 图集精灵

- 原版 GUI 精灵用 `blitSprite(pipeline, spriteId, ...)` 画，九宫格由精灵元数据处理，**不要自己按 UV 拉伸**。
- 查精灵用 `AtlasManager` + `SpriteId`；**查不到会拿到「缺失精灵」→ 画出来是紫色棋盘格 + 条纹**。
  `SpriteId` 的第一个分量在不同版本分别表示过图集纹理位置 / 精灵定义目录，两种都试。
- `blitSprite` 要的是**原始精灵 id**（如 `minecraft:widget/button`），
  所以 `TextureSprite.name()` 必须存原始 id，不能换成图集里的真实名字。
- `Matrix3x2fStack` 的无参构造会 NPE（内部 `mats` 为 null），要用 `new Matrix3x2fStack(64)`。

### 6.7 GUI 两条绘制路径的**顺序**（26.2 最阴的一个坑）

26.2 的 GUI 是「状态机」，绘制分两条路径：

| 路径 | 入口 | 什么时候进入 GUI 渲染状态 |
|---|---|---|
| 顶点路径 | `renderRect` / `renderQuad` / `renderTexture`（common-api 的矩形/四边形/贴图） | 顶点先攒着，**`flush()` 时**才变成渲染状态里的元素 |
| 直接路径 | `graphics.text` / `item` / `setTooltipForNextFrame` / `blitSprite` | **立刻**进入渲染状态 |

两条路径混用时，**不先 flush 就会顺序反过来**：先画的文字会排在自己面板的贴图**前面**，
于是被面板底图盖住 —— 表现是「面板标题完全不显示」，而按钮标签正常（按钮渲染时刚好触发了 flush）。

→ `MinecraftRenderer` 里凡是走直接路径（文字 / 物品 / tooltip / blitSprite），**绘制前先 `flushPending()`**。

### 6.8 26.2 删掉的原版资源

- `textures/gui/demo_background.png`（上游 1.21.3 的面板底图）**没有了**。
  面板底图改用本 mod 自带的 `textures/gui/panel_background.png`，
  按九宫格（`SpriteScaling.NineSlice(32, 32, 6)`）画，保持上游的「浅灰面板 + 黑边 + 白高光」观感。

## 7. 渲染纪律

- 26.2 渲染栈已重构：旧的 `RenderType` / `RenderStateShard` / `MultiBufferSource` / `ShaderInstance` / `CoreShaders` **全部不存在**。
- 26.2 的 `RenderType` 构造器是 private、工厂包内可见 → **外部无法自建 RenderType**，
  只能用 `net.minecraft.client.renderer.rendertype.RenderTypes` 的现成条目。
- 自定义几何入口：`SubmitNodeCollector.submitCustomGeometry(PoseStack, RenderType, (pose, consumer) -> ...)`，
  因此**世界侧绘制必须在 `LevelRenderEvents.COLLECT_SUBMITS` 阶段调用**。
- **不自带自定义着色器**，一律用 vanilla 现成管线（`RenderTypes.OUTLINE` / `LINES` / `SECONDARY_BLOCK_OUTLINE`）+
  顶点色 —— 这样 OpenGL 与 Vulkan 两个后端都自动可用，Vulkan 零风险。
- 铁律：适配层里不得出现 `GL11` / `lwjgl.opengl` / `com.mojang.blaze3d.systems.RenderSystem` 的 GL 专属状态。
- 优先用官方 `fabric-rendering-v1`（`FabricRenderPipeline` / `SubmitRenderPhase` / `LevelRenderEvents` / `HudElementRegistry`），
  不要 mixin 渲染内部结构 —— Mojang 已经重构过两次渲染栈，第三次（Vibrant Visuals / deferred）在路上。

## 8. 「服务端装 mod、客户端没装也能进服」

这是本项目的核心诉求，任何改动都不能破坏：

1. 发包前用 `ServerPlayNetworking.canSend(player, channel)` 过滤（`FabricNetworking.Server#send` 已实现）。
2. 服务端逻辑不得假设玩家装了 mod。
3. 原版 26.2 客户端对未知 channel 走 `DiscardedPayload` 静默丢弃（不踢线）—— 这是兜底，不能当作唯一保障。

## 8. 跟版流程（服务器升级 MC 时照着做）

1. 改 `gradle.properties` 的四个版本号（`minecraft_version` / `loader_version` / `loom_version` / `fabric_api_version`）。
2. `gradlew build` —— 编译错误**只应出现在 `universal/vanilla/**` 与 `universal/fabric/**`**。
3. 检查 `MinecraftMixin` 的注入点方法名是否还在
   （26.2 是 `startAttack` / `continueAttack` / `startUseItem` / `pickBlockOrEntity`，都已是 private）。
4. 跑自测：`runServer` 起服看日志 + 双后端渲染验收（Vulkan 主测，OpenGL 必须同样可用）。
5. 升 mod 版本号（见「3.1 版本号规则」）：**MC 小版本变了就改 `+` 后缀**，跨年才动年份位；
   功能/修复计数按这一版实际改了什么来 bump。写 `CHANGELOG.md`。

预期每次跟版**小时级到一天级**，而不是重写。

### 8.1 发版检查清单

- [ ] `gradle.properties` 的 `mod_version` 按规则 bump（功能 / 修复别搞混）
- [ ] `CHANGELOG.md` 记了这一版改了什么
- [ ] `gradlew clean build` 通过，jar 名 = `McooBuildwithEase-<版本>.jar`
- [ ] 服务端 + 客户端**同一个 jar**（版本号不一致会连不上）
- [ ] 改了网络协议 / 配置格式 / 资源路径的，必须重新发版而不是只替换文件
- [ ] `TEST-CHECKLIST.md` 里的项过了（真人验收）

## 9. 范围边界（v1）

**做**：13 种建造模式、4 种替换模式、4 种变换器（含物品随机化器）、撤销/重做、被动模式、
剪贴板（复制粘贴，含方块实体与容器内容）、完整 UI、会话系统、服务端独占可玩。

**不做（推到 v1.1）**：FTB Chunks / OpenPAC 集成、快照集合/快照库/历史 UI/跨玩家分享、自定义着色器。

- 第三方集成要补时，目标是 **Carpet API**（`BuildInterceptor` 钩子留着），不是 FTB Chunks。
- 跨世界搬结构由服务器上的 **WorldEdit + Axiom + Syncmatica** 覆盖，本 mod 不重复造。
- 快照子系统在上游 3.4.0 本就是半成品（`SnapshotConfigSerializer` 空实现、库页 export/edit 按钮空实现、
  `PlayerSnapshotCapturePacket` 服务端 handler 空），v1.1 要补的话建议直接落 schematic 格式。

## 10. 对 vendored common-api 的改动（只有这一处）

`api/platform/Entrance.java` 的 `findPlugin`：

上游只捕获 `LinkageError`，假设插件实现类一定随 universal 一起打包；
我们按范围决策**不打包** FTB Chunks / OpenPAC 的实现，ServiceLoader 找不到实现时会抛
`NoSuchElementException` → 客户端一进世界就崩（自测抓到）。

已改为同时捕获 `NoSuchElementException`，插件没装时返回 `Optional.empty()`，由调用方自行降级。
拦截器（`OpenPacInterceptor` / `FtbChunksInterceptor`）本身是「插件不在就禁用」的设计，不用改。

> 改 common-api 的规矩：**只做行为修复，不动接口签名与命名**（`ResourceLocation` 等名字保持上游原样）。
