# Starting books

The included MTS configuration disables its first-join manuals with `giveManualsOnJoin = false` in `references/configs/config/mtsconfig.json`.

For other mods, the addon reads `config/myinfinitecreation-starting-books.json` on server startup. If it does not exist, the addon creates it with these rules:

```json
{
  "window_ticks": 200,
  "books": [
    { "item": "theoneprobe:probenote" },
    { "item": "prehistoricfauna:ancient_journal" },
    { "item": "tconstruct:materials_and_you" },
    { "item": "ars_nouveau:worn_notebook" },
    { "item": "patchouli:guide_book", "patchouli_book": "apotheosis:apoth_chronicle" },
    { "item": "patchouli:guide_book", "patchouli_book": "divinerpg:divine" }
  ]
}
```

`item` is the item registry ID. `patchouli_book` is optional and matches the `patchouli:book` NBT value, so other Patchouli books remain. `window_ticks` controls how long after a new player's first login to check their inventory (200 ticks is about ten seconds). The addon removes matching items only for players with zero recorded play time who have not been checked before. It never removes them on later logins, and it does not prevent crafting, quest rewards, or finding the same book later.

Restart the server after editing the file. A mod that drops books directly into the world instead of placing them in the player's inventory is outside this filter.
