# Vitals — RPG Health Bars for Minecraft

![Vitals: luminous RPG health bars floating above creatures in a voxel fantasy battle](images/vitals-github-poster.png)

> **Vitals is a client-side Fabric mod for Minecraft 1.21.3 that adds smooth, configurable RPG-style health bars above living entities.** Install it only on the clients that want the HUD; a server-side installation is not required.

[简体中文](README.zh-CN.md) · [Download v1.0.1](https://github.com/zoyluoblue/Minecraft_Vitals/releases/tag/v1.0.1) · [Report an issue](https://github.com/zoyluoblue/Minecraft_Vitals/issues)

## Why Vitals?

Combat should be readable at a glance. Vitals turns nearby living entities into clear RPG targets with an exact health readout, a smooth health transition, and a delayed damage trail—without changing gameplay, entity data, or server rules.

- **RPG clarity:** world-space health bars, exact current/max health, optional names, and yellow armor values.
- **Client-only freedom:** works in single-player and can join multiplayer servers that do not install Vitals.
- **Your view, your rules:** choose categories, range, scale, decimal precision, and what each bar shows.
- **Built for real worlds:** depth-tested bars stay behind opaque blocks; entity and cache caps keep rendering bounded.

## At a glance

| Question | Answer |
| --- | --- |
| What Minecraft version? | Minecraft `1.21.3` |
| Which mod loader? | Fabric Loader `0.18.4` or a newer compatible release |
| Is it client-side? | Yes. Vitals has no server entrypoint, Payload, or Mixin. |
| Does a server need the mod? | No. Install it only on the clients that want health bars. |
| Is multiplayer supported? | Yes, for the health state Minecraft already synchronizes to your client. |
| Is it configurable? | Yes: display, numbers, armor, categories, range, scale, and precision. |
| Languages | English and Simplified Chinese, following Minecraft's language setting. |

## Install Vitals

1. Install **Fabric Loader** for Minecraft `1.21.3`.
2. Download [Vitals v1.0.1](https://github.com/zoyluoblue/Minecraft_Vitals/releases/tag/v1.0.1) and a compatible **Fabric API** release.
3. Put both JAR files in your Minecraft client's `mods` folder.
4. Start Minecraft with the Fabric profile and enter a world or server.

Vitals does not need to be installed on the server. Each player who wants to see health bars installs it on their own client; local display settings are not shared with other players.

## Use it in game

### Open the configuration screen

| Platform | Shortcut |
| --- | --- |
| Windows / Linux | **Left Alt + V** |
| macOS | **Left Option + V** |

Only the **left** Alt/Option key triggers the shortcut. The native configuration screen has **Display** and **Entities** pages, a live preview, **Defaults**, **Cancel**, **Apply**, and **Done** actions. Pressing Escape with unapplied edits asks before discarding them.

### What the bar shows

- A smooth health fill plus a delayed damage trail.
- Exact current and maximum health, with configurable decimal precision.
- Optional entity name above the bar.
- Optional **yellow armor value** above the bar. Armor is hidden when it is `0`.

Vitals always renders eligible, full-health entities by default. It hides the local player, invisible entities, spectators, and dead entities so the HUD stays readable.

### Choose what to track

Enable or disable independent filters for players, vanilla bosses, tamed, neutral, hostile, passive creatures, armor stands, and other living entities. Unknown modded entities that extend `LivingEntity` use the generic **Other Living Entities** filter.

## Multiplayer and compatibility

Vitals reads the health and maximum-health values that Minecraft already provides to your client. The server remains authoritative: if a server deliberately hides or rewrites a value, Vitals respects that behavior.

- No server mod, custom protocol, or configuration sync is added.
- No Mixin or third-party configuration library is used.
- Vitals does not force-load chunks, change entity state, or modify world saves.
- Modded `LivingEntity` implementations receive generic support; dedicated integrations, including The Twilight Forest, require targeted compatibility testing before they are claimed as supported.

## Performance boundaries

Vitals only evaluates loaded entities near the player and renders the closest eligible targets.

| Boundary | Limit |
| --- | --- |
| Configurable range | `8–64` blocks; default `32` |
| Nearby candidates collected per frame | `256` |
| Health bars rendered per frame | `96` |
| Animation cache | `128` entities, cleared when worlds/connections change |

Bars are depth-tested, so opaque world geometry occludes them rather than allowing them to render through walls.

## FAQ

### Does Vitals work on a multiplayer server without installing it on the server?

Yes. Vitals is client-only. Install it on each client that wants the display; the server and players without the mod are unaffected.

### Why does a health bar not show through a wall?

That is intentional. Vitals uses depth-tested world rendering, so opaque blocks hide bars behind them.

### Can I see absorption hearts or every status effect?

Not in `1.0.0`. A client-only mod cannot reliably obtain those values for every remote entity, so Vitals only presents health data that is safe to read from normal client state.

### Does Vitals work with modded mobs?

Usually, if the mob is a `LivingEntity` and exposes normal client-visible health. It is handled by the generic filter. Specific third-party mods are not advertised as fully tested until an integration pass is complete.

### Where are my settings stored?

On your client at `config/vitals.json`. Vitals validates saves, keeps a backup when replacing a previous file, and preserves malformed files for recovery.

## Build from source

Requirements: Java `21`.

~~~bash
./gradlew clean build --no-daemon --stacktrace
~~~

The build checks Java compilation, dependency-free logic, English/Chinese localization parity, client-only metadata, resources, and the remapped JAR.

## Credits and license

Vitals is released under the [MIT License](LICENSE). Minecraft is a trademark of Mojang Studios; this project is not affiliated with or endorsed by Mojang Studios or Microsoft.
