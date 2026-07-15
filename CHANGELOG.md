# Changelog

All notable changes to Vitals are documented here.

## [Unreleased]

## [1.0.0] - 2026-07-15

### Added

- Initial client-only RPG health bar renderer for Minecraft 1.21.3.
- Built-in bilingual configuration screen opened with Left Alt/Option + V.
- Exact health values, optional armor, entity filters, distance, scale, and decimal precision settings.
- Smooth health animation and delayed damage trail.
- Atomic client configuration persistence with backup and corrupt-file preservation.
- Localization, logic, metadata, CI, and release verification gates.

### Fixed

- Eliminated health-bar flicker caused by overlapping coplanar translucent quads.

### Changed

- Non-zero armor is rendered as a yellow line above the health bar; zero armor is hidden.

### Compatibility

- Generic support for modded `LivingEntity` implementations.
- No server installation, Mixin, custom protocol, public API, or save migration.

### Known Issues

- Windows/macOS shortcut behavior and world-space visuals require final in-game acceptance on physical platforms.
- Dedicated Twilight Forest integration has not yet been tested.
