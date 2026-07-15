# Vitals

Vitals is a client-only Fabric mod that renders configurable RPG-style health bars above living entities. It works in single-player and on multiplayer servers without requiring a server-side installation.

[简体中文](README.zh-CN.md)

## Features

- World-space health bars above nearby living entities.
- Smooth health transitions and a delayed damage trail.
- Exact current/max health values with configurable decimal precision.
- Optional entity name and non-zero armor value above the health bar.
- Separate filters for players, vanilla bosses, tamed, neutral, hostile and passive creatures, armor stands, and other living entities.
- Depth-tested rendering: bars are hidden behind opaque world geometry.
- Built-in bilingual configuration screen with a live preview.
- No Mixin, no custom network protocol, and no third-party configuration library.

## Open the configuration screen

- Windows/Linux: **Left Alt + V**
- macOS: **Left Option + V**

Only the left modifier key triggers the shortcut. The screen provides Display and Entities pages, live preview, defaults, Cancel, Apply, and Done actions. Pressing Escape with unapplied changes asks before discarding them.

Settings are stored in `config/vitals.json`. Saves use a temporary file, validation, backup, and atomic replacement when supported.

## Multiplayer behavior

The server does not need Vitals. The mod reads the health and maximum-health state that Minecraft already synchronizes to each client.

Every player who wants to see health bars must install Vitals on their own client. Health bars and settings are not pushed to players who do not have the mod, and local configuration is not shared between clients.

## Supported environment

| Component | Version |
| --- | --- |
| Minecraft | `1.21.3` |
| Java | `21` |
| Fabric Loader | `0.18.4` or newer compatible release |
| Fabric API | `0.114.1+1.21.3` or newer compatible release |

Vitals is client-only. Install the JAR in the client's `mods` directory together with Fabric API.

## Compatibility and limitations

- Unknown modded `LivingEntity` implementations use the generic “Other Living Entities” filter.
- Dedicated adapters for mods such as The Twilight Forest are planned after targeted compatibility testing.
- Absorption health and complete status effects are not shown in the initial release because a client-only mod cannot reliably obtain those values for every remote entity.
- The local player is hidden to avoid a first-person camera overlap.
- Vanilla nameplates are respected; Vitals avoids drawing a duplicate name when Minecraft is already rendering one.
- Servers that intentionally hide or rewrite client-visible health remain authoritative.

## Performance boundaries

- Configurable range: `8–64` blocks; default `32`.
- At most `256` nearby candidates are collected and at most `96` bars are rendered per frame.
- The animation cache is capped at `128` entities and is cleared across world/connection lifecycle changes.
- Vitals never force-loads chunks and never changes entity state.

## Build

~~~bash
./gradlew clean build --no-daemon --stacktrace
~~~

The build verifies compilation, dependency-free logic checks, localization parity, client-only metadata, resources, and the remapped JAR.

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Feature design](docs/features/health-bars/FEATURE_DESIGN.md)
- [Test plan](docs/testing/TEST_PLAN.md)
- [Release process](RELEASING.md)

## License

[MIT](LICENSE)
