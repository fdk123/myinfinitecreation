# My Infinite Creation

Forge 1.20.1 progression addon for the My Infinite Creation modpack.

The first goal is to move fragile CraftTweaker recipe removals into a small addon-side recipe policy layer. Rules live in datapack JSON under:

`data/<namespace>/recipe_policies/*.json`

Current rule matchers:

- `id`: exact recipe id, for example `minecraft:wooden_pickaxe`
- `namespace`: recipe id namespace/mod id, for example `immersiveengineering`
- `type`: recipe type id, for example `minecraft:crafting` or `immersiveengineering:blast_furnace`
- `output`: recipe result item id, for example `minecraft:wooden_pickaxe`
- `output_tag`: recipe result item tag, for example `forge:ingots/steel`
- `input` / `inputs`: recipe ingredient item id; useful for CraftTweaker-style `removeByInput`
- `input_tag` / `input_tags`: recipe ingredient item tag
- `except_id` / `except_ids`: recipe ids that must survive an otherwise matching rule
- `except_type` / `except_types`: recipe types that must survive an otherwise matching rule
- `except_output` / `except_outputs`: result items that must survive an otherwise matching rule

Multiple values inside one matcher are OR. Multiple matchers in one rule are AND. For example, a rule with both `type` and `output` only removes recipes matching both fields.
Exclusion fields are checked last and win over positive matchers.

Rules are applied after server recipes are loaded and log every removed recipe.

Replacement recipes should use normal datapack recipe JSON under:

`data/<namespace>/recipes/**/*.json`

The safe migration pattern from CraftTweaker is:

1. remove the old recipe by exact `id` in `recipe_policies`
2. add the replacement recipe under `recipes`
3. add a `recipe_gates` rule if that replacement should stay hidden until progression unlocks it

Prefer exact recipe ids for replacements. A broad output-only removal will also remove replacement recipes that produce the same item.

Progression gates are separate from hard removals. Rules live in datapack JSON under:

`data/<namespace>/recipe_gates/*.json`

Recipe gates do not remove recipes from the server. They block players from taking gated crafting results until their TeamStages/GameStages stage is present or a required MineColonies research is complete. Without those mods, `/mic stage set stage_2` can be used as a simple dev fallback for rules requiring stage `2`.

When JEI is present, untyped gated output items are hidden from the JEI ingredient list until they unlock. Vanilla crafting recipes and anvil recipes that produce or use untyped gated items are hidden too. Type-specific gates hide matching recipes for that recipe type without hiding the item globally, so `minecraft:crafting` can be gated while `create:mechanical_crafting` remains visible. The dev fallback stage is synced to the client when players join and whenever `/mic stage set ...` is used.

Current gate matchers:

- `required_stage` / `required_stages`: stage name needed to unlock the result
- `required_research` / `required_researches`: MineColonies research id needed to unlock the result, for example `myinfinitecreation:technology/iron_tooling`
- `type` / `types`: optional recipe type id, for example `minecraft:crafting` or `create:mechanical_crafting`
- `output` / `outputs`: result item id
- `output_tag` / `output_tags`: result item tag

If `type` is omitted, the gate applies to the output item globally. If `type` is present, the gate applies only to recipes of that type.

Mod gates are broader progression restrictions for whole mods or item/block groups. Rules live under:

`data/<namespace>/mod_gates/*.json`

They do not delete items. Locked items can still be picked up, but they can be hidden from JEI, shown as an unknown item in tooltips, and blocked from use/place/break interactions until the required stage or MineColonies research is unlocked.

Rules use `mode: "restrict"` by default. A `mode: "allow"` rule can open selected items or blocks earlier than a broad mod restriction. An allow rule without `required_stage` or `required_research` is always active, which is useful for split namespaces such as Thermal. The cascade is:

1. unlocked `allow` rules make matching entries available
2. locked `restrict` rules hide/block matching entries
3. unlocked `restrict` rules stop blocking their entries

This lets a few early components from a mod be available while the rest of that mod remains unknown.

Example:

```json
{
  "replace": false,
  "rules": [
    {
      "name": "Early Create components",
      "mode": "allow",
      "required_stage": "1",
      "items": [
        "create:andesite_alloy",
        "create:shaft",
        "create:cogwheel"
      ],
      "blocks": [
        "create:andesite_casing"
      ]
    },
    {
      "name": "Create entry",
      "mode": "restrict",
      "modid": "create",
      "required_research": "myinfinitecreation:technology/create_entry",
      "hide_in_jei": true,
      "mask_name": true,
      "allow_pickup": true,
      "prevent_use": true,
      "prevent_place": true,
      "prevent_break_blocks": true
    }
  ]
}
```

