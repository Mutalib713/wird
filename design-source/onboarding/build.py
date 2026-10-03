"""
Builds onboarding-flow.html from flow.src.html.

Two jobs, both so that nothing on the board is typed from memory:
  1. Colour tokens. Every tint is a mix of two pinned colours (PROFILE 6f), computed here,
     so the board and IMPLEMENT.md carry the recipe, not a hand-picked hex.
  2. Qur'an text. {{v 1:2}} becomes the verse from app/src/main/assets/arabic plus an
     end-of-ayah mark; {{t 1:2}} the verse alone; {{en 112:1}} Saheeh International from
     assets/translations/20. The app's own files are the only source.
"""
import io
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
# The app's own text. Run from design-source/onboarding/ in the repo, or set WIRD_ASSETS.
ASSETS = os.environ.get("WIRD_ASSETS", os.path.join(HERE, "..", "..", "app", "src", "main", "assets"))

PINNED = {
    "cream": "#F7F5ED", "green": "#245847", "deep": "#17382D",
    "ink2": "#4B5551", "gold": "#C9A24B", "goldtext": "#8E6900",
}


def rgb(h):
    h = h.lstrip("#")
    return [int(h[i:i + 2], 16) for i in (0, 2, 4)]


def hexof(c):
    return "#%02X%02X%02X" % tuple(round(v) for v in c)


def mix(a, b, f):
    A, B = rgb(a), rgb(b)
    return hexof([A[i] * (1 - f) + B[i] * f for i in range(3)])


# name -> (hex, recipe, where it is used)
TOKENS = {}
for k, v in PINNED.items():
    TOKENS[k] = (v, "pinned", "")
P = PINNED
DERIVED = [
    ("sky-top", "green", "cream", .20, "top of the sky, behind the white wordmark"),
    ("sky-upper", "green", "cream", .44, "sky, a third of the way down"),
    ("sky-glow", "cream", "gold", .18, "the dawn glow behind the logo and headlines"),
    ("sky-bottom", "cream", "green", .31, "mist at the bottom of the splash"),
    ("mosque", "cream", "green", .75, "mosque and minaret silhouette"),
    ("tile", "cream", "gold", .05, "inner tiles: verses, tracks, summaries"),
    ("chip", "cream", "green", .12, "chips, selected fills, the tonal button"),
    ("bubble-me", "cream", "green", .18, "the reader's own chat bubbles"),
    ("rule", "cream", "ink2", .12, "hairlines and unselected outlines (an edge, never text)"),
    ("dot-off", "cream", "ink2", .28, "inactive pager dots, the sheet grip"),
    ("soon-edge", "cream", "gold", .45, "dashed edge of the Coming soon box"),
]
for name, a, b, f, use in DERIVED:
    TOKENS[name] = (mix(P[a], P[b], f), f"{a} + {round(f * 100)}% {b}", use)
TOKENS["sky-mid"] = (mix(TOKENS["sky-glow"][0], TOKENS["sky-bottom"][0], .5),
                     "sky-glow + 50% sky-bottom", "where the glow meets the mist")


def load(folder):
    out = {}
    d = os.path.join(ASSETS, folder)
    for fn in os.listdir(d):
        if fn.endswith(".json"):
            for row in json.load(io.open(os.path.join(d, fn), encoding="utf-8")):
                out[row["v"]] = row["t"]
    return out


AR = load("arabic")
EN = load(os.path.join("translations", "20"))
DIGITS = "٠١٢٣٤٥٦٧٨٩"


def ayah_digits(key):
    """The ayah number in Arabic-Indic digits, for the ring drawn after each verse."""
    return "".join(DIGITS[int(c)] for c in key.split(":")[1])


def fill(m):
    kind, key = m.group(1), m.group(2)
    if kind == "en":
        return EN[key]
    text = AR[key]
    if kind == "v":
        return f'{text}<span class="mk">{ayah_digits(key)}</span>'
    return text


def css_tokens():
    lines = [f"--{k}:{v[0]};" for k, v in TOKENS.items()]
    return " ".join(lines)


def swatches():
    rows = []
    order = ["cream", "deep", "ink2", "green", "gold", "goldtext"] + [d[0] for d in DERIVED] + ["sky-mid"]
    uses = {
        "cream": "the ground under everything", "deep": "headlines and body ink",
        "ink2": "secondary text (7.5:1 on white)", "green": "buttons, selection, progress",
        "gold": "ornament only: never text on a light ground", "goldtext": "gold text on light grounds (5.1:1)",
    }
    for k in order:
        hx, recipe, use = TOKENS[k]
        use = use or uses.get(k, "")
        rows.append(
            f'<div class="sw"><i style="background:{hx}"></i><div><b>{k}</b> <code>{hx}</code>'
            f'<span>{recipe}</span><em>{use}</em></div></div>'
        )
    return "\n".join(rows)


src = io.open(os.path.join(HERE, "flow.src.html"), encoding="utf-8").read()
out = src.replace("{{TOKENS}}", css_tokens()).replace("{{SWATCHES}}", swatches())
out = re.sub(r"\{\{(v|t|en) (\d{1,3}:\d{1,3})\}\}", fill, out)
# Keep transliterated names whole: "Al-Fātiḥah" must not break after "Al-".
head, body = out.split("<body>", 1)
body = re.sub(r"\b(Al|An|Ar|As|At|Ad|Yā|Āl)-(?=[A-ZĀ])", lambda m: m.group(1) + "\u2011", body)
out = head + "<body>" + body
left = re.findall(r"\{\{[^}]*\}\}", out)
if left:
    sys.exit(f"unfilled placeholders: {left}")
io.open(os.path.join(HERE, "onboarding-flow.html"), "w", encoding="utf-8").write(out)
json.dump({k: {"hex": v[0], "recipe": v[1], "use": v[2]} for k, v in TOKENS.items()},
          io.open(os.path.join(HERE, "tokens.json"), "w", encoding="utf-8"), indent=2)
print("built onboarding-flow.html;", len(TOKENS), "tokens")
