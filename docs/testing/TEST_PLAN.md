# Test Plan：Vitals 1.0.0

## 1. 范围

- In scope：配置逻辑、组合键、头顶渲染、过滤、遮挡、动画、双语、客户端专用打包。
- Out of scope：服务器 Payload、世界存档、Mod 专用 adapter、Boss Bar 替换。
- Target：Minecraft `1.21.3` + Fabric Loader `0.18.4` + Fabric API `0.114.1+1.21.3`。
- Fixtures：普通生物、敌对生物、玩家、隐身实体、高生命实体、密集实体区。

## 2. Requirement Matrix

| ID | Requirement | Level | Procedure | Expected | Evidence | Status |
| --- | --- | --- | --- | --- | --- | --- |
| R-001 | 所有合格生物显示头顶血条 | manual | 单人生成多类实体 | 名称、条和数字正确且无闪烁 | screenshot | PENDING |
| R-002 | 左 Alt/Option + V 打开配置 | unit/manual | 边沿逻辑检查并在 Windows/macOS 分别按键 | 仅左修饰键触发 | `runLogicChecks` + device recording | PARTIAL |
| R-003 | 设置保存和重载 | integration/manual | 修改、保存、重启客户端 | JSON 与 UI 一致 | generated config/log | PARTIAL |
| R-004 | 平滑生命与拖尾 | unit/manual | 实体受伤和治疗 | 收敛、无超界或跳变 | `runLogicChecks` + video | PARTIAL |
| R-005 | 方块遮挡和隐身安全 | manual | 墙后、隐身、死亡 | 不显示 | screenshots | PENDING |
| R-006 | 无服务器 Mod 的多人兼容 | manual | 进入 vanilla/Fabric server | 可显示已同步生命，无断线 | server log/video | PARTIAL |
| R-007 | 双语和语言切换 | build/manual | 切换 zh_cn/en_us | key 完整、文本更新 | `verifyTranslations` + screenshots | PARTIAL |
| R-008 | client-only dedicated server 安全 | integration | 检查 metadata/JAR，服务端不安装该 JAR | 无 server entrypoint/class load | Gradle + server log | PASS |
| R-009 | 高密度性能有界 | manual/perf | 200 实体场景 | 只渲染最近上限，缓存稳定 | profiler | PENDING |

## 3. 自动检查

- `verifyTranslations`：JSON、key parity、空值和占位符一致性。
- `runLogicChecks`：配置 clamp、精度格式、比例、动画收敛，以及左 Alt 组合键边沿的长按、Screen 打开和失焦用例。右 Alt 仍需真机确认不会触发。
- `build`：Java 21 编译、资源展开、sources JAR 和 remap JAR。
- `verifyClientOnlyMetadata`：确认 `environment=client`、只有 client entrypoint、无 Mixin 声明。

## 4. Client/Server Tests

- 客户端进入单人世界。
- 客户端进入未安装 Vitals 的多人服务器。
- 断线重连与换维度后动画缓存不串状态。
- 未安装 Vitals 的其他客户端不受影响。
- 首版无 Payload validation/version mismatch 测试。

## 5. Persistence Tests

- 首次启动创建默认配置。
- 合法配置重启回读。
- 缺失字段回退默认值。
- 超界数值被 clamp。
- 损坏 JSON 会被保留，优先恢复 `.bak`，否则使用默认配置并记录警告；不修改世界存档。
- 保存失败时 UI 保持打开并显示错误。

## 6. Localization Tests

- `zh_cn.json` 和 `en_us.json` 可解析且 key 相同。
- 中文、英文下检查标题、控件、按钮、错误、成功消息和快捷键说明。
- 在原生语言设置切换后重新打开面板，文本随语言变化。

## 7. Performance Tests

| Scenario | Scale | Duration | Metrics | Budget | Result |
| --- | --- | --- | --- | --- | --- |
| 普通游戏 | 40 loaded / 20 visible | 5 min | frame time/cache | 无可感知卡顿、缓存稳定 | pending |
| 实体农场 | 200 loaded / 96 cap | 5 min | avg/p95 render stage | avg `<1.5 ms`，无增长 | pending |
| 反复换维度 | 20 changes | 10 min | cache/memory | 旧 UUID 状态清空 | pending |

## 8. Manual Acceptance

1. 在中文语言进入世界，确认满血和受伤的友好、敌对、玩家实体，以及驯服狼和驯服马；持续观察并平移、旋转镜头 30 秒，确认条、伤害拖尾和背景不会闪烁或互相覆盖。
2. 开启护甲显示后，确认非零护甲以黄色显示在血条上方；护甲为 `0` 时不显示护甲行。
3. 用墙体完全遮挡实体，确认血条不会穿墙。
4. 测试隐身、死亡、旁观者和本地玩家。
5. 打开配置，逐项修改并观察预览；“取消”不生效，“应用”保存，“恢复默认”只替换当前草稿。
6. 切换英文，重新打开面板并重复关键流程。
7. 连接无 Vitals 的多人服务器，观察普通实体和远端玩家。
8. 在 Windows 与 macOS 分别验证左 Alt/Option；右侧修饰键不得触发。

## 9. Unverified

- Twilight Forest 和其他第三方实体兼容需后续安装目标 Mod 实测。
- Windows/macOS 双平台物理键位必须在对应真实设备完成最终验收。
- GPU 性能预算需要运行客户端场景后填写数据。

## 10. 当前验证证据（2026-07-15）

- `./gradlew clean build --warning-mode all --no-daemon --stacktrace`：PASS；14 个任务完成，无 Gradle 弃用警告，包含 Java 21 编译、逻辑检查、双语检查、client-only metadata 检查和正式 remap JAR。
- `./gradlew runServerSmoke --no-daemon --stacktrace`：PASS；独立服务端加载完成到 `Done`，Mod 列表不含 `vitals`，随后正常保存并退出。
- `unzip -t build/libs/vitals-fabric-1.21.3-loader0.18.4-1.0.0.jar`：PASS；JAR 完整且包含两套语言资源。
- 客户端启动检查：PASS；Fabric 加载 `Vitals 1.0.0`、资源加载和默认配置生成均无 Vitals 错误。
- 损坏配置恢复：PASS；截断 `vitals.json` 后，客户端保留带时间戳的损坏原件并重新生成可解析的有效配置。
- 多人自动 quick-play 尝试未建立连接，因此 R-006 的客户端入服和实际头顶渲染仍保持 PARTIAL，不计为通过。
