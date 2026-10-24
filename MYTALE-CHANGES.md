# MyTale changes to EasyHunger

This is a modified version of EasyHunger by haasapenas (based on 1.2.4, upstream commit `35c8e29`,
2026-03-27), itself based on Hungry by Aex12. The original repository (github.com/haasapenas/EasyHunger)
is no longer public; its full history is kept in this repository. It runs on
the MyTale Hytale server (mytale.es). It is distributed under the same license as the original, the
GNU Affero General Public License v3.0 (see `LICENSE`). The source of every version that runs in
production is published in this repository; the exact commit of each one is tagged `mytale-<version>`.

All changes are by MyTale (HyThanoz). This repository has one commit per published version. Summary:

| Version | Date | Change |
|---|---|---|
| 1.2.4 → 1.5.8 | before 2026-09-29 | Work done before this repository used git, ports to newer Hytale versions, `DisabledWorlds` and the staff panel `/ehconfig`, among others |
| 1.5.9 | 2026-09-30 | Fixes: Starving removed when leaving 0, HUD resend, range checks and apply-on-blur in the config panel |
| 1.5.10 | 2026-10-02 | Chat messages in Spanish through a shared `ChatStyle` |
| 1.5.11 | 2026-10-03 | Staff panel restyled with the MyTale UI tokens |
| 1.5.12–1.5.15 | 2026-10-03 | Dead code removed, constants named, thirst only counts real drinks, world gate respected everywhere, logging fixes, `/eh addall` uses the value it is given, biome priority fix |
| 1.5.16–1.5.18 | 2026-10-05 | Values panel layout, player and staff texts in Spanish, English and Portuguese |
| 1.5.19 | 2026-10-10 | `DisabledWorlds` accepts a prefix wildcard (`instance-Name*`) and is editable in the panel |
| 1.5.20 | 2026-10-14 | New "bars" HUD style (`HungerBar.ui`, `WaterBar.ui`, `RuneLayout`); the config panel reports rejected values |
| 1.5.21 | 2026-10-14 | Bar textures redrawn |
| 1.5.22 | 2026-10-19 | The values panel opens one step later (`AperturaDiferida`), so the client does not get stuck on "Loading..." |
| 1.5.23 | 2026-10-19 | Bar textures flipped vertically (tip on top) |
| 1.5.24 | 2026-10-20 | Plates and soups that use the drink chain now restore hunger when they are listed as food (`ConsumableKind`) |
| 1.5.25 | 2026-10-20 | `/eh addall` sends juices and other drinks to Drinks by name |
| 1.5.26 | 2026-10-20 | Shared `Lang` piece updated (language chosen in MytaleComunidad). Version running in production |

Shared MyTale pieces copied into this mod (`utils/ChatStyle`, `utils/Lang`, `commands/AperturaDiferida`)
are distributed here under the AGPL-3.0 as part of this work.

## Build

```
./gradlew jar
```

Output: `build/libs/EasyHunger-<version>.jar`.
