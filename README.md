# Cauldron-Pipes

Cauldron-Pipes is a NeoForge mod for Minecraft 1.21.11 that turns vanilla cauldrons into proper fluid endpoints for pipe mods.

Instead of treating a cauldron as a few hardcoded vanilla states, this mod gives cauldrons an internal fluid tank and exposes that tank through NeoForge fluid capabilities. That lets fluid transport mods interact with cauldrons in a predictable way while still keeping the vanilla cauldron behavior and visuals where possible.

This mod is inspired by Tiled Cauldron by gisellevonbingen.

Creator: Acidglow  
Minecraft: 1.21.11  
Loader: NeoForge 21.11.0-beta  
GitHub: https://github.com/Acidglow/Cauldron-Pipes

## What This Mod Does

- Adds a fluid tank to vanilla cauldrons.
- Exposes cauldrons as fluid-capable blocks for automation.
- Supports partial fluid storage instead of only empty or full states.
- Keeps cauldron block states synchronized with stored fluid data.
- Preserves partial fluid amounts through save and load.

## Supported Fluids

Currently supported:

- Water
- Lava

Water can exist in partial amounts and still maps cleanly to vanilla layered water cauldrons. Lava can also be stored partially, with a custom renderer showing the lava level inside the cauldron.

## In-Game Behavior

### Pipe and Automation Support

Cauldrons expose a NeoForge fluid capability, so mods that can insert or extract fluids from standard fluid handlers can interact with them. This mod is built around the use case of connecting pipes to cauldrons and includes Pipez as a development dependency.

### Bucket Interactions

- Buckets can top up a partially filled cauldron if the stored fluid matches.
- Buckets only extract from a managed cauldron when at least one full bucket is available.
- Dispensers follow the same rules.

This avoids losing fluid or creating inconsistent vanilla state changes when a cauldron contains a managed partial amount.

### Bottle Interactions

Water bottles can fill a managed water cauldron in partial steps:

- 333 mB
- 666 mB
- 1000 mB

That means three water bottles will fill an empty cauldron to a full bucket.

### Vanilla Mutation Protection

When a cauldron is holding a managed fluid amount that does not match a normal vanilla state, the mod blocks vanilla behaviors that would otherwise desync the cauldron, including:

- precipitation filling
- dripstone filling
- lowering fill level through vanilla logic
- extracting full buckets from partial contents

## Visuals

Partial lava is rendered inside the cauldron so the stored amount is visible instead of silently existing only in block entity data.

## Tested Behaviors

The mod includes game tests that cover:

- water bottle fill progression
- topping up a partially filled water cauldron with a bucket
- persistence of partial lava through save/load
- dispenser extraction only when a full bucket is available

## Installation

Install:

- Minecraft 1.21.11
- NeoForge 21.11.0-beta or newer in the declared range
- Cauldron-Pipes
- a fluid transport mod if you want automation, such as Pipez

## License

MIT

## Modpacks

You may use this mod in modpacks under the MIT license.
