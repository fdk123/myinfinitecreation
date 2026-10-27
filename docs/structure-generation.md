# Structure generation rules

On server startup, the mod creates `config/myinfinitecreation-structures.json` if it does not exist. Set a chance from `0` to `1` for each structure registry ID:

```json
{
  "structures": {
    "examplemod:rare_tower": 0.25,
    "prehistoricfauna:time_temple": 0,
    "prehistoricfauna:geologist_camp": 0.25
  }
}
```

`0` prevents new starts, `1` preserves normal generation, and `0.25` permits about one quarter of the normal attempts. Omitted structures use normal generation. The rule applies to the individual structure, even if it shares a structure set with others. It does not change biomes or the spacing of candidate positions. Use `/reload` or restart the server after editing the file.

IDs must be **structure IDs** (from `data/<namespace>/worldgen/structure/<path>.json` in the relevant mod), rather than structure set IDs. For example, `data/examplemod/worldgen/structure/rare_tower.json` corresponds to `examplemod:rare_tower`.

Rules affect only chunks whose structure starts have not yet been generated. Existing structures remain. Structures that a mod creates outside Minecraft's standard `ChunkGenerator` structure path are outside this mechanism.
