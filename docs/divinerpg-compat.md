# DivineRPG compatibility in this addon

Target: Minecraft 1.20.1, Forge 47, DivineRPG 1.20, Tinkers' Construct 1.20.1. Keep the official [DivineRPG Compat](https://github.com/DivineRPG/DivineRPG-Compat/tree/1.20) installed alongside this addon.

## Ownership

| Integration | Owner in the modpack |
| --- | --- |
| Integrated Dynamics ore squeezing, Mystical Agriculture, Productive Bees, Ender IO, Industrial Foregoing, Resource Pandas and other official integrations | Official DivineRPG Compat; no copies here |
| Tinkers' Construct DivineRPG materials, molten fluids and selected traits | `myinfinitecreation` data in `tinkering/materials`, `tinkering/modifiers` and `recipes/divinerpg/tconstruct`, with Forge fluid registration in `DivineMoltenFluids` |
| Create ore crushing | `myinfinitecreation` recipes in `recipes/divinerpg/create` |
| Immersive Engineering crusher, arc furnace and alloy smelter | `myinfinitecreation` recipes in `recipes/divinerpg/immersiveengineering` |
| Thermal pulverizer, smelter, recycling and shadow bar recipe | `myinfinitecreation` recipes in `recipes/divinerpg/thermal` |
| Mekanism crushing, enriching, purifying, injecting and combining | `myinfinitecreation` recipes in `recipes/divinerpg/mekanism` |

## Ported scope

The 39 Tinkers' Construct materials use existing DivineRPG items as ingredients. Their head and handle stats were adapted from the 1.16.5 DivineRPG Extras data to the 1.20.1 Tinkers' Construct schema. `harvestLevel` became `mining_tier`; old handle multipliers became offsets. Material colors are generated through Tinkers' native recoloring format. The two legacy materials without suitable DivineRPG 1.20.1 source items, bluefire and awakened halite, are omitted.

Existing Tinkers traits from Extras are restored. All-Seeing Eye, Arthritis, Poison and Multi Hit are reimplemented through Tinkers' 1.20.1 data modules. Twilight, Divine Intervention, Magical and Corrupting are implemented as four static 1.20.1 Java modifiers in `DivineCompatModifiers`; these need Tinkers on the compile/runtime classpath. Similar 1.20.1 traits stand in for Icy, Earthbound and Skybound. Other custom traits in the 1.16.5 source are empty, use incompatible hook signatures, or spawn drops only on the client. They are not treated as working gameplay in this port.

The 41 surviving Extras molten fluids are registered as Forge 1.20.1 fluid types, sources, flowing variants, blocks and buckets. Bluefire stone and awakened halite have no suitable DivineRPG 1.20.1 source items, so their fluids remain absent. The fluid textures and bucket icons are original assets; fluid tiles are tinted with each legacy color. There are 198 smeltery recipes: 49 item melting, 68 casting, 9 alloys and 72 material fluid/melting links. Only old recipes with valid DivineRPG 1.20.1 items and existing fluids were retained. One ingot is 90 mB, one nugget 10 mB and one block 810 mB in the 1.20.1 Tinkers scale; a quarter ingot shadow bar is rounded to 23 mB. Nonmetal quantities such as 1000 mB remain unchanged. The three mob based Tinkers materials (crabs, cyclops and kraken) have no legacy molten fluid and still use their direct item material recipes.

Examples of omitted legacy Java traits: `DreamScapeModifier` has an empty body; `TwilightModifier` and `SkinFryModifier` declare methods that do not match the 1.20.1 Tinkers hooks; `MagicalModifier` dereferences a null Arcana object, and that API is absent; `EdenGardenModifier`, `WoodyModifier` and `InterpolateModifier` create drops only on the client, so they do not create authoritative server drops. These need new, separately specified behavior rather than direct source copying.

The modpack's TCIntegrations 1.20.1 source was also checked; it has no DivineRPG material definitions.

The Processing Compat port registers 16 intermediate items: dust, dirty dust, clump and shard for arlemite, realmite, rupee and torridite. Their original pixel icons were drawn for this addon. There are 151 processing recipes: Create 16, Immersive Engineering 35, Mekanism 60, Thermal 32 and furnace/blast furnace 8. The former direct-to-raw recipes were replaced by this complete dust path. The original Mekanism recipes' `chemicalInput` key was updated to the [1.20.1 `gasInput` syntax](https://github.com/mekanism/Mekanism/wiki/Recipe-Type-JSON-Syntax).

Of the original 218 processing recipes, the other 67 are excluded for specific reasons: 10 Integrated Dynamics recipes already belong to official DivineRPG Compat; 21 Factorium and 23 Plain/Simple/Stone Grinder recipes target mods not listed in this workspace's modpack inventory; 8 ore smelting and 4 Create blasting recipes duplicate existing recipes; one Mekanism recipe targets the nonexistent `divinerpg:torridite_ore_deepslate`. The Factorium-only chunks and powders are not registered.

## Extension points

- Add another material with matching definition, stats and material recipe JSON in `data/myinfinitecreation`, plus a palette in `assets/myinfinitecreation/tinkering/materials` and a `material.myinfinitecreation.<name>` language key.
- Add processing recipes under `data/myinfinitecreation/recipes/divinerpg/<mod>`. Use the `forge:mod_loaded` conditions already present in the recipes.
- The 16 item textures are generated by `tools/generate_divinerpg_processing_textures.py` (Pillow is needed only to regenerate them).
- Run `python3 tools/check_divinerpg_compat.py` after modifying these files.
- Add a molten fluid in `DivineMoltenFluids`, then generate its original bucket art and models with `tools/generate_divinerpg_molten_textures.py`, and add a fluid tag, translations and recipes under `recipes/divinerpg/tconstruct/smeltery`.
- If a specific omitted custom Tinkers effect becomes necessary, implement and balance it against the 1.20.1 API as a separate feature.

## Sources checked

- [DivineRPG Extras](https://github.com/BladeColdsteel/divinerpg_extras), `86465b7` (Minecraft 1.16.5)
- [DivineRPG Processing Compat](https://github.com/LemonDevArchive/DivineRPGProcessingCompat/tree/1.19.2), `97be9b0`
- [Official DivineRPG Compat](https://github.com/DivineRPG/DivineRPG-Compat/tree/1.20), `4f712a8`
- [DivineRPG](https://github.com/DivineRPG/DivineRPG/tree/1.20), `a16f6d7` (item ID check)
- [Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct/tree/1.20.1), `215e687e` (data schema check)

All new JSON was syntax-checked, and DivineRPG item IDs were checked against the 1.20 source registry. IE and Thermal recipe formats were checked against their 1.20.1 sources. An in-game recipe reload and material rendering check are still required with the actual modpack jars. The workspace currently has neither its local `references/mods` jar set nor a cached Gradle 8.6 distribution, so the addon build cannot be run offline here.
