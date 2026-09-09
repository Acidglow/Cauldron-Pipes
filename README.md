# Cauldron-Pipes

Cauldron-Pipes is a NeoForge mod that turns vanilla cauldrons into fluid endpoints for automation mods. It is inspired by Tiled Cauldron by gisellevonbingen.

## Compatibility

- Minecraft 26.2
- NeoForge 26.2.0.0-beta or newer
- Java 25 for development

## Features

- Adds fluid tanks and NeoForge fluid capabilities to vanilla water and lava cauldrons.
- Stores partial water and lava amounts, preserving them through saves and block-state changes.
- Lets pipes and other standard fluid handlers insert and extract supported fluids.
- Lets matching buckets top up partial cauldrons; extraction requires a full bucket.
- Applies the same partial-bucket rules to dispensers.
- Fills water cauldrons in three water-bottle interactions.
- Prevents vanilla precipitation, dripstone, and interaction behavior from desynchronizing partial contents.
- Renders partial lava levels in cauldrons.

## Installation

Install the matching NeoForge version for Minecraft 26.2, then place the mod JAR in the instance or server `mods` directory. Pipe and automation mods need no dedicated integration as long as they use NeoForge fluid capabilities.

## Building and testing

Install JDK 25, then run:

```sh
./gradlew build
./gradlew runGameTestServer
```

The GameTest server runs the mod's automated interaction, persistence, dispenser, and capability-cache tests.

## License

Cauldron-Pipes is licensed under the [MIT License](LICENSE). It may be included in modpacks under that license.
