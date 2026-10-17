"""Regenerate original molten fluid and bucket art. Pillow is needed only here."""

import json
import random
import re
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src/main/resources/assets/myinfinitecreation"
JAVA = ROOT / "src/main/java/fdk123/myinfinitecreation/divinerpg/DivineMoltenFluids.java"


def shade(rgb, factor):
    return tuple(max(0, min(255, round(channel * factor))) for channel in rgb) + (255,)


def bucket(rgb):
    image = Image.new("RGBA", (16, 16))
    draw = ImageDraw.Draw(image)
    draw.polygon([(2, 3), (13, 3), (12, 12), (10, 14), (5, 14), (3, 12)], fill=(49, 53, 59))
    draw.polygon([(3, 4), (12, 4), (11, 11), (9, 13), (6, 13), (4, 11)], fill=(133, 144, 153))
    draw.polygon([(4, 6), (11, 6), (10, 11), (9, 12), (6, 12), (5, 11)], fill=shade(rgb, .70))
    draw.polygon([(4, 6), (11, 6), (10, 8), (5, 8)], fill=shade(rgb, 1.15))
    draw.line([(3, 4), (12, 4)], fill=(218, 226, 230))
    draw.line([(4, 5), (4, 9)], fill=(207, 214, 220))
    draw.point((7, 6), fill=shade(rgb, 1.45))
    draw.point((9, 7), fill=shade(rgb, 1.28))
    return image


def fluid_tile(flowing):
    image = Image.new("RGBA", (16, 16))
    pixels = image.load()
    rng = random.Random(72 if flowing else 35)
    for y in range(16):
        for x in range(16):
            wave = (x * 3 + y * 5 + (x * y) % 7 + (8 if flowing else 0)) % 17
            value = max(130, min(245, 178 + wave * 3 + rng.randrange(-15, 16)))
            pixels[x, y] = (value, value, value, 255)
    return image


def main():
    source = JAVA.read_text()
    specs = re.findall(r'add\("(molten_[a-z_]+)", (0x[0-9A-Fa-f]+)\)', source)
    assert len(specs) == 41
    (RESOURCES / "textures/block").mkdir(parents=True, exist_ok=True)
    (RESOURCES / "textures/item/divinerpg_molten").mkdir(parents=True, exist_ok=True)
    (RESOURCES / "models/block").mkdir(parents=True, exist_ok=True)
    for flowing in (False, True):
        name = "molten_flowing" if flowing else "molten_still"
        fluid_tile(flowing).save(RESOURCES / f"textures/block/{name}.png")
    (RESOURCES / "models/block/molten_fluid.json").write_text(json.dumps({
        "textures": {"particle": "myinfinitecreation:block/molten_still"}
    }, indent=2) + "\n")
    for name, argb in specs:
        color = int(argb, 16)
        rgb = ((color >> 16) & 255, (color >> 8) & 255, color & 255)
        bucket(rgb).save(RESOURCES / f"textures/item/divinerpg_molten/{name}_bucket.png")
        (RESOURCES / f"models/item/{name}_bucket.json").write_text(json.dumps({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"myinfinitecreation:item/divinerpg_molten/{name}_bucket"}
        }, indent=2) + "\n")
        (RESOURCES / f"blockstates/{name}.json").write_text(json.dumps({
            "variants": {"": {"model": "myinfinitecreation:block/molten_fluid"}}
        }, indent=2) + "\n")
    print(f"Generated textures and models for {len(specs)} molten fluids")


if __name__ == "__main__":
    main()
