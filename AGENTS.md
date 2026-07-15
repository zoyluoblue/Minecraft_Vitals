# Vitals AI 开发规则

本仓库同时遵循上级 `../AGENTS.md`、`../MOD_DEVELOPMENT_GUIDE.md` 与
`../docs/mod-development/`。发生冲突时，以用户当前请求和上级规则为准。

## 开始前

依次阅读：

1. `README.zh-CN.md` 与 `README.md`。
2. `docs/ARCHITECTURE.md`。
3. `docs/features/health-bars/FEATURE_DESIGN.md`。
4. `docs/testing/TEST_PLAN.md`。
5. `fabric.mod.json`、Gradle 配置和相关源码。

## 稳定契约

- Mod ID：`vitals`。
- Java package：`com.zoyluo.vitals`。
- 基线：Minecraft `1.21.3`、Java `21`、Fabric Loader `0.18.4`、Fabric API `0.114.1+1.21.3`。
- Vitals 是纯客户端 Mod，不得新增服务端 entrypoint、Payload 或协议，除非先更新设计并获得用户确认。
- 优先使用 Fabric rendering event；新增 Mixin、第三方依赖或公共 API 前必须提交 ADR 并获得用户确认。
- 配置文件只保存稳定字段、数值和布尔值，不保存本地化字符串。

## UI、双语与无障碍

- 配置面板使用原生 Widget，必须支持键盘焦点、Narrator 和可见提示。
- 默认快捷键为左 `Alt/Option + V`；右 Alt/Option 不应触发。
- 玩家可见文本必须使用 translation key。
- `en_us.json` 与 `zh_cn.json` 的 key 集合必须完全一致。
- 同步维护 `README.md` 和 `README.zh-CN.md`。

## 完成门禁

至少执行：

~~~bash
./gradlew clean build --no-daemon --stacktrace
~~~

同时确认：

- `verifyTranslations` 与无依赖逻辑检查通过。
- JAR 只包含客户端 entrypoint，不包含服务端网络代码或 Mixin。
- 配置面板、语言切换、多人服务器与遮挡行为完成手工验收。
- Git diff 不包含 `run/`、日志、构建产物或无关文件。