Current mod gate matchers:

- `modid` / `modids`: item or block namespace, for example `create`
- `item` / `items`: exact item ids
- `item_pattern` / `item_patterns`: item id glob patterns, for example `thermal:rubberwood_*`
- `item_tag` / `item_tags`: item tags
- `block` / `blocks`: exact block ids
- `block_pattern` / `block_patterns`: block id glob patterns, for example `prehistoricfauna:agathoxylon_*`
- `block_tag` / `block_tags`: block tags, useful for ores
- `except_item` / `except_items`: item exceptions
- `except_block` / `except_blocks`: block exceptions

Example:

```json
{
  "replace": false,
  "rules": [
    {
      "name": "Iron pickaxe requires stage 2",
      "required_stage": "2",
      "type": "minecraft:crafting",
      "output": "minecraft:iron_pickaxe"
    }
  ]
}
```

MineColonies researches can be added through datapack JSON under:

`data/<namespace>/researches/<branch>/<research>.json`

Example:

```json
{
  "branch": "minecolonies:technology",
  "costs": [
    {
      "type": "minecolonies:item_simple",
      "item": "minecraft:paper",
      "quantity": 8
    }
  ],
  "icon": "minecraft:iron_pickaxe",
  "researchLevel": 1,
  "subtitle": "com.myinfinitecreation.research.technology.iron_tooling.subtitle"
}
```

Recipe gate example using that research:

```json
{
  "replace": false,
  "rules": [
    {
      "name": "Iron pickaxe requires MineColonies Iron Tooling",
      "required_research": "myinfinitecreation:technology/iron_tooling",
      "type": "minecraft:crafting",
      "output": "minecraft:iron_pickaxe"
    }
  ]
}
```

MineColonies researches can optionally unlock TeamStages stages through datapack JSON under:

`data/<namespace>/research_stage_unlocks/*.json`

Use this only for public milestones that other systems should see, such as FTB Quests chapters or broad mod unlocks. Researches do not need a stage if they are only used directly by `recipe_gates` or `mod_gates`.

Example:

```json
{
  "replace": false,
  "unlocks": [
    {
      "name": "Iron Tooling research stage bridge",
      "research": "myinfinitecreation:technology/iron_tooling",
      "stage": "mic_research_iron_tooling"
    }
  ]
}
```

Migrated recipe examples currently live under:

`data/myinfinitecreation/recipes/crafting`
`data/myinfinitecreation/recipes/create/mechanical_crafting`

The first batch covers simple CraftTweaker `addShaped` / `addShapeless` recipes and Create mechanical crafting replacements.

For CraftTweaker `.reuse()` style shaped recipes, use:

`type: "myinfinitecreation:shaped_with_remainders"`

This format behaves like vanilla shaped crafting, with extra `key` fields:

```json
{
  "type": "myinfinitecreation:shaped_with_remainders",
  "pattern": ["ABC"],
  "key": {
    "A": {
      "item": "minecraft:dragon_head",
      "reuse": true
    },
    "B": {
      "item": "minecraft:beacon",
      "remainder": "minecraft:beacon"
    }
  },
  "result": {
    "item": "minecolonies:supplycampdeployer"
  }
}
```

Vanilla item remainders still work too, so items like `minecraft:water_bucket` keep returning their normal bucket unless an explicit remainder overrides that slot.

Recipe policies can be grouped by world progression stage:

`data/<namespace>/recipe_policies/global/*.json`
`data/<namespace>/recipe_policies/stage_1/*.json`
`data/<namespace>/recipe_policies/stage_2/*.json`

`global` policies are always active. Only the current world stage folder is active alongside `global`.

Commands:

- `/mic stage get`
- `/mic stage set stage_2`
- `/mic recipes reapply`

Example:

```json
{
  "replace": false,
  "rules": [
    {
      "name": "Remove vanilla wooden tools",
      "output": [
        "minecraft:wooden_pickaxe",
        "minecraft:wooden_axe"
      ]
    }
  ]
}
```

Next planned layers:

- more MineColonies research definitions
- loot gates
- FTB Quests bridge
- broader machine recipe enforcement for non-player-context blocks
