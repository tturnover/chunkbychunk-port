# Chunk By Chunk

This mod is built around the concept of a single-chunk skyworld with the ability to expand it over time. This is achieved
by maintaining a generation dimension and copying chunks out of it as necessary. Further mod features expand upon this
core concept.

## Usage 

See the [wiki](https://github.com/immortius/chunkbychunk/wiki) for information on configuration

## Changes in this port (3.0.0 - Minecraft 26.3)

This port brings Chunk By Chunk from 1.18.2/1.20.4 up to **Minecraft 26.3**, and adds content from the versions in between.

### Platform

- Targets Minecraft **26.3**. Requires **Java 25**.
- Ships for **Fabric** (Loader 0.19.5+, Fabric API) and **NeoForge** (26.3.0.95+). The old Forge build has been replaced
  by NeoForge.
- Optional integrations updated: JEI 31.16 (both loaders) and Mod Menu 21 (Fabric).
- Built with the current MultiLoader Template layout (`common`, `fabric`, `neoforge` modules, Gradle 9.8, Loom 1.18,
  ModDevGradle 2). Minecraft 26.x ships unobfuscated, so the build no longer uses mappings or refmaps.

### New biome themed chunk spawners

Each new spawner is crafted from a World Core surrounded by 4 copper ingots and the ingredients below. The recipes avoid
items that can only be found in the biome itself.

| Spawner | Biomes spawned | Extra ingredients |
|---|---|---|
| Cherry Grove Chunk Spawner | Cherry Grove | 2 pink dye, 2 saplings |
| Mangrove Swamp Chunk Spawner | Mangrove Swamp | 2 mud, 2 water buckets |
| Pale Garden Chunk Spawner | Pale Garden | 2 bone meal, 2 logs |
| Deep Dark Chunk Spawner | Deep Dark | 2 amethyst shards, 2 cobbled deepslate |
| Ocean Chunk Spawner | Ocean, Deep Ocean, Warm/Lukewarm/Cold (and deep) Oceans | 2 water buckets, 2 gravel |
| Sulfur Caves Chunk Spawner | Sulfur Caves | 2 gunpowder, 2 yellow dye |

All new spawners work with the World Mender, count towards the "Particular Tastes" advancement, can be taken apart like
the other spawners, and have English, Chinese and Ukrainian names.

### Existing themes expanded

- **Swamp** now includes Mangrove Swamp.
- **Forest** now includes Pale Garden, Dappled Forest (26.3) and Cherry Grove.
- **Mountain** now includes Cherry Grove.

### World Scanner

New scanner targets:

| Input item(s) | Scans for |
|---|---|
| Netherite scrap, ancient debris | Ancient debris |
| Amethyst shard | Budding amethyst, amethyst blocks and clusters (geodes) |
| Nether quartz | Nether quartz ore |
| Trial key, ominous trial key | Trial spawners and vaults (trial chambers) |
| Echo shard | Sculk shriekers, sculk catalysts, reinforced deepslate (ancient cities) |

### Recipes, fuel and loot

- The World Mender recipe now requires a **Heavy Core** (2 dispensers, heavy core, world core), replacing the old
  4-dispenser recipe.
- The World Forge now only produces World Fragments; it no longer fuses them into Shards, Crystals and Cores. Those
  are crafted at a crafting table (4 Fragments → Shard, 4 Shards → Crystal, 4 Crystals → Core).
- World Forge **strong fuel** now accepts echo shards, trial keys and breeze rods.
- World Forge fuel comes from existing item tags rather than the mod's own, so blocks from other mods in those tags
  work too:
  - **soil fuel**: `#minecraft:dirt`, `#minecraft:grass_blocks`, `#minecraft:moss_blocks`, `#minecraft:mud`,
    `#minecraft:sand`, `#minecraft:soul_fire_base_blocks`, `#c:sands`, `#c:gravels`;
  - **stone fuel**: `#minecraft:stone_crafting_materials`, `#minecraft:terracotta`, `#c:stones`, `#c:cobblestones`,
    `#c:netherracks`, `#c:end_stones`, `#c:sandstone/blocks`;
  - **strong fuel**: echo shards, trial keys and breeze rods (no existing tag covers these).
  The mod's own `weakworldforgefuel`, `worldforgefuel` and `strongworldforgefuel` tags have been removed.
- The randomised chunk chest can now also contain a trial key or breeze rods.

### Aiming chunk spawners

- Chunk spawners no longer need to be placed on the edge of a chunk. Using a spawner spawns its own chunk if that is
  empty, otherwise the neighbouring chunk on the far side of the spawner from the player using it (the chunk in front
  of them).
- The target depends only on the player who uses the spawner, so it works the same for every player in multiplayer.
- Looking at a placed chunk spawner shows a floating arrow (like the barrier marker) above the block, or below it when
  your head is below the block. It points at the chunk that would spawn if you used it from where you stand: green if
  that chunk is empty, red if it already exists, blue if the spawner will fill its own chunk, orange if the target
  chunk isn't loaded. Each player sees their own arrow.
- If the target chunk already exists, "No space" appears above the hotbar, framed by glitching symbols, and decodes
  from fully glitched text to readable over 2 seconds. If the chunk is already being spawned or the spawner does not
  work in this dimension, a plain message is shown instead. In all cases the spawner is not used up.

### Mining

- All Chunk By Chunk blocks (chunk spawners, World Core, World Forge, World Scanner, World Mender) now drop when broken
  by hand. The pickaxe is the preferred tool and mines them faster.

### Technical changes (for pack makers)

- Data folders use the modern singular names: `recipe/`, `loot_table/`, `advancement/`, `tags/item/`, `tags/block/`.
- Recipes use the new ingredient format (`"minecraft:dirt"` / `"#minecraft:logs"`) and `result: {"id": ...}`.
- Items have item model definitions under `assets/chunkbychunk/items/`.
- The biome theme dimension type (`chunkbychunk:overworld_theme`) mirrors the 26.3 overworld dimension type.
- Chunk spawn progress is now saved to `data/chunkbychunk/chunkspawncontroller.dat`. A chunk spawn that was in progress
  in an older world is not carried over.
- Server-to-client config sync uses a custom payload (`chunkbychunk:config`) on both loaders.

### Taking spawners apart

- Shift + left click a placed chunk spawner while holding a pickaxe to turn it back into a World Core block. The other
  ingredients of its crafting recipe drop on the ground (ingredients that give back a container when crafted, such as
  water buckets, are not returned), and the pickaxe loses 1 durability. Works in creative too.
- This replaces the old crafting recipe that took a spawner apart with an iron pickaxe.
- JEI shows how to do this on every chunk spawner's info page, using your own sneak and attack keys.

### Continuing random chunks

- Chunk spawners used in a chunk that came from an Unstable Chunk Spawner continue that chunk's terrain, instead of
  spawning the world's own chunk at that position. Chunks spawned this way continue it further, so a random chunk can
  be grown into a whole area of its original terrain.
- This applies to the plain Chunk Spawner and all themed spawners (themed spawners continue it in their biome theme).
  The Unstable Chunk Spawner always picks a new random chunk.
- The World Mender does the same based on the chunk it is placed in: placed in such a chunk, every chunk it fills
  continues that terrain.
- The World Scanner previews unspawned chunks the same way: placed in such a chunk, it scans the terrain the
  continuation would produce.

### Chunk spawning effects

- While a chunk builds up layer by layer, block break particles crumble along the seam where it meets the neighbouring
  chunks: on the outline edges of the neighbouring blocks that face the new chunk, using those blocks' own particles.

### World types

- Chunk By Chunk is now chosen per world with two world types in the World Type list on the Create World screen:
  **Chunk By Chunk** (empty chunks are void) and **Chunk By Chunk Sealed** (empty chunks are bedrock). Other world
  types are plain vanilla worlds, with no chunk restrictions.
- The `enabled` and `seal_world` config options have been removed, as the world type now decides both. Old config
  files that still contain them only log a warning.
- On a dedicated server, set `level-type=chunkbychunk:chunkbychunk` (or `chunkbychunk:chunkbychunk_sealed`) in
  `server.properties` before the world is first created.
- Worlds from earlier versions of the mod, which were not created with these world types, load as normal worlds.

### World options

- The Create World screen has a Chunk By Chunk settings button (bottom left). Settings chosen there apply only to the
  world being created and are saved in its `serverconfig/chunkbychunk.toml`. The default settings used by other new
  worlds are unchanged, and can still be edited from the mod list.

### Translations

- Added a full Ukrainian translation (`uk_ua`).

### Known issues

- The textures for the new spawners are placeholders, made by recolouring the existing spawner textures.
- Worlds from 1.18.2/1.20.4 versions of the mod have not been tested with this port. Back up worlds before upgrading.

## Developing

This project requires [JDK 25](https://adoptium.net/) & uses the [MultiLoader Template](https://github.com/jaredlll08/MultiLoader-Template) provided by [jaredlll08](https://github.com/jaredlll08).

- `./gradlew build` builds both loader jars (`fabric/build/libs`, `neoforge/build/libs`).
- `./gradlew :fabric:runClient` / `:neoforge:runClient` launch a development client.

## Contributing

This branch targets Minecraft 26.3. The original project's 1.18.2 - 1.20.4 branches are maintained upstream at
[immortius/chunkbychunk](https://github.com/immortius/chunkbychunk).

## Credits

Chunk By Chunk was developed by dividesBy0/immortius.
