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

Multiple values inside one matcher are OR. Multiple matchers in one rule are AND. For example, a rule with both `type` and `output` only removes recipes matching both fields.

Rules are applied after server recipes are loaded and log every removed recipe.

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
