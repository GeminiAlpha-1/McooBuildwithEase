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
| mod_version | 26.0.0 | 年份法，与 MC 的 26.x 对齐；功能 `26.1.0`、修复 `26.0.1` |

⚠️ 编译版本必须 ≤ 服务器版本：用更新的 Fabric API 编译会引入服务器没有的类/方法 → 运行时 `NoSuchMethodError`。

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
- 自测服务器：`testserver/`（不入库，从 `D:\WorkSpace\McooServerFeatures\testserver` 复制），
  脚本 `_tools/self-test.ps1` 会构建 → 装 jar → 起服 → 抓日志。
- 查 26.2 API：类索引 `_probe\classes-26.2.txt`（10952 个类）+ javap，不要凭 1.21 记忆猜。

## 5. mixin 纪律

**mixin 是跟版成本的主要来源**（vanilla 内部结构每版都动）。当前只保留一个：

| mixin | 为什么必须留 |
|---|---|
| `dev.huskuraft/universal/fabric/mixin/MinecraftMixin` | 客户端攻击 / 使用 / 选取方块的拦截，Fabric 没有官方 API |

其余一律用官方 Fabric API 替代（已删：`ShaderManagerMixin` / `PlayerListMixin` / `GuiMixin` / `KeyboardMixin`）。
新增 mixin 前先确认官方 API 真的没有；注入点一律放 **方法 HEAD**，不要用深层 `INVOKE` 锚点。

## 6. 渲染纪律

- 26.2 渲染栈已重构：旧的 `RenderType` / `RenderStateShard` / `MultiBufferSource` / `ShaderInstance` / `CoreShaders` **全部不存在**。
- **不自带自定义着色器**，一律用 vanilla 现成管线（`RenderTypes.OUTLINE` / `LINES` / `SECONDARY_BLOCK_OUTLINE`）+
  顶点色 —— 这样 OpenGL 与 Vulkan 两个后端都自动可用，Vulkan 零风险。
- 铁律：适配层里不得出现 `GL11` / `lwjgl.opengl` / `com.mojang.blaze3d.systems.RenderSystem` 的 GL 专属状态。
- 优先用官方 `fabric-rendering-v1`（`FabricRenderPipeline` / `SubmitRenderPhase` / `LevelRenderEvents` / `HudElementRegistry`），
  不要 mixin 渲染内部结构 —— Mojang 已经重构过两次渲染栈，第三次（Vibrant Visuals / deferred）在路上。

## 7. 「服务端装 mod、客户端没装也能进服」

这是本项目的核心诉求，任何改动都不能破坏：

1. 发包前用 `ServerPlayNetworking.canSend(player, channel)` 过滤（`FabricNetworking.Server#send` 已实现）。
2. 服务端逻辑不得假设玩家装了 mod。
3. 原版 26.2 客户端对未知 channel 走 `DiscardedPayload` 静默丢弃（不踢线）—— 这是兜底，不能当作唯一保障。

## 8. 跟版流程（服务器升级 MC 时照着做）

1. 改 `gradle.properties` 的四个版本号。
2. `gradlew build` —— 编译错误**只应出现在 `universal/vanilla/**` 与 `universal/fabric/**`**。
3. 检查 `MinecraftMixin` 的注入点方法名是否还在
   （26.2 是 `startAttack` / `continueAttack` / `startUseItem` / `pickBlockOrEntity`，都已是 private）。
4. 跑自测：`runServer` 起服看日志 + 双后端渲染验收（Vulkan 主测，OpenGL 必须同样可用）。
5. 升 mod 版本号（年份法），写 CHANGELOG。

预期每次跟版**小时级到一天级**，而不是重写。

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
