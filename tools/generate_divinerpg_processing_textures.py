"""Generate the small, original pixel icons for DivineRPG processing intermediates.

Requires Pillow only when regenerating assets; the mod has no Python dependency.
"""

from pathlib import Path
import json

from PIL import Image, ImageDraw


RESOURCES = Path(__file__).resolve().parents[1] / "src/main/resources/assets/myinfinitecreation"
COLORS = {
    "arlemite": (67, 181, 62),
    "realmite": (200, 137, 46),
    "rupee": (43, 119, 205),
    "torridite": (206, 45, 39),
}
KINDS = {
    "dust": lambda material: f"{material}_dust",
    "dirty_dust": lambda material: f"dirty_dust_{material}",
    "clump": lambda material: f"clump_{material}",
    "shard": lambda material: f"shard_{material}",
}


def shade(rgb, multiplier):
    return tuple(max(0, min(255, round(channel * multiplier))) for channel in rgb) + (255,)


def render(kind, color):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    dark, mid, light = shade(color, .52), shade(color, 1), shade(color, 1.33)
    if kind in {"dust", "dirty_dust"}:
        if kind == "dirty_dust":
            dark, mid, light = (74, 64, 57, 255), shade(color, .67), shade(color, .96)
        for x, y, radius in [(4, 10, 2), (9, 10, 3), (11, 5, 2), (5, 5, 1)]:
            draw.ellipse((x-radius, y-radius, x+radius, y+radius), fill=dark)
            draw.point((x, y-1), fill=light)
            draw.point((x+1, y), fill=mid)
        draw.point((2, 6), fill=mid)
        draw.point((13, 12), fill=light)
    elif kind == "clump":
        draw.polygon([(2, 6), (5, 3), (9, 4), (12, 2), (14, 7), (12, 12), (8, 14), (3, 12)], fill=dark)
        draw.polygon([(4, 6), (7, 4), (9, 5), (12, 4), (12, 8), (8, 10), (4, 9)], fill=mid)
        draw.polygon([(5, 6), (7, 5), (9, 6), (7, 7)], fill=light)
    else:
        draw.polygon([(8, 1), (12, 5), (10, 10), (7, 15), (4, 10), (3, 6)], fill=dark)
        draw.polygon([(8, 2), (11, 5), (8, 11), (6, 13), (5, 6)], fill=mid)
        draw.line([(8, 2), (7, 8), (6, 10)], fill=light, width=1)
    return image


def main():
    texture_dir = RESOURCES / "textures/item/divinerpg_processing"
    model_dir = RESOURCES / "models/item"
    texture_dir.mkdir(parents=True, exist_ok=True)
    model_dir.mkdir(parents=True, exist_ok=True)
    for material, color in COLORS.items():
        for kind, name_factory in KINDS.items():
            name = name_factory(material)
            render(kind, color).save(texture_dir / f"{name}.png")
            model = {"parent": "minecraft:item/generated", "textures": {
                "layer0": f"myinfinitecreation:item/divinerpg_processing/{name}"
            }}
            (model_dir / f"{name}.json").write_text(json.dumps(model, indent=2) + "\n")


if __name__ == "__main__":
    main()
