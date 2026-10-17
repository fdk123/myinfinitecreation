"""Check the cross-file invariants of the DivineRPG integrations (stdlib only)."""

import json
from pathlib import Path
import re
import struct


RESOURCES = Path(__file__).resolve().parents[1] / "src/main/resources"
DATA = RESOURCES / "data/myinfinitecreation"
ASSETS = RESOURCES / "assets/myinfinitecreation"
MATERIALS = ("arlemite", "realmite", "rupee", "torridite")
FORMS = ("{material}_dust", "dirty_dust_{material}", "clump_{material}", "shard_{material}")


def read_json(path):
    return json.loads(path.read_text())


def main():
    definitions = {path.stem for path in (DATA / "tinkering/materials/definition").glob("*.json")}
    for folder in (DATA / "tinkering/materials/stats", DATA / "tinkering/materials/traits",
                   ASSETS / "tinkering/materials"):
        assert definitions == {path.stem for path in folder.glob("*.json")}, folder

    modifier_ids = {path.stem for path in (DATA / "tinkering/modifiers").glob("*.json")}
    modifier_ids.update({"twilight", "divine_intervention", "magical", "corrupting"})
    for path in (DATA / "tinkering/materials/traits").glob("*.json"):
        for trait in read_json(path)["default"]:
            if trait["name"].startswith("myinfinitecreation:"):
                assert trait["name"].split(":", 1)[1] in modifier_ids, path

    for path in (DATA / "recipes/divinerpg/tconstruct/materials").rglob("*.json"):
        assert read_json(path)["material"] == f"myinfinitecreation:{path.parent.name}", path

    fluid_java = (RESOURCES.parent / "java/fdk123/myinfinitecreation/divinerpg/DivineMoltenFluids.java").read_text()
    fluid_names = set(re.findall(r'add\("(molten_[a-z_]+)"', fluid_java))
    assert len(fluid_names) == 41
    english = read_json(ASSETS / "lang/en_us.json")
    russian = read_json(ASSETS / "lang/ru_ru.json")
    for name in fluid_names:
        assert (ASSETS / f"models/item/{name}_bucket.json").exists(), name
        assert (ASSETS / f"blockstates/{name}.json").exists(), name
        assert (ASSETS / f"textures/item/divinerpg_molten/{name}_bucket.png").exists(), name
        for language in (english, russian):
            assert f"fluid.myinfinitecreation.{name}" in language, name
            assert f"block.myinfinitecreation.{name}" in language, name
            assert f"item.myinfinitecreation.{name}_bucket" in language, name
        tag = read_json(RESOURCES / f"data/forge/tags/fluids/{name}.json")
        assert tag["values"] == [f"myinfinitecreation:{name}", f"myinfinitecreation:flowing_{name}"], name
    for texture in ("molten_still", "molten_flowing"):
        header = (ASSETS / f"textures/block/{texture}.png").read_bytes()[:24]
        assert header[:8] == b"\x89PNG\r\n\x1a\n" and struct.unpack(">II", header[16:24]) == (16, 16)
    smeltery = DATA / "recipes/divinerpg/tconstruct/smeltery"
    for path in smeltery.rglob("*.json"):
        recipe = read_json(path)
        assert recipe["type"].startswith("tconstruct:")
        assert any(c.get("modid") == "tconstruct" for c in recipe["conditions"]), path
        for fluid in re.findall(r'"myinfinitecreation:(molten_[a-z_]+)"', path.read_text()):
            assert fluid in fluid_names, path
    modifiers_java = (RESOURCES.parent / "java/fdk123/myinfinitecreation/divinerpg/DivineCompatModifiers.java").read_text()
    for name in ("twilight", "divine_intervention", "magical", "corrupting"):
        assert f'MODIFIERS.register("{name}"' in modifiers_java, name

    custom_items = {template.format(material=material) for material in MATERIALS for template in FORMS}
    for name in custom_items:
        assert f"item.myinfinitecreation.{name}" in english, name
        assert f"item.myinfinitecreation.{name}" in russian, name
        assert (ASSETS / f"models/item/{name}.json").exists(), name
        texture = ASSETS / f"textures/item/divinerpg_processing/{name}.png"
        header = texture.read_bytes()[:24]
        assert header[:8] == b"\x89PNG\r\n\x1a\n" and struct.unpack(">II", header[16:24]) == (16, 16), texture

    recipe_count = 0
    for path in (DATA / "recipes/divinerpg").rglob("*.json"):
        data = read_json(path)
        assert "type" in data, path
        content = path.read_text()
        assert "drpgpc:" not in content and "divinerpg_extras:" not in content, path
        for name in re.findall(r"myinfinitecreation:([a-z_]+)", content):
            if name in custom_items:
                assert f"item.myinfinitecreation.{name}" in english, path
        recipe_count += 1
    print(f"OK: {len(definitions)} Tinkers materials, {len(fluid_names)} fluids, "
          f"{len(custom_items)} items, {recipe_count} recipes")


if __name__ == "__main__":
    main()
