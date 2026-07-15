# Feature Design：头顶生物血条

## 1. 元数据

- Owner：zoyluo
- Status：approved
- Target version：`1.0.0`
- Related ADR：无；首版不引入依赖、协议、公共 API 或 Mixin
- Related issues：初始项目需求

## 2. 目标与成功标准

Vitals 为单人和多人游戏中的可见生物提供 RPG 风格头顶血条。

成功标准：

1. 所有满足过滤条件的 `LivingEntity` 在头顶显示名称、平滑血条与精确生命数字。
2. 血条受世界深度遮挡，不透过方块显示；隐身、死亡、旁观者和本地玩家默认隐藏。
3. 左 `Alt + V`（Windows）或左 `Option + V`（macOS）打开内置配置面板。
4. 配置支持启用状态、显示距离、UI 缩放、数字精度、名称/数字/护甲，以及玩家、Boss、驯服、敌对、中立、友好、盔甲架和其他活体分类开关。
5. 纯客户端安装即可连接未安装 Vitals 的多人服务器，并展示原版已同步的实体生命状态。
6. 在高密度场景中有距离、数量和缓存上限，不进行区块加载或服务端扫描。

## 3. 非目标

- 首版不显示吸收生命、完整状态效果或伤害跳字；这些远端数据无法由纯客户端可靠获得。
- 首版不提供服务器强制配置、配置共享、Payload 或协议。
- 首版不针对 Twilight Forest 等 Mod 编写专用 adapter；未知 `LivingEntity` 走通用兼容路径。
- 不替换原版 Boss Bar，不修改生命值、伤害或实体状态。
- 不引入 Mod Menu、Cloth Config 或其他第三方配置依赖。

## 4. 用户与玩法流程

### 单人玩家

进入世界后，符合条件的生物始终显示血条。玩家可用组合键进入配置面板，预览修改，保存后立即生效。

### 多人玩家

每位希望看到血条的玩家需要在自己的客户端安装 Vitals；服务器和其他客户端无需安装。显示只读取客户端从服务器收到的原版状态，不把本地推测写回服务器。

### 服务器管理员

无服务器组件、命令或存档变更。

### 其他 Mod 集成方

首版无公共 API。只要实体继承 `LivingEntity` 且向客户端同步原版生命属性，即可获得通用血条。

## 5. 功能状态机

配置面板状态：

~~~text
closed -> editing draft -> saved -> closed
                      -> cancelled -> closed
                      -> save error -> editing draft
~~~

渲染状态：

~~~text
eligible -> visible -> animated toward latest health
eligible -> occluded -> hidden by depth buffer
ineligible/removed/world change -> cache entry pruned
~~~

## 6. 模块影响

| 模块 | 变化 | Owner | 风险 |
| --- | --- | --- | --- |
| bootstrap | client entrypoint 和事件接线 | client | 低 |
| config | JSON 配置、校验、原子替换 | client | 中 |
| input | 左 Alt/Option + V 边沿检测 | client | 低 |
| render | 实体筛选、billboard、动画缓存 | client | 中 |
| screen | 原生 Widget 配置面板和预览 | client | 中 |
| network | 无 | - | 无 |
| persistence | 仅客户端配置文件 | client | 低 |

## 7. 数据与持久化

- 状态所有者：当前实体生命属于服务器；UI 配置属于本地客户端。
- 配置路径：`config/vitals.json`。
- Schema：`schemaVersion = 1`。
- 默认值：代码内集中定义，读取后统一 clamp/normalize。
- 保存：写临时文件、强制落盘并复读校验后，备份旧配置再原子替换；失败时保留原配置并显示可翻译错误。
- 损坏恢复：保留带时间戳的损坏原件，尝试读取 `.bak`；无法恢复时使用默认值，并尝试写回有效主配置。
- 迁移：未知字段忽略；缺失字段使用默认值；未来 Schema 变化单独设计。
- registry ID：无新增游戏内容 registry。
- 旧存档行为：不读取或修改世界存档。

## 8. 服务端与客户端

- 服务端权威状态：实体生命和最大生命仍由服务器决定。
- 客户端输入：仅配置 UI 和组合键。
- 客户端预测：只对显示值做视觉插值，不预测真实生命。
- S2C read model：复用原版客户端 `LivingEntity` 状态。
- 断线、重连、换维度：清空动画缓存；配置保留。
- dedicated server：`environment: client` 且只注册 client entrypoint。

## 9. 网络

- Payload：无。
- 协议版本：无。
- 限制：未安装 Mod 的客户端不会显示血条；服务器未同步的数据不能可靠展示。

## 10. UI 与双语

- ready：Display 与 Entities 两个分页、可交互配置项和实时示例。
- disabled：关闭总开关时其余控件禁用，但值会保留；重新启用后可继续编辑。
- error：保存失败时在面板显示错误并保持草稿。
- success：Apply 保存并保持面板；Done 保存后关闭；均提供可翻译反馈。
- empty/loading/permission/stale：本地同步面板不适用。
- 所有标题、控件、提示、快捷键说明和消息使用 translation key。
- `zh_cn` 与 `en_us` key 完全一致，语言使用 Minecraft 原生设置切换。

## 11. 性能

- 触发频率：每个世界渲染帧一次，仅客户端。
- 最大范围：配置范围 `8-64` 方块，默认 `32`。
- 最大渲染对象：每帧最多收集 `256` 个候选、渲染 `96` 个，优先最近实体。
- 缓存：按 UUID 保存最多 `128` 个动画状态；未见实体定期清理。
- 区块加载：绝不强制加载区块，只处理客户端已加载实体。
- 异步：无。
- 目标基准：200 个已加载实体、96 个入选血条时，血条阶段平均低于 `1.5 ms/frame`，不得产生持续增长的缓存。

## 12. 兼容与 API

- 公共 API：无。
- optional integration：后续为 Twilight Forest 等项目单独设计。
- Mixin：无。
- 通用降级：无法获得合法最大生命、实体不可见或状态无效时不渲染。
- 已知限制：部分服务器可隐藏或修改客户端可见生命；首版不绕过服务器策略。

## 13. 测试

见 `docs/testing/TEST_PLAN.md`。纯逻辑使用无第三方依赖的 Java 检查；渲染、输入和语言切换执行客户端手工验收。

## 14. 发布与回滚

- Release 类型：初始 `1.0.0`。
- Migration Notes：无世界或服务器迁移。
- 回滚：删除客户端 JAR 即可；仅在需要恢复默认设置时删除 `config/vitals.json`。
- 不可逆变化：无。

## 15. 已决策默认值

- 所有实体分类默认开启；分类优先级为盔甲架、玩家、原版 Boss、已驯服生物、中立生物、敌对生物、友好生物、其他活体，每个实体只命中一个分类开关。
- 始终显示符合条件的血条，包括满血实体。
- 本地玩家、隐身实体、旁观者和死亡实体隐藏。
- 方块遮挡由深度缓冲处理，不提供穿墙选项。
- 名称、生命数字默认开启；数字精度默认两位小数，尾随零会省略。
