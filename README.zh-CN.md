# Vitals — Minecraft RPG 风格生物血条

![Vitals：体素奇幻战场中，生物头顶显示发光的 RPG 血条](images/vitals-github-poster.png)

> **Vitals 是 Minecraft 1.21.3 的纯客户端 Fabric Mod，为活体实体添加平滑、可配置的 RPG 风格头顶血条。** 只需在想看到 HUD 的客户端安装，服务器无需安装。

[English](README.md) · [下载 v1.0.1](https://github.com/zoyluoblue/Minecraft_Vitals/releases/tag/v1.0.1) · [Modrinth](https://modrinth.com/mod/zoyluo-vitals) · [反馈问题](https://github.com/zoyluoblue/Minecraft_Vitals/issues)

## 为什么选择 Vitals？

战斗信息应该一眼可读。Vitals 为附近活体实体提供清晰的 RPG 血条、精确生命数值、平滑过渡与延迟伤害拖尾；它不改变玩法、实体数据或服务器规则。

- **RPG 战斗可读性：** 世界空间血条、精确当前/最大生命、可选名称和黄色护甲值。
- **纯客户端自由：** 支持单人游戏，也可以进入没有安装 Vitals 的多人服务器。
- **显示由你决定：** 自定义实体分类、距离、缩放、小数精度与显示内容。
- **面向真实世界：** 血条会被不透明方块遮挡；实体和缓存上限让渲染保持有界。

## 一览

| 问题 | 答案 |
| --- | --- |
| 支持哪个 Minecraft 版本？ | Minecraft `1.21.3` |
| 使用哪个加载器？ | Fabric Loader `0.18.4` 或更新的兼容版本 |
| 是客户端 Mod 吗？ | 是。Vitals 没有服务端 entrypoint、Payload 或 Mixin。 |
| 服务器也要安装吗？ | 不需要。只在想显示血条的客户端安装即可。 |
| 支持多人吗？ | 支持，显示 Minecraft 已同步到客户端的生命状态。 |
| 可以自定义吗？ | 可以：显示、数字、护甲、分类、距离、缩放和精度。 |
| 支持哪些语言？ | 英文与简体中文，跟随 Minecraft 原生语言设置。 |

## 安装 Vitals

1. 为 Minecraft `1.21.3` 安装 **Fabric Loader**。
2. 下载 [Vitals v1.0.1](https://github.com/zoyluoblue/Minecraft_Vitals/releases/tag/v1.0.1) 和兼容版本的 **Fabric API**。
3. 将两个 JAR 文件放入 Minecraft 客户端的 `mods` 文件夹。
4. 使用 Fabric 配置启动 Minecraft，进入单人世界或多人服务器。

Vitals 不需要安装到服务器。每位想看到血条的玩家都在自己的客户端安装；本地显示设置不会共享给其他玩家。

### Modrinth

可通过 [Modrinth](https://modrinth.com/mod/zoyluo-vitals) 或上方 GitHub Release 下载经过验证的 Fabric `1.21.3` 版本。

## 游戏内使用

### 打开配置面板

| 平台 | 快捷键 |
| --- | --- |
| Windows / Linux | **左 Alt + V** |
| macOS | **左 Option + V** |

只有**左侧** Alt/Option 会触发快捷键。原生配置面板有“显示”和“实体”两个分页，提供实时预览、“恢复默认”、“取消”、“应用”和“完成”。存在未应用的修改时按 Escape，会先询问是否放弃修改。

### 血条会显示什么？

- 平滑生命填充和延迟伤害拖尾。
- 精确的当前/最大生命，可自定义小数精度。
- 可选的实体名称，显示在血条上方。
- 可选的**黄色护甲值**，显示在血条上方；护甲为 `0` 时自动隐藏。

默认会始终显示符合条件的满血实体。为了避免遮挡视线，Vitals 会隐藏本地玩家、隐身实体、旁观者和死亡实体。

### 选择要追踪的实体

可分别启用或关闭：玩家、原版 Boss、已驯服生物、中立生物、敌对生物、友好生物、盔甲架和其他活体实体。未知 Mod 添加、但继承 `LivingEntity` 的实体，会使用通用的“其他活体实体”分类。

## 多人和兼容性

Vitals 读取 Minecraft 已同步给客户端的生命和最大生命数据。服务器仍是权威：若服务器主动隐藏或改写客户端可见数据，Vitals 会尊重该行为。

- 不增加服务器 Mod、自定义网络协议或配置同步。
- 不使用 Mixin 或第三方配置库。
- 不强制加载区块、不修改实体状态，也不修改世界存档。
- 继承 `LivingEntity` 的 Mod 生物可走通用兼容路径；包括暮色森林在内的专用兼容，在完成定向测试前不会宣称完全支持。

## 性能边界

Vitals 只处理玩家附近、客户端已加载的实体，并优先渲染最近的合格目标。

| 边界 | 限制 |
| --- | --- |
| 可配置显示距离 | `8–64` 格；默认 `32` |
| 每帧收集的附近候选实体 | `256` |
| 每帧渲染的血条 | `96` |
| 动画缓存 | `128` 个实体；换世界或连接时清空 |

血条使用深度测试，因此会被不透明世界方块遮挡，不会穿墙显示。

## 常见问题

### 多人服务器没安装 Vitals，我还能用吗？

可以。Vitals 是纯客户端 Mod。想看到血条的客户端各自安装即可；服务器和没有安装 Mod 的玩家不会受到影响。

### 为什么血条不会穿墙？

这是刻意设计。Vitals 使用深度测试进行世界渲染，不透明方块会遮挡后方的血条。

### 能显示吸收生命或全部状态效果吗？

`1.0.0` 暂不支持。纯客户端无法可靠取得每个远端实体的这些数据，因此 Vitals 只显示可从常规客户端状态安全读取的生命数据。

### 支持其他 Mod 的生物吗？

通常支持：只要该生物继承 `LivingEntity`，并向客户端暴露常规可见的生命数据，就会走通用分类。在完成实际集成测试前，不会把特定第三方 Mod 宣传为“完全兼容”。

### 设置保存在哪里？

保存在客户端的 `config/vitals.json`。Vitals 会校验保存内容、替换旧文件时保留备份，并保留格式损坏的文件以便恢复。

## 从源码构建

要求：Java `21`。

~~~bash
./gradlew clean build --no-daemon --stacktrace
~~~

构建会检查 Java 编译、无依赖逻辑、英文/中文本地化 key 一致性、客户端专用 metadata、资源和重映射 JAR。

## 致谢与许可证

Vitals 使用 [MIT License](LICENSE) 发布。Minecraft 是 Mojang Studios 的商标；本项目与 Mojang Studios 或 Microsoft 没有隶属、合作或认可关系。
