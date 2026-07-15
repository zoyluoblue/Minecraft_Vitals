# Vitals Architecture

## 1. 产品边界

- 核心功能：为客户端已知的 `LivingEntity` 渲染可配置头顶血条。
- 目标用户：单人玩家、客户端 Mod 用户、无需服务器改造的多人玩家。
- 非目标：玩法规则、服务端状态、网络协议、世界存档、公共 API。
- 支持版本：Minecraft `1.21.3`、Fabric Loader `0.18.4`、Fabric API `0.114.1+1.21.3`、Java `21`。

## 2. 复杂度等级

Level 1。项目只有一个客户端展示领域，无游戏 registry、Payload、世界状态或跨领域写操作。由于包含渲染、配置持久化和自定义 Screen，仍维护完整 Feature Design 与 Test Plan。

## 3. 模块与依赖

~~~text
VitalsClient
  -> input/config screen
  -> config manager -> config model
  -> health bar renderer -> visibility policy
                         -> animation cache
                         -> health bar math
~~~

| 模块 | 职责 | 状态所有权 | 依赖 |
| --- | --- | --- | --- |
| `VitalsClient` | 注册客户端生命周期、快捷键和渲染事件 | 无 | Fabric client API |
| `config` | 纯 Java 配置模型与数值校验 | 无 | JDK |
| `client.config` | 配置加载、恢复和原子保存 | 本地配置 | config、Gson、FabricLoader |
| `client.screen` | 可访问的草稿式配置 UI 和预览 | 临时草稿 | config |
| `client.render` | 筛选、排序、billboard 渲染和动画 | 短期动画缓存 | client world、config |

## 4. Client/Server 边界

- 所有依赖 Minecraft client API 的源码都位于 `src/client`；`src/main` 只保存无客户端依赖的纯逻辑。
- `fabric.mod.json` 设置 `environment: client`，没有 `main` 或 server entrypoint。
- 不注册 Payload，不写世界状态，不修改实体。
- 多人数据只读取原版已同步 `LivingEntity#getHealth()` 与 `getMaxHealth()`。

## 5. 稳定契约

- Namespace：`vitals`。
- 配置路径：`config/vitals.json`。
- Translation key 前缀：`screen.vitals`、`option.vitals`、`message.vitals`、`hud.vitals`。
- 发布后配置字段只做向后兼容扩展；删除或改义需要 Schema 迁移。

## 6. 配置架构

- Schema version：`1`。
- Manager 启动时加载一次，Screen 保存后原子替换当前快照。
- 所有数字在加载和保存前 clamp，所有未知字段忽略。
- 日志使用英文；玩家错误使用 translation key。

## 7. 渲染架构

- 使用 `WorldRenderEvents.AFTER_ENTITIES`，避免注入原版 renderer。
- 只查询客户端已加载空间范围，筛选后按距离选择最近对象。
- 使用 camera-facing billboard、深度测试 RenderLayer 和 full-bright UI light。
- 实际数字直接读取最新状态；展示条和伤害拖尾仅做视觉插值。
- 世界切换或断线清空动画缓存。

## 8. 性能预算

| 系统 | 规模 | 频率 | 预算 | 诊断方式 |
| --- | --- | --- | --- | --- |
| 实体筛选 | 64 方块内已加载实体 | 每帧 | 最多收集 256 个候选、最多渲染 96 个 | debug 日志/Profiler |
| billboard | 最多 96 个 | 每帧 | 平均 `<1.5 ms` | Spark/客户端 Profiler |
| 动画缓存 | 最多 128 UUID | 每帧更新入选项 | 无持续增长 | 缓存大小断言 |
| 配置保存 | 单个小型 JSON | 用户保存时 | 原子写，非 tick | 错误日志与 UI 提示 |

## 9. 双语

- `assets/vitals/lang/en_us.json` 与 `zh_cn.json` 必须通过 key parity 门禁。
- `README.md` 与 `README.zh-CN.md` 结构同步。
- 原生语言切换后重新打开配置面板即可更新全部 UI。

## 10. 测试架构

- unit-like：无依赖 JavaExec 检查配置 clamp、数值格式和动画收敛。
- integration：Gradle build、资源处理、JAR metadata 与 client-only 检查。
- manual：单人、多人、遮挡、密集实体、组合键、双语和 UI 状态。
- GameTest：首版不适用；功能不修改服务器或世界行为。

## 11. ADR 索引

当前无 ADR。引入 Mixin、第三方配置依赖、网络协议或公共兼容 API 时必须新增 ADR。
