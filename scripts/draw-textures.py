#!/usr/bin/env python3
"""Draws every Atzen texture as 64x64 pixel art: items, fish buckets and the fish skins of the 3D models.

    python3 scripts/draw-textures.py

Only the standard library is used. Shapes are rasterised per pixel centre without anti-aliasing, shaded with a
light from the upper left and quantised to a short colour ramp per material, then outlined, so the sprites stay
crisp at any GUI scale. Change a colour or a shape here and run the script again; the PNGs are overwritten.
"""
import math
import os
import random
import struct
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "atzen", "textures")
N = 64
LIGHT = (-0.45, -0.7, 0.55)
_l = math.sqrt(sum(c * c for c in LIGHT))
LIGHT = tuple(c / _l for c in LIGHT)


# ---------------------------------------------------------------- png + canvas

def save(path, px):
    h, w = len(px), len(px[0])
    raw = b"".join(b"\x00" + b"".join(bytes(c) for c in row) for row in px)

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def canvas(fill=(0, 0, 0, 0)):
    return [[fill] * N for _ in range(N)]


def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def ramp(base, n=5):
    """Pixel-art ramp: darker tones drift towards blue, lighter ones towards warm yellow."""
    r, g, b = hexc(base) if isinstance(base, str) else base
    out = []
    for i in range(n):
        t = i / (n - 1) * 2 - 1  # -1 dark .. +1 light
        if t < 0:
            k = 1 + t * 0.55
            c = (r * k + 18 * -t, g * k + 10 * -t, b * k + 40 * -t)
        else:
            c = (r + (255 - r) * t * 0.55 + 10 * t, g + (255 - g) * t * 0.5 + 6 * t, b + (255 - b) * t * 0.35)
        out.append(tuple(max(0, min(255, int(v))) for v in c) + (255,))
    return out


def shade(tones, intensity):
    """Map a lighting value 0..1 onto a ramp."""
    i = int(max(0, min(0.999, intensity)) * len(tones))
    return tones[i]


def lit(nx, ny):
    nz = math.sqrt(max(0.0, 1 - nx * nx - ny * ny))
    return max(0.0, nx * LIGHT[0] + ny * LIGHT[1] + nz * LIGHT[2])


def outline(px, strength=0.42, inner=0.18):
    """Dark 1px outline on the shape edge plus a softer second ring, both from the pixel's own colour."""
    def opaque(x, y):
        return 0 <= x < N and 0 <= y < N and px[y][x][3] > 0

    edge = [[False] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            if opaque(x, y) and not all(opaque(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                edge[y][x] = True
    ring = [[False] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            if opaque(x, y) and not edge[y][x] and any(
                    0 <= x + dx < N and 0 <= y + dy < N and edge[y + dy][x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                ring[y][x] = True
    for y in range(N):
        for x in range(N):
            c = px[y][x]
            if edge[y][x]:
                px[y][x] = (int(c[0] * strength) + 8, int(c[1] * strength) + 6, int(c[2] * strength) + 16, 255)
            elif ring[y][x]:
                k = 1 - inner
                px[y][x] = (int(c[0] * k), int(c[1] * k), int(c[2] * k), 255)


def put(px, x, y, c):
    x, y = int(x), int(y)
    if 0 <= x < N and 0 <= y < N:
        px[y][x] = c


def disc(px, cx, cy, r, tones, ring=None):
    for y in range(N):
        for x in range(N):
            dx, dy = (x + 0.5 - cx) / r, (y + 0.5 - cy) / r
            if dx * dx + dy * dy <= 1:
                px[y][x] = shade(tones, lit(dx * 0.9, dy * 0.9) * 1.1)
                if ring and dx * dx + dy * dy > ring:
                    px[y][x] = tones[1]


def line(px, x0, y0, x1, y1, c, width=1.0):
    steps = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    for i in range(steps + 1):
        t = i / steps
        x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        if width <= 1:
            put(px, x, y, c)
        else:
            r = width / 2
            for yy in range(int(y - r) - 1, int(y + r) + 2):
                for xx in range(int(x - r) - 1, int(x + r) + 2):
                    if (xx + 0.5 - x) ** 2 + (yy + 0.5 - y) ** 2 <= r * r:
                        put(px, xx, yy, c)


def polygon(px, pts, colour):
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    for y in range(max(0, int(min(ys))), min(N, int(max(ys)) + 1)):
        for x in range(max(0, int(min(xs))), min(N, int(max(xs)) + 1)):
            if inside(pts, x + 0.5, y + 0.5):
                c = colour(x, y) if callable(colour) else colour
                if c:
                    px[y][x] = c


def inside(pts, x, y):
    res = False
    j = len(pts) - 1
    for i in range(len(pts)):
        xi, yi = pts[i]
        xj, yj = pts[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi + 1e-9) + xi:
            res = not res
        j = i
    return res


# ---------------------------------------------------------------- fish

def F(length, height, back, side, belly, fins, pattern="none", tail="fork", **extra):
    return dict(length=length, height=height, back=back, side=side, belly=belly, fins=fins, pattern=pattern, tail=tail, **extra)


FISH = {
    # salmonids
    "river_trout": F(40, 12, "#5f6b52", "#b9a88a", "#efe2c8", "#8f7a58", "redspots", "square"),
    "rainbow_trout": F(40, 12, "#5a6e5a", "#c8ccc4", "#f2eee6", "#9a8f80", "trout", "square", stripe="#e0839a"),
    "grayling": F(38, 11, "#4a5260", "#9aa2b4", "#e2e2e8", "#8a5a8a", "speckle", sail=True),
    "salmon": F(46, 13, "#3e5a70", "#c9d2da", "#f4f4f2", "#7a8a96", "speckle", "square"),
    "sea_trout": F(44, 13, "#4a5a64", "#c2cad0", "#f2f2ee", "#8a949a", "speckle", "square"),
    # freshwater predators
    "pike": F(48, 10, "#3f5228", "#7f8c3e", "#e6e2b6", "#a07a3a", "spots", snout=1.6),
    "zander": F(44, 10, "#5e6650", "#a8ae98", "#e8e8dc", "#8a8a6a", "bars", spiny=True),
    "perch": F(36, 16, "#4f6a2a", "#b7b54a", "#efe1b0", "#e5672b", "bars", spiny=True),
    "catfish": F(44, 13, "#3c3446", "#7a6f8a", "#d9d2c8", "#5a5068", "none", "round", barbels=True, flathead=True),
    "eel": F(52, 6, "#3e3c26", "#6a6438", "#d8cc98", "#4a4628", "none", "round", eel=True),
    "asp": F(42, 11, "#4a5a66", "#b8c4cc", "#eef2f4", "#9aa0a8"),
    # coarse fish
    "carp": F(36, 19, "#6b4a1c", "#c48d2e", "#efd27a", "#c1542a", "scales"),
    "barbel": F(42, 11, "#6a5a3a", "#b8a070", "#eadcb0", "#b0743a", "none", barbels=True),
    "roach": F(30, 13, "#4a5a5e", "#c2cacc", "#f2f2ee", "#d0503a", "scales"),
    "rudd": F(30, 13, "#6a6a3a", "#d0b858", "#f2e6a8", "#d8402a", "scales"),
    "bream": F(34, 19, "#5a5236", "#a8986a", "#e0d4a8", "#6a5a48", "scales"),
    "tench": F(34, 13, "#3e4a26", "#6a7a3a", "#c8b860", "#4a5228", "none", "round"),
    "crucian_carp": F(32, 18, "#5a4a1e", "#b8962e", "#ecd47a", "#9a6a2a", "scales", "round"),
    "sturgeon": F(52, 9, "#4a5260", "#8a929e", "#dcdcd8", "#5a6270", "plates", snout=1.8, barbels=True),
    "chub": F(38, 12, "#4e5a5e", "#a8b0a8", "#e8e8e0", "#b06a4a", "scales"),
    # cod family
    "atlantic_cod": F(44, 13, "#6a6040", "#b8a878", "#ece4cc", "#8a7a50", "speckle", "square", barbels=True),
    "saithe": F(44, 12, "#34404a", "#6a7884", "#c8d0d6", "#4a5660", "lateral", "square"),
    "alaska_pollock": F(42, 11, "#5a5844", "#a8a488", "#e8e4d4", "#7a7a60", "speckle", "square"),
    "haddock": F(42, 12, "#4a4e58", "#9aa0a8", "#e8eaee", "#5a5e66", "haddock", "square", barbels=True),
    "hake": F(46, 10, "#5e6a74", "#a8b4bc", "#e8ecf0", "#7a8690", "none", "square", snout=1.3),
    "whiting": F(40, 10, "#8a8676", "#c8c4b4", "#f0eee6", "#9a9684", "none", "square"),
    # herring family
    "herring": F(36, 9, "#2e5a7e", "#b8c8d4", "#f2f6f8", "#8aa0b0"),
    "sardine": F(32, 8, "#2a5a86", "#a8bccc", "#eef4f8", "#8a9eb0", "speckle"),
    "anchovy": F(32, 6, "#2e5470", "#8aa6b8", "#e4ecf0", "#7a90a0", "lateral"),
    "sprat": F(28, 7, "#3a6080", "#b4c4d0", "#f0f4f6", "#8a9cac"),
    # perch-like sea fish
    "sea_bass": F(40, 12, "#4a5864", "#a4b0bc", "#eef0f2", "#7a8692", "none", spiny=True),
    "gilthead_bream": F(34, 17, "#5a6878", "#b4bcc4", "#eef0f2", "#8a929a", "lateral"),
    "red_snapper": F(38, 15, "#a83a34", "#e0645a", "#f4c4b8", "#c84a40", "none", spiny=True),
    "red_mullet": F(34, 10, "#b8483a", "#e88a6a", "#f6dcc8", "#e0a040", "none", barbels=True),
    "grouper": F(44, 16, "#5a3e26", "#8a6a46", "#c8aa80", "#6a4a30", "spots", "round", spiny=True),
    "barracuda": F(52, 8, "#4a5a68", "#a8b4c0", "#eef2f4", "#6a7884", "bars", snout=1.4),
    # mackerels and tunas
    "mackerel": F(40, 10, "#1e5a6a", "#7aa8b8", "#eef2f2", "#6a8a96", "mackerel", finlets=True),
    "tuna": F(44, 15, "#1a2a50", "#6a7aa0", "#dce2ec", "#e0c040", "none", finlets=True),
    "skipjack": F(40, 14, "#22345a", "#8a9ab4", "#e8ecf2", "#6a7a94", "belly_stripes", finlets=True),
    "swordfish": F(40, 12, "#2e3a56", "#7a88a4", "#d8dce6", "#4a5674", "none", sword=True),
    # flatfish (seen from above)
    "plaice": F(36, 22, "#7a6a4a", "#8a7a56", "#8a7a56", "#6a5a3c", "orange_spots", flat=True),
    "flounder": F(34, 22, "#6a5e44", "#7a6e50", "#7a6e50", "#5a503a", "speckle", flat=True),
    "sole": F(40, 18, "#7a6648", "#8a7454", "#8a7454", "#5e4c34", "speckle", flat=True),
    "turbot": F(34, 28, "#5e5644", "#6e6650", "#6e6650", "#4e4838", "speckle", flat=True),
    "halibut": F(44, 22, "#4e4a42", "#5e5a50", "#5e5a50", "#3e3a34", "speckle", flat=True),
    # other sea fish
    "redfish": F(36, 14, "#b8402e", "#e06a4a", "#f4b8a0", "#c84a36", "none", spiny=True),
    "monkfish": F(40, 26, "#6a5a44", "#7a6a50", "#7a6a50", "#4e4232", "speckle", "round", flat=True, monk=True),
    "gurnard": F(38, 11, "#b8483a", "#e07a5a", "#f2d0c0", "#4a8aa8", "none", wings=True),
    "wolffish": F(46, 11, "#4a525a", "#7a828a", "#c4c8cc", "#5a6068", "bars", "round", eel=True),
    # the special one
    "golden_koi": F(36, 15, "#f2f0ea", "#fbf8f2", "#ffffff", "#f5c24a", "koi", "veil"),
    # cave water: pale, blind or glowing
    "cave_tetra": F(30, 12, "#c8a8a8", "#e8d0d0", "#f6ecec", "#d89a9a"),
    "olm": F(50, 6, "#e0c0b0", "#f0d8d0", "#faf0ea", "#e8b0a0", "none", "round", eel=True),
    "amethyst_perch": F(36, 16, "#4a2a7a", "#9a6ad0", "#e0c8f4", "#c890f0", "bars", spiny=True),
    "dripstone_eel": F(52, 6, "#5a4a3a", "#8a6a50", "#d0b898", "#6a5440", "speckle", "round", eel=True),
    "glow_carp": F(36, 19, "#1a6a5a", "#3ab8a0", "#a8f0e0", "#5ae0c0", "glow"),
    # lava
    "ember_carp": F(36, 19, "#7a2a10", "#e8682a", "#ffc070", "#ff9a30", "scales"),
    "magma_eel": F(52, 6, "#3a1a10", "#8a2a1a", "#ff7a2a", "#c84a1a", "glow", "round", eel=True),
    "blaze_pike": F(48, 10, "#8a4a10", "#f0a030", "#fff0a0", "#ffd040", "spots", snout=1.6),
    "basalt_catfish": F(44, 13, "#2a2a30", "#4a4a50", "#8a8a90", "#3a3a40", "speckle", "round", barbels=True, flathead=True),
    "obsidian_sturgeon": F(52, 9, "#120a20", "#2a1a40", "#6a4a8a", "#1e1230", "plates", snout=1.8, barbels=True),
    # the End
    "chorus_fish": F(30, 13, "#6a3a6a", "#a878a8", "#e8d0e8", "#c898c8", "scales"),
    "ender_eel": F(52, 6, "#0a1a18", "#1a3a34", "#3ac8a8", "#14302a", "glow", "round", eel=True),
    "void_ray": F(34, 28, "#140e24", "#2a2040", "#2a2040", "#0e0a1a", "speckle", flat=True),
    "shulker_puffer": F(32, 22, "#6a3e6a", "#a87aa8", "#e8d0a8", "#8a5a8a", "spots", "round"),
    # the bosses
    "ancient_catfish": F(48, 14, "#2a2620", "#4e4636", "#a89c84", "#3a3428", "scars", "round", barbels=True, flathead=True),
    "reed_king": F(50, 10, "#26381a", "#5a6e2a", "#d8d4a0", "#8a6a2a", "spots", snout=1.7),
    "black_sturgeon": F(54, 9, "#121418", "#2c3038", "#8a8e94", "#1e2228", "plates", snout=1.9, barbels=True),
    "deep_warden": F(42, 18, "#08262e", "#0e3a44", "#1e5a62", "#0a2a32", "glow", "round", spiny=True, monk=False),
}


def fish_fields(spec, seed):
    """Returns body(u,v)->(inside, nx, ny, part) in fish space: u from 0 (snout) to 1 (tail root), v in -1..1."""
    rnd = random.Random(seed)
    spots = [(rnd.uniform(0.18, 0.95), rnd.uniform(-0.8, 0.6), rnd.uniform(0.05, 0.09)) for _ in range(14)]
    koi = [(rnd.uniform(0.15, 0.9), rnd.uniform(-0.9, 0.4), rnd.uniform(0.12, 0.22)) for _ in range(5)]
    return spots, koi


def body_half_height(spec, u):
    if spec.get("eel"):
        return math.sin(min(1.0, u / 0.12) * math.pi / 2) ** 0.4 * (1 - 0.45 * u)
    hump = 0.55 if spec.get("flathead") else 0.42
    if u < hump:
        t = u / hump
        h = math.sin(t * math.pi / 2) ** (0.55 if spec.get("snout") else 0.45)
    else:
        t = (u - hump) / (1 - hump)
        h = 1 - 0.68 * t ** 1.4
    return h


def draw_fish(px, ox, oy, scale=1.0, angle=0.0, name="carp", seed=1):
    """Draws a fish facing left with its snout near (ox, oy)."""
    spec = FISH[name]
    L, H = spec["length"] * scale, spec["height"] * scale * 1.3
    back, side, belly, fins = ramp(spec["back"]), ramp(spec["side"]), ramp(spec["belly"]), ramp(spec["fins"])
    spots, koi = fish_fields(spec, seed)
    ca, sa = math.cos(angle), math.sin(angle)
    tail_len = L * (0.36 if spec["tail"] == "veil" else 0.24)

    def to_fish(x, y):
        dx, dy = x - ox, y - oy
        return (dx * ca + dy * sa), (-dx * sa + dy * ca)

    for y in range(N):
        for x in range(N):
            fx, fy = to_fish(x + 0.5, y + 0.5)
            u = fx / L
            # tail
            if 0.92 <= u <= 1 + tail_len / L:
                t = (fx - L * 0.92) / (tail_len + L * 0.08)
                spread = H * (0.22 + 0.5 * t) * (1.15 if spec["tail"] == "veil" else 0.9)
                notch = 0
                if spec["tail"] == "fork":
                    notch = H * 0.32 * max(0, t - 0.55) * 2.2
                elif spec["tail"] == "veil":
                    notch = H * 0.35 * max(0, t - 0.5)
                ok = abs(fy) <= spread and not (abs(fy) < notch and t > 0.35)
                if spec["tail"] == "round":
                    ok = (fy / (H * 0.95)) ** 2 + ((t - 0.45) / 0.62) ** 2 <= 1
                if ok:
                    stripe = int((fy / max(spread, 1)) * 3 + 9) % 3 == 0
                    tone = 3 if stripe else 2
                    if spec["tail"] == "veil":
                        tone = 4 if stripe else 3
                    px[y][x] = fins[min(4, tone - (1 if t > 0.85 else 0))]
                    continue
            if not (0 <= u <= 1):
                continue
            hh = body_half_height(spec, u) * H / 2
            if spec.get("snout") and u < 0.15:
                hh *= 0.75
            if abs(fy) > hh:
                continue
            v = fy / hh  # -1 top .. 1 bottom
            nx, ny = (u - 0.45) * 0.9, v * 0.85
            light = lit(nx, ny)
            base = back if v < -0.35 else side if v < 0.35 else belly
            colour = shade(base, light * 1.05 + 0.08)
            pat = spec["pattern"]
            if pat == "bars" and 0.25 < u < 0.92 and v < 0.45:
                if int((u - 0.25) * 11) % 2 == 0:
                    colour = shade(back, light * 0.8)
            elif pat == "spots" and 0.15 < u < 0.95:
                for su, sv, sr in spots:
                    if ((u - su) / sr) ** 2 + ((v - sv) / (sr * 3.2)) ** 2 < 1:
                        colour = shade(belly, light * 0.9 + 0.1)
            elif pat == "trout" and 0.12 < u < 0.97:
                if abs(v - 0.05) < 0.16:
                    colour = shade(ramp(spec["stripe"]), light + 0.05)
                for su, sv, sr in spots:
                    if sv < 0 and ((u - su) / (sr * 0.5)) ** 2 + ((v - sv) / (sr * 1.6)) ** 2 < 1:
                        colour = back[0]
            elif pat == "scales" and 0.2 < u < 0.93:
                cell_u, cell_v = (u - 0.2) * 14, (v + 1) * 3.2
                cu, cv = cell_u % 1, cell_v % 1
                if int(cell_v) % 2:
                    cu = (cu + 0.5) % 1
                if abs(math.hypot(cu - 0.5, cv - 0.2) - 0.55) < 0.1:
                    colour = shade(base, light * 0.75)
            elif pat == "redspots" and 0.12 < u < 0.95:
                for k, (su, sv, sr) in enumerate(spots):
                    if ((u - su) / (sr * 0.55)) ** 2 + ((v - sv) / (sr * 1.6)) ** 2 < 1:
                        colour = (200, 60, 50, 255) if k % 3 == 0 else back[0]
            elif pat == "speckle" and 0.1 < u < 0.97 and v < 0.4:
                if (int(x * 7 + y * 13) % 11) == 0:
                    colour = back[0]
            elif pat == "lateral" and 0.12 < u < 0.97 and abs(v + 0.05) < 0.12:
                colour = shade(belly, light + 0.15)
            elif pat == "haddock" and 0.12 < u < 0.97:
                if abs(v + 0.1 - 0.15 * u) < 0.07:
                    colour = back[0]
                if ((u - 0.33) / 0.05) ** 2 + ((v + 0.25) / 0.16) ** 2 < 1:
                    colour = (30, 28, 34, 255)
            elif pat == "mackerel" and 0.15 < u < 0.95 and v < -0.05:
                if math.sin(u * 60 + math.sin(v * 9) * 2.2) > 0.35:
                    colour = back[0]
            elif pat == "belly_stripes" and 0.15 < u < 0.95 and v > 0.05:
                if int((v + 1) * 9) % 2 == 0:
                    colour = back[1]
            elif pat == "plates" and 0.12 < u < 0.97:
                # the sturgeon's rows of bony scutes
                for row in (-0.55, 0.05, 0.6):
                    if abs(v - row) < 0.13 and (u * 22) % 1 < 0.55:
                        colour = shade(belly, light * 0.7 + 0.25)
            elif pat == "scars" and 0.1 < u < 0.97:
                if abs(math.sin(u * 31 + v * 4)) < 0.06 and v < 0.3:
                    colour = shade(belly, light * 0.6 + 0.2)
                elif (int(x * 5 + y * 11) % 13) == 0:
                    colour = back[0]
            elif pat == "glow" and 0.1 < u < 0.97:
                for su, sv, sr in spots:
                    if ((u - su) / (sr * 0.6)) ** 2 + ((v - sv) / (sr * 1.6)) ** 2 < 1:
                        colour = (90, 240, 230, 255)
            elif pat == "koi":
                for su, sv, sr in koi:
                    if ((u - su) / sr) ** 2 + ((v - sv) / (sr * 2.4)) ** 2 < 1:
                        colour = shade(ramp("#e8571c") if su < 0.5 else ramp("#f0a020"), light + 0.1)
            # gill arc
            gill = 0.24 if not spec.get("snout") else 0.3
            if abs(u - gill - 0.05 * v * v) < 0.018 and abs(v) < 0.75:
                colour = shade(base, light * 0.55)
            px[y][x] = colour

    def at(u, v):
        fx, fy = u * L, v
        return ox + fx * ca - fy * sa, oy + fx * sa + fy * ca

    # dorsal fin
    top = lambda u: -body_half_height(spec, u) * H / 2
    d0, d1 = (0.32, 0.72) if not spec.get("spiny") else (0.28, 0.7)
    rise = 0.55 if spec.get("sail") else 0.26
    if spec.get("sail"):
        d0, d1 = 0.22, 0.66
    pts = [at(d0, top(d0) + 1), at(d0 + 0.08, top(d0) - H * rise), at(d1 - 0.05, top(d1) - H * rise * 0.55), at(d1, top(d1) + 1)]
    polygon(px, pts, lambda x, y: fins[2] if (x + y) % 3 else fins[1])
    if spec.get("spiny"):
        for k in range(5):
            uu = d0 + 0.03 + k * 0.075
            x0, y0 = at(uu, top(uu))
            x1, y1 = at(uu + 0.02, top(uu) - H * 0.32)
            line(px, x0, y0, x1, y1, fins[1])
    if spec.get("finlets"):
        for k in range(5):
            uu = 0.74 + k * 0.04
            for sign in (-1, 1):
                edge = body_half_height(spec, uu) * H / 2 * sign
                polygon(px, [at(uu, edge), at(uu + 0.02, edge + sign * H * 0.08), at(uu + 0.035, edge)], fins[3])
    if spec.get("sword"):
        sx, sy = at(0.0, -H * 0.02)
        tip = at(-0.55, -H * 0.04)
        line(px, sx, sy, tip[0], tip[1], back[2], 2.2)
        line(px, sx, sy + 1, tip[0], tip[1] + 0.5, back[1], 1.2)
    if spec.get("wings"):
        polygon(px, [at(0.22, 0), at(0.42, H * 0.55), at(0.6, H * 0.35), at(0.36, -H * 0.05)],
                lambda x, y: fins[3] if (x - y) % 3 else fins[2])
    # belly fins
    for u0, size in ((0.3, 0.2), (0.62, 0.15)):
        b = body_half_height(spec, u0) * H / 2
        pts = [at(u0, b - 1), at(u0 + 0.05, b + H * size), at(u0 + 0.13, b + H * size * 0.6), at(u0 + 0.12, b - 1)]
        polygon(px, pts, fins[2])
    # pectoral fin on the flank
    pts = [at(0.27, H * 0.05), at(0.38, H * 0.2), at(0.42, H * 0.1)]
    polygon(px, pts, fins[3])
    # eye
    ex, ey = at(0.11 if not spec.get("snout") else 0.13, -H * 0.12)
    # a realistic fish eye: small, a golden iris ring around a large dark pupil
    r = max(1.4, H * 0.085)
    for y in range(int(ey - r) - 1, int(ey + r) + 2):
        for x in range(int(ex - r) - 1, int(ex + r) + 2):
            d = math.hypot(x + 0.5 - ex, y + 0.5 - ey)
            if d <= r:
                put(px, x, y, (196, 170, 96, 255) if d > r * 0.62 else (14, 12, 18, 255))
    put(px, ex - r * 0.35, ey - r * 0.35, (255, 255, 255, 255))
    # mouth
    mx, my = at(0.01, H * 0.1)
    line(px, mx, my, *at(0.06, H * 0.12), back[0])
    if spec.get("barbels"):
        for dv, du in ((0.05, 0.0), (0.18, 0.04)):
            sx, sy = at(0.02 + du, H * dv)
            line(px, sx, sy, sx - 7 * scale, sy + 5 * scale, back[1])
            line(px, sx, sy + 1, sx - 9 * scale, sy + 8 * scale, back[2])


def draw_flatfish(px, ox, oy, scale, angle, name, seed):
    """A flatfish from above: an oval body inside a fringe of fin, a fan tail, both eyes on top near the head."""
    spec = FISH[name]
    L, H = spec["length"] * scale, spec["height"] * scale
    body, fins = ramp(spec["back"]), ramp(spec["fins"])
    ca, sa = math.cos(angle), math.sin(angle)
    rnd = random.Random(seed)
    spots = [(rnd.uniform(0.15, 0.85), rnd.uniform(-0.7, 0.7)) for _ in range(12)]
    for y in range(N):
        for x in range(N):
            dx, dy = x + 0.5 - ox, y + 0.5 - oy
            fx, fy = dx * ca + dy * sa, -dx * sa + dy * ca
            u, v = fx / L, fy / (H / 2)
            if spec.get("monk"):
                head = ((u - 0.3) / 0.32) ** 2 + (v / 1.0) ** 2
                tailbody = 0.3 <= u <= 1.0 and abs(v) <= 0.45 * (1 - (u - 0.3) / 0.9)
                inside_body = head <= 1 or tailbody
                fringe = head <= 1.15 and not inside_body
            else:
                e = ((u - 0.5) / 0.5) ** 2 + v ** 2
                inside_body = e <= 0.78
                fringe = 0.78 < e <= 1.0
            tail = 0.97 <= u <= 1.22 and abs(v) <= 0.18 + (u - 0.97) * 1.6
            if inside_body:
                light = lit((u - 0.45) * 0.8, v * 0.8)
                c = shade(body, light * 1.05 + 0.1)
                pat = spec["pattern"]
                if pat == "orange_spots" and any(((u - su) / 0.045) ** 2 + ((v - sv) / 0.16) ** 2 < 1 for su, sv in spots):
                    c = shade(ramp("#e0782a"), light + 0.15)
                elif pat == "speckle" and (int(x * 7 + y * 13) % 9) == 0:
                    c = body[0]
                px[y][x] = c
            elif fringe or tail:
                px[y][x] = fins[2] if (x + y) % 3 else fins[1]
    # both eyes on the upper side, close to the head
    for eu, ev in ((0.14, -0.28), (0.2, -0.02)):
        ex, ey = ox + eu * L * ca - ev * H / 2 * sa, oy + eu * L * sa + ev * H / 2 * ca
        r = max(1.4, H * 0.06)
        for yy in range(int(ey - r) - 1, int(ey + r) + 2):
            for xx in range(int(ex - r) - 1, int(ex + r) + 2):
                d = math.hypot(xx + 0.5 - ex, yy + 0.5 - ey)
                if d <= r:
                    put(px, xx, yy, (235, 226, 196, 255) if d > r * 0.5 else (20, 18, 24, 255))
    if spec.get("monk"):
        mx, my = ox + 0.02 * L, oy
        line(px, mx, my - H * 0.35, mx, my + H * 0.35, body[0], 1.5)


def draw_any(px, ox, oy, scale, angle, name, seed):
    if FISH[name].get("flat"):
        draw_flatfish(px, ox, oy, scale, angle, name, seed)
    else:
        draw_fish(px, ox, oy, scale, angle, name, seed)


def item_length(spec):
    if spec.get("flat"):
        return spec["length"] * 1.22
    if spec.get("sword"):
        return spec["length"] * 1.8
    return spec["length"] * (1.36 if spec["tail"] == "veil" else 1.24)


def fish_item(name):
    px = canvas()
    spec = FISH[name]
    total = item_length(spec)
    scale = 56 / total
    if spec.get("flat"):
        scale = min(scale, 50 / spec["height"])
    ox = (N - total * scale * 0.98) / 2 + 1
    if spec.get("sword"):
        ox += spec["length"] * 0.55 * scale
    draw_any(px, ox, 37 if not spec.get("flat") else 32, scale, -0.24, name, seed=sum(map(ord, name)))
    outline(px)
    return px


def bucket(name):
    px = canvas()
    metal = ramp("#a9b0b6")
    water = ramp("#2f6fd0")
    # handle behind everything
    for t in range(0, 181, 2):
        a = math.radians(t)
        put(px, 32 + 25 * math.cos(a), 30 - 22 * math.sin(a), metal[1])
        put(px, 32 + 25 * math.cos(a), 31 - 22 * math.sin(a), metal[0])
    # opening: rim ring with water inside
    for y in range(20, 42):
        for x in range(2, 63):
            d = ((x + 0.5 - 32) / 26) ** 2 + ((y + 0.5 - 31) / 8) ** 2
            if d <= 1:
                px[y][x] = metal[3] if d > 0.7 else shade(water, 0.4 + 0.35 * ((x - 8) / 50))
    # the fish lies in the water
    spec = FISH[name]
    total = item_length(spec)
    scale = 40 / total
    ox = 11 + (spec["length"] * 0.55 * scale if spec.get("sword") else 0)
    draw_any(px, ox, 30 if not spec.get("flat") else 28, scale * (0.8 if spec.get("flat") else 1), -0.08, name,
             seed=sum(map(ord, name)))
    # front half of the rim and the body cover the fish's lower half
    for y in range(31, 64):
        for x in range(2, 63):
            d = ((x + 0.5 - 32) / 26) ** 2 + ((y + 0.5 - 31) / 8) ** 2
            if y + 0.5 > 31 and 0.7 < d <= 1:
                px[y][x] = metal[3]
    for y in range(33, 62):
        t = (y - 33) / 29
        half = 25 - 6 * t
        for x in range(int(32 - half), int(32 + half) + 1):
            if ((x + 0.5 - 32) / 26) ** 2 + ((y + 0.5 - 31) / 8) ** 2 <= 1:
                continue
            nx = (x + 0.5 - 32) / half
            c = shade(metal, lit(nx * 0.95, 0.1) + (0.1 if y in (42, 43, 53, 54) else 0))
            if y in (41, 52):
                c = metal[1]
            px[y][x] = c
    outline(px)
    return px


# ---------------------------------------------------------------- rods

RODS = {
    "carbon_rod": dict(shaft="#26282c", wrap="#d8302c", grip="#3a3c42", reel="#b8bec6", tip="#e8e8ea", weave=True),
    "bamboo_rod": dict(shaft="#c8b060", wrap="#6a8a3a", grip="#8a6a3a", reel="#9a7a4a", tip="#e8dca0", nodes=True,
                       cork="#a8844e"),
    "netherite_rod": dict(shaft="#3a3436", wrap="#e8682a", grip="#2a2224", reel="#6a5e60", tip="#ffb070", ember=True,
                          solid_grip=True),
}


def rod(name):
    """A rod as it is held: a cork grip and reel seat at the butt, a blank that tapers to the tip, thread-wrapped
    guides along it, the reel hanging below the seat, and the line running from the reel through the guides to a
    float and hook."""
    spec = RODS[name]
    px = canvas()
    shaft, wrap, grip, reel, tip = (ramp(spec[k]) for k in ("shaft", "wrap", "grip", "reel", "tip"))
    cork = ramp(spec.get("cork", "#c8a070"))
    seat = ramp("#9aa2ac")
    x0, y0, x1, y1 = 6, 59, 56, 7
    length = math.hypot(x1 - x0, y1 - y0)
    dx, dy = (x1 - x0) / length, (y1 - y0) / length
    nx, ny = -dy, dx
    rnd = random.Random(hash(name) & 0xFFFF)

    def at(t, off=0.0):
        return x0 + dx * length * t + nx * off, y0 + dy * length * t + ny * off

    for y in range(N):
        for x in range(N):
            px_, py_ = x + 0.5 - x0, y + 0.5 - y0
            along = px_ * dx + py_ * dy
            across = px_ * nx + py_ * ny
            if not (0 <= along <= length):
                continue
            t = along / length
            if t < 0.2:
                width = 5.0                       # rear grip
            elif t < 0.29:
                width = 3.8                       # reel seat
            elif t < 0.36:
                width = 4.2                       # fore grip
            else:
                width = 3.0 - 2.0 * (t - 0.36) / 0.64
            if abs(across) > width / 2:
                continue
            k = across / (width / 2)
            light = lit(k * 0.9, -k * 0.3)
            if t < 0.2 or 0.29 <= t < 0.36:
                c = shade(grip if spec.get("solid_grip") else cork, light)
                if not spec.get("solid_grip") and rnd.random() < 0.18:
                    c = cork[1]
            elif t < 0.29:
                c = shade(seat, light + 0.15)
                if int(along) % 3 == 0:
                    c = seat[1]
            elif t > 0.95:
                c = shade(tip, light + 0.2)
            else:
                c = shade(shaft, light)
                if spec.get("weave") and (int(along) + int(across * 2)) % 3 == 0:
                    c = shaft[min(4, shaft.index(c) + 1)] if c in shaft else c
                if spec.get("nodes") and int(along) % 9 == 0:
                    c = shaft[1]                  # bamboo knots
                if spec.get("ember") and rnd.random() < 0.05:
                    c = ramp("#ff8a3a")[3]
            px[y][x] = c
    # butt cap and winding checks at the ends of the grips
    bx, by = at(0.0)
    disc(px, bx + dx, by + dy, 2.8, wrap)
    for t in (0.2, 0.36):
        cx, cy = at(t)
        line(px, cx - nx * 2.2, cy - ny * 2.2, cx + nx * 2.2, cy + ny * 2.2, wrap[2], 1.2)
    # the reel hangs under the seat
    sx, sy = at(0.245)
    rx, ry = at(0.245, 7.5)
    line(px, sx, sy, rx, ry, reel[1], 1.6)
    disc(px, rx, ry, 4.8, reel, ring=0.5)
    disc(px, rx, ry, 3.0, ramp("#e8e2d0"))          # line wound on the spool
    disc(px, rx, ry, 1.2, reel)
    line(px, rx, ry, rx + dx * 5 + nx * 2, ry + dy * 5 + ny * 2, reel[0], 1.3)
    put(px, rx + dx * 5.5 + nx * 2, ry + dy * 5.5 + ny * 2, grip[3])
    # guides: small rings standing off the blank, smaller towards the tip
    line_colour = (236, 240, 246, 255)
    points = [(rx + nx * -1.5, ry + ny * -1.5)]
    for t, size in ((0.45, 2.2), (0.58, 1.9), (0.7, 1.6), (0.81, 1.4), (0.9, 1.2)):
        gx, gy = at(t, 2.4)
        cx, cy = at(t)
        line(px, cx, cy, gx, gy, wrap[1], 1.0)
        put(px, cx, cy, wrap[3])
        for a in range(0, 360, 30):
            r = math.radians(a)
            put(px, gx + size * 0.6 * math.cos(r), gy + size * 0.6 * math.sin(r), seat[3])
        points.append((gx, gy))
    tx, ty = at(1.0)
    points.append((tx, ty))
    for (ax, ay), (bx2, by2) in zip(points, points[1:]):
        line(px, ax, ay, bx2, by2, line_colour, 0.6)
    # the line hangs from the tip to a red and white float, the hook below it
    fx, fy = tx + 2, ty + 18
    line(px, tx, ty, fx, fy - 3, line_colour, 0.6)
    disc(px, fx, fy - 1.5, 2.2, ramp("#d8302c"))
    disc(px, fx, fy + 1.5, 2.2, ramp("#f0f0ec"))
    line(px, fx, fy - 4.5, fx, fy - 3.5, ramp("#2a2a2a")[2], 0.8)
    line(px, fx, fy + 3.5, fx, fy + 11, line_colour, 0.6)
    hook = ramp("#b9c0c8")
    hx, hy = fx, fy + 11
    for a in range(0, 200, 12):
        r = math.radians(a)
        put(px, hx - 1.8 + 1.8 * math.cos(r), hy + 1.8 * math.sin(r) + 1, hook[1])
    put(px, hx - 3.6, hy, hook[3])
    outline(px)
    return px


# ---------------------------------------------------------------- reels, lines, carbon fibre

REELS = {"copper_reel": "#c9783a", "iron_reel": "#b8bec6", "diamond_reel": "#4fd8d0"}
LINES = {"fishing_line": "#e8eef2"}


def reel(name):
    """A spinning reel seen from the side, as it hangs under a rod: the foot on top, the stem, the rounded gearbox,
    the spool with wound line and the bail arm in front, the crank with its knob behind."""
    px = canvas()
    metal = ramp(REELS[name])
    dark = ramp("#2e3036")
    wound = ramp("#e8e2d0")
    # foot and stem
    polygon(px, [(20, 6), (46, 6), (44, 10), (22, 10)], lambda x, y: shade(metal, 0.9 - (x - 20) / 60))
    polygon(px, [(31, 10), (37, 10), (36, 22), (32, 22)], lambda x, y: shade(metal, 0.7 - (x - 31) / 20))
    # gearbox body
    disc(px, 36, 34, 12, metal)
    polygon(px, [(36, 22), (48, 30), (46, 44), (36, 46)], lambda x, y: shade(metal, 0.55 - (x - 36) / 40))
    # spool: a short cylinder on the front, line wound on it, a rim at each end
    polygon(px, [(10, 26), (24, 26), (24, 42), (10, 42)], lambda x, y: shade(wound, 0.95 - abs(y - 34) / 14))
    for y in range(27, 42, 2):
        line(px, 11, y, 23, y, wound[2], 0.5)
    polygon(px, [(8, 24), (11, 24), (11, 44), (8, 44)], lambda x, y: shade(metal, 0.85))
    polygon(px, [(23, 23), (26, 23), (26, 45), (23, 45)], lambda x, y: shade(metal, 0.6))
    # the bail arm over the spool
    line(px, 25, 23, 14, 18, dark[3], 1.4)
    line(px, 14, 18, 9, 24, dark[3], 1.4)
    # crank arm and knob on the far side
    disc(px, 40, 34, 3, dark)
    line(px, 40, 34, 52, 48, dark[2], 2.4)
    disc(px, 53, 50, 3.6, ramp("#c8a070"))
    if name == "diamond_reel":
        for sx, sy in ((14, 12), (52, 20), (20, 54)):
            put(px, sx, sy, (210, 255, 250, 255))
    outline(px)
    return px


def glints(px, name):
    """Sparkles for the special reels: echo blue, sea green, boss gold."""
    colour = {"echo_reel": (90, 230, 255, 255), "tide_reel": (160, 255, 230, 255), "reed_reel": (230, 220, 90, 255),
              "deep_reel": (90, 240, 230, 255), "ancient_reel": (240, 200, 90, 255), "current_reel": (150, 200, 255, 255),
              "netherite_reel": (200, 140, 255, 255)}.get(name)
    if not colour:
        return
    for sx, sy in ((10, 14), (54, 12), (8, 46), (56, 40)):
        if px[sy][sx][3] == 0:
            put(px, sx, sy, colour)
            for ddx, ddy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if 0 <= sx + ddx < N and 0 <= sy + ddy < N and px[sy + ddy][sx + ddx][3] == 0:
                    put(px, sx + ddx, sy + ddy, tuple(int(c * 0.6) for c in colour[:3]) + (255,))


def baitcaster(name):
    px = canvas()
    metal, dark = ramp(REELS[name]), ramp("#2a2c32")
    polygon(px, [(8, 30), (56, 30), (58, 44), (6, 44)], lambda x, y: shade(metal, 0.85 - (y - 30) / 22))
    disc(px, 20, 37, 9, dark)
    disc(px, 20, 37, 5, ramp("#e8e2d0"))
    disc(px, 44, 37, 9, metal)
    polygon(px, [(24, 22), (40, 22), (42, 30), (22, 30)], lambda x, y: dark[2])
    line(px, 44, 37, 56, 52, dark[2], 3)
    disc(px, 56, 53, 3.8, dark)
    outline(px)
    return px


def multiplier(name):
    px = canvas()
    metal, dark = ramp(REELS[name]), ramp("#2a2c32")
    for x0 in (10, 46):
        polygon(px, [(x0, 14), (x0 + 8, 14), (x0 + 8, 54), (x0, 54)], lambda x, y: shade(metal, 0.9 - (y - 14) / 50))
    for y in range(20, 49):
        for x in range(18, 46):
            px[y][x] = shade(ramp("#e8e2d0"), 0.4 + 0.5 * math.cos((y - 34) / 16))
            if (x + y) % 5 == 0:
                px[y][x] = ramp("#e8e2d0")[1]
    polygon(px, [(26, 6), (38, 6), (36, 14), (28, 14)], lambda x, y: dark[2])
    line(px, 54, 34, 60, 50, dark[2], 3)
    disc(px, 60, 51, 3.2, dark)
    outline(px)
    glints(px, name)
    return px


def fly_reel(name):
    px = canvas()
    metal = ramp(REELS[name])
    disc(px, 32, 32, 24, metal, ring=0.8)
    for k in range(6):
        a = k * math.pi / 3
        disc(px, 32 + 13 * math.cos(a), 32 + 13 * math.sin(a), 4, ramp("#2a2c32"))
    disc(px, 32, 32, 5, ramp("#c8c0a0"))
    line(px, 44, 44, 50, 50, ramp("#2a2c32")[2], 3)
    outline(px)
    return px


def line_spool(name):
    """A spool of fishing line: wooden flanges, the wound line in its colour and a loose end."""
    px = canvas()
    wood = ramp("#8a5a32")
    colour = ramp(LINES[name])
    for y in range(18, 47):
        for x in range(14, 51):
            nx = (x + 0.5 - 32) / 18
            c = shade(colour, lit(nx * 0.9, 0) + 0.05)
            if (y + (x // 3)) % 4 == 0:
                c = colour[1] if name != "carbon_line" else colour[3]
            px[y][x] = c
    for x0 in (8, 50):
        for y in range(10, 55):
            for x in range(x0, x0 + 6):
                px[y][x] = shade(wood, 0.7 - abs(y - 32) / 60)
    line(px, 50, 30, 58, 50, colour[3], 1.2)
    if name == "carbon_line":
        for sx in range(18, 48, 6):
            put(px, sx, 22, (200, 205, 215, 255))
    outline(px)
    return px


def carbon_fiber():
    """A sheet of woven carbon fibre."""
    px = canvas()
    base = ramp("#2a2c32")
    pts = [(10, 16), (48, 8), (54, 46), (16, 56)]
    polygon(px, pts, lambda x, y: base[3] if ((x // 4) + (y // 4)) % 2 == 0 else base[1])
    for i in range(0, 40, 8):
        line(px, 12 + i, 14 - i * 0.2, 18 + i, 54 - i * 0.2, base[4], 1)
    outline(px)
    return px


# ---------------------------------------------------------------- baits

def worm():
    px = canvas()
    skin = ramp("#d7737a")
    pts = [(9 + i * 0.78, 32 + 13 * math.sin(i / 7.6 * 1.25)) for i in range(60)]
    for i, (cx, cy) in enumerate(pts):
        r = 5.2 if 5 < i < 52 else 5.2 * (min(i, 59 - i) + 2) / 7
        for y in range(int(cy - r) - 1, int(cy + r) + 2):
            for x in range(int(cx - r) - 1, int(cx + r) + 2):
                dx, dy = (x + 0.5 - cx) / r, (y + 0.5 - cy) / r
                if dx * dx + dy * dy <= 1:
                    c = shade(skin, lit(dx * 0.5, dy) + 0.05)
                    if i % 6 == 0:
                        c = skin[1]
                    if 22 < i < 28:
                        c = shade(ramp("#e99aa0"), lit(dx * 0.5, dy) + 0.1)
                    put(px, x, y, c)
    outline(px)
    return px


def glow():
    px = canvas()
    body = ramp("#43c8d8")
    for i in range(7):
        cx, cy, r = 16 + i * 5.2, 40 - math.sin(i / 2.2) * 12, 6.6 - i * 0.35
        disc(px, cx, cy, r, body)
    disc(px, 50, 22, 5.4, ramp("#2a8fb0"))
    outline(px)
    for i in range(7):
        cx, cy = 16 + i * 5.2, 40 - math.sin(i / 2.2) * 12
        put(px, cx - 1, cy - 2, (220, 255, 255, 255))
    put(px, 51, 21, (255, 255, 255, 255))
    rnd = random.Random(3)
    for _ in range(9):
        sx, sy = rnd.randint(6, 58), rnd.randint(6, 58)
        if px[sy][sx][3] == 0:
            put(px, sx, sy, (150, 250, 255, 255))
            for ddx, ddy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if px[sy + ddy][sx + ddx][3] == 0:
                    put(px, sx + ddx, sy + ddy, (70, 190, 220, 255))
    return px


def hearty():
    px = canvas()
    leaf = ramp("#4f9a3a")
    for pts in ([(30, 14), (44, 6), (50, 14), (40, 20)], [(28, 15), (16, 8), (12, 16), (22, 21)]):
        polygon(px, pts, lambda x, y: leaf[3] if (x - y) % 5 else leaf[1])
    line(px, 30, 16, 30, 26, ramp("#6a4a2a")[1], 2)
    for cx, cy, r in ((21, 37, 11), (43, 37, 11), (32, 51, 10.5)):
        disc(px, cx, cy, r, ramp("#c8352c"))
        put(px, cx - r * 0.4, cy - r * 0.45, (255, 220, 210, 255))
        put(px, cx - r * 0.4 + 1, cy - r * 0.45, (255, 170, 150, 255))
    outline(px)
    return px


def blob_bait(colour, count, seed, size=(4.5, 7.0), area=(14, 50)):
    """Small round baits on a heap: maggots, corn, boilies, bread."""
    px = canvas()
    rnd = random.Random(seed)
    for _ in range(count):
        r = rnd.uniform(*size)
        disc(px, rnd.uniform(area[0], area[1]), rnd.uniform(area[0] + 6, area[1] + 4), r, ramp(colour))
    outline(px)
    return px


def maggot():
    px = canvas()
    rnd = random.Random(11)
    for cx, cy, a in ((22, 24, 0.4), (40, 22, 2.6), (24, 44, 2.9), (42, 42, 0.8), (32, 33, 1.6)):
        for i in range(9):
            disc(px, cx + math.cos(a) * (i - 4) * 2.0, cy + math.sin(a) * (i - 4) * 2.0, 4.6 - abs(i - 4) * 0.35, ramp("#efe6c8"))
        put(px, cx + math.cos(a) * 9, cy + math.sin(a) * 9, (40, 30, 30, 255))
    outline(px)
    return px


def bread():
    px = canvas()
    crust, crumb = ramp("#b8762e"), ramp("#f0e2b8")
    polygon(px, [(12, 22), (48, 14), (54, 40), (20, 52)], lambda x, y: shade(crumb, 0.7 - (y - 14) / 90))
    for x in range(12, 56):
        for y in range(10, 56):
            if px[y][x][3] and not inside([(15, 24), (46, 17), (51, 38), (21, 49)], x + 0.5, y + 0.5):
                px[y][x] = crust[2]
    rnd = random.Random(5)
    for _ in range(14):
        put(px, rnd.randint(20, 46), rnd.randint(22, 44), crumb[1])
    outline(px)
    return px


def fish_bait(back, belly, strip=False):
    """A small baitfish, or a strip cut from one."""
    px = canvas()
    if strip:
        polygon(px, [(10, 26), (52, 18), (56, 30), (14, 44)], lambda x, y: shade(ramp(belly if (x + y) % 9 else back), 0.6))
        line(px, 10, 26, 52, 18, ramp("#c03a3a")[3], 2)
    else:
        polygon(px, [(8, 32), (20, 24), (46, 26), (56, 18), (54, 32), (56, 46), (46, 38), (20, 40)],
                lambda x, y: shade(ramp(back if y < 32 else belly), 0.75 - abs(y - 32) / 30))
        put(px, 15, 30, (20, 20, 24, 255))
    outline(px)
    return px


def larva():
    px = canvas()
    body = ramp("#c8a868")
    for i in range(9):
        disc(px, 14 + i * 4.4, 34 + math.sin(i / 1.8) * 6, 5.2 - (i == 8) * 1.5, body)
    for i in range(9):
        put(px, 14 + i * 4.4, 30 + math.sin(i / 1.8) * 6, body[1])
    disc(px, 12, 34, 4, ramp("#4a3020"))
    outline(px)
    return px


def shrimp():
    px = canvas()
    shell = ramp("#e8806a")
    for i in range(8):
        a = i / 7 * math.pi * 0.95
        disc(px, 32 + 16 * math.cos(a + 0.2), 30 + 16 * math.sin(a + 0.2) * 0.9, 6.5 - i * 0.45, shell)
    for k in range(4):
        line(px, 46, 30, 58, 12 + k * 3, shell[1], 1)
    outline(px)
    return px


def lure(kind, colour, accent):
    """Artificial lures: blade, spoon, crankbait, soft bait, popper, jig, flies; with a hook."""
    px = canvas()
    c, a, metal = ramp(colour), ramp(accent), ramp("#c0c4cc")
    if kind == "spinner":
        line(px, 6, 32, 50, 32, metal[2], 2)
        polygon(px, [(10, 32), (26, 12), (40, 18), (30, 38)], lambda x, y: shade(c, 0.95 - (x - 10) / 40))
        disc(px, 40, 32, 6, a)
        disc(px, 47, 33, 4.5, a)
    elif kind == "spoon":
        for y in range(N):
            for x in range(N):
                dx, dy = (x + 0.5 - 30) / 20, (y + 0.5 - 30) / 9
                if dx * dx + dy * dy <= 1:
                    px[y][x] = shade(c, lit(dx, dy) + 0.1) if (x // 6) % 2 else shade(a, lit(dx, dy) + 0.1)
    elif kind == "wobbler":
        polygon(px, [(8, 32), (18, 22), (44, 22), (52, 30), (44, 40), (18, 40)], lambda x, y: shade(c if y < 31 else a, 0.8 - abs(y - 31) / 25))
        polygon(px, [(4, 34), (10, 30), (12, 38)], metal[3])
        put(px, 14, 28, (20, 20, 20, 255))
    elif kind == "soft_lure":
        polygon(px, [(10, 32), (20, 24), (40, 28), (48, 22), (52, 32), (48, 42), (40, 36), (20, 40)], lambda x, y: shade(c, 0.85 - abs(y - 32) / 25))
        disc(px, 14, 32, 6, a)
    elif kind == "popper":
        polygon(px, [(10, 24), (40, 26), (50, 32), (40, 38), (10, 40)], lambda x, y: shade(c if x > 18 else a, 0.85 - abs(y - 32) / 25))
        disc(px, 12, 32, 6, ramp("#2a2c32"))
    elif kind == "jig":
        disc(px, 18, 32, 9, a)
        for k in range(9):
            line(px, 22, 32, 54, 20 + k * 3, c[2 + k % 2], 1.3)
    else:  # flies: streamer, dry fly, nymph
        line(px, 8, 32, 46, 32, metal[2], 2.5)
        if kind == "streamer":
            for y in range(24, 41):
                for x in range(18, 58):
                    if abs(y - 32) < (x - 14) * 0.22 + 1 and (x + y) % 3:
                        px[y][x] = shade(c if abs(y - 32) > 2 else a, 0.75 - abs(y - 32) / 30)
            disc(px, 14, 32, 5, a)
        elif kind == "dry_fly":
            for k in range(10):
                ang = k * math.pi / 5
                line(px, 22, 32, 22 + 11 * math.cos(ang), 32 + 11 * math.sin(ang), a[3], 2)
            polygon(px, [(24, 28), (46, 14), (50, 20), (28, 32)], lambda x, y: shade(c, 0.8))
            polygon(px, [(24, 34), (44, 40), (40, 44), (22, 36)], lambda x, y: shade(c, 0.6))
        else:
            disc(px, 14, 32, 7, ramp("#d8b040"))
            for i in range(6):
                disc(px, 22 + i * 5, 32, 7 - i * 0.6, c)
            for i in range(6):
                put(px, 22 + i * 5, 26 + i * 0.6, a[3])
    # the hook
    line(px, 50, 36, 54, 46, metal[1], 1.5)
    line(px, 54, 46, 48, 50, metal[1], 1.5)
    line(px, 48, 50, 46, 46, metal[1], 1.5)
    outline(px)
    return px


def boss_bait(name):
    px = canvas()
    if name == "ancient_bait":
        disc(px, 32, 34, 18, ramp("#6a3a2a"))
        rnd = random.Random(9)
        for _ in range(8):
            disc(px, rnd.uniform(20, 44), rnd.uniform(22, 46), 3, ramp("#2a8fb0"))
    elif name == "current_bait":
        disc(px, 32, 34, 15, ramp("#4a90a0"))
        for k in range(3):
            line(px, 16, 26 + k * 7, 48, 22 + k * 7, ramp("#c8f0ff")[3], 1)
    else:
        disc(px, 32, 32, 16, ramp("#0e3a44"))
        disc(px, 32, 32, 8, ramp("#5af0e6"))
    outline(px)
    for sx, sy in ((10, 12), (54, 14), (52, 54)):
        put(px, sx, sy, (255, 220, 120, 255))
    return px


def trophy_item(name):
    px = canvas()
    if name == "ancient_barbel":
        for i in range(30):
            disc(px, 10 + i * 1.5, 50 - math.sin(i / 9) * 30, 3.4 - i * 0.07, ramp("#4e4636"))
    elif name == "reed_crown":
        polygon(px, [(10, 46), (14, 22), (22, 36), (32, 14), (42, 36), (50, 22), (54, 46)], lambda x, y: shade(ramp("#5a8a2a"), 0.9 - (y - 14) / 40))
        for cx in (14, 32, 50):
            disc(px, cx, 20 if cx != 32 else 13, 3, ramp("#e8c040"))
    elif name == "sturgeon_plate":
        polygon(px, [(32, 8), (52, 26), (44, 54), (20, 54), (12, 26)], lambda x, y: shade(ramp("#2c3038"), 0.9 - (y - 8) / 50))
        line(px, 32, 12, 32, 50, ramp("#8a8e94")[3], 2)
    else:
        disc(px, 32, 34, 18, ramp("#0e3a44"))
        disc(px, 32, 34, 9, ramp("#5af0e6"))
        polygon(px, [(26, 18), (32, 6), (38, 18)], ramp("#0e3a44")[2])
    outline(px)
    return px


BAITS = {
    "baitfish_bait": lambda: fish_bait("#5a7a8a", "#e8eef2"),
    "reed_king_lure": lambda: lure("wobbler", "#2a5a1a", "#e8c040"),
    "ancient_bait": lambda: boss_bait("ancient_bait"), "current_bait": lambda: boss_bait("current_bait"),
    "deep_light_bait": lambda: boss_bait("deep_light_bait"),
}
SPOOLS = {}


def spare_spool(name):
    """A spare spool seen from the front: the rim, the wound line and the colour of its kind."""
    px = canvas()
    rim = ramp(SPOOLS[name])
    disc(px, 32, 32, 22, rim, ring=0.75)
    deep = 0.55 if name == "deep_spool" else 0.3 if name == "shallow_spool" else 0.42
    disc(px, 32, 32, 22 * (1 - deep * 0.6), ramp("#e8e2d0"))
    disc(px, 32, 32, 6, rim)
    if name in ("fast_gear_spool", "power_gear_spool"):
        teeth = 12 if name == "fast_gear_spool" else 8
        for k in range(teeth):
            a = k * 2 * math.pi / teeth
            disc(px, 32 + 24 * math.cos(a), 32 + 24 * math.sin(a), 2.6, rim)
    outline(px)
    return px


def leader(name):
    """A coiled leader with a swivel: clear blue fluorocarbon or grey steel."""
    px = canvas()
    colour = ramp("#9ad8f0" if name == "fluoro_leader" else "#9aa0a8")
    for k in range(3):
        r = 18 - k * 4
        for i in range(120):
            a = i / 120 * 2 * math.pi
            for d in (-1, 0, 1, 2):
                put(px, 30 + r * math.cos(a), 30 + d + r * 0.8 * math.sin(a), colour[3 - k % 2] if d < 1 else colour[1])
    disc(px, 50, 50, 4, ramp("#c0c4cc"))
    line(px, 46, 46, 40, 40, ramp("#c0c4cc")[2], 2)
    outline(px, 0.6, 0.1)
    return px


HOOKS = {}


def hook_item(name):
    """A fishing hook in its shape: single, treble, barbless, carp (curved), offset (kinked shank), circle."""
    px = canvas()
    scale = HOOKS[name]
    metal = ramp("#3a3e46" if name in ("carp_hook", "circle_hook") else "#8a929c")

    def one(angle, mirror=1):
        cx, cy = 32, 30
        ca, sa = math.cos(angle), math.sin(angle)

        def p(x, y):
            x, y = x * scale * mirror, y * scale
            return cx + x * ca - y * sa, cy + x * sa + y * ca
        # shank, bend, point
        top = -24 if name != "offset_hook" else -18
        line(px, *p(0, top), *p(0, 6), metal[3], 5)
        if name == "offset_hook":
            line(px, *p(0, -18), *p(-5, -22), metal[3], 5)
            line(px, *p(-5, -22), *p(-5, -26), metal[3], 5)
        disc(px, *p(0, top - 3), 3.2 * scale, metal)
        bend = 11 if name != "circle_hook" else 13
        for i in range(30):
            t = i / 29 * math.pi
            line(px, *p(bend - bend * math.cos(t), 6 + bend * math.sin(t)), *p(bend - bend * math.cos(t), 6 + bend * math.sin(t)), metal[3], 5)
        tip_y = -6 if name != "circle_hook" else 2
        tip_x = 2 * bend if name != "circle_hook" else 2 * bend - 6
        line(px, *p(2 * bend, 6), *p(tip_x, tip_y), metal[3], 5)
        if name != "barbless_hook":
            line(px, *p(tip_x, tip_y + 4), *p(tip_x - 4, tip_y + 8), metal[2], 4)
    if name == "treble_hook":
        one(0, 1)
        one(0, -1)
    else:
        one(0.25)
    outline(px, 0.35, 0.1)
    return px


TROPHIES = ["ancient_barbel", "reed_crown", "sturgeon_plate", "warden_heart"]


# ---------------------------------------------------------------- tomes

TOMES = {}


def tome(name):
    spec = TOMES[name]
    px = canvas()
    cover = ramp(spec["cover"])
    pages = ramp("#efe4c4")
    gold = ramp("#e3b53c")
    # page block (right and bottom edge) and the cover in a slight three-quarter view
    polygon(px, [(14, 12), (52, 8), (56, 50), (18, 56)], lambda x, y: pages[2] if y % 3 else pages[1])
    polygon(px, [(8, 10), (48, 6), (52, 48), (12, 54)],
            lambda x, y: shade(cover, 0.75 - (x - 8) / 140 - (y - 6) / 160))
    # spine
    polygon(px, [(8, 10), (14, 9), (18, 53), (12, 54)], lambda x, y: cover[1] if y % 9 > 1 else gold[2])
    # corner caps
    for cx, cy in ((46, 8), (50, 46)):
        polygon(px, [(cx - 4, cy - 2), (cx + 2, cy - 3), (cx + 3, cy + 3), (cx - 3, cy + 4)], gold[3])
    # emblem
    ex, ey = 31, 29
    if spec["emblem"] == "wave":
        for k in range(3):
            for i in range(26):
                x = ex - 12 + i
                y = ey - 6 + k * 6 + 2.4 * math.sin(i / 2.6)
                put(px, x, y, gold[4] if k == 1 else gold[3])
                put(px, x, y + 1, gold[1])
    elif spec["emblem"] == "target":
        for r, k in ((11, 4), (7, 1), (3, 4)):
            for i in range(90):
                a = i / 90 * math.tau
                put(px, ex + r * math.cos(a), ey + r * math.sin(a), gold[k])
        line(px, ex - 15, ey, ex + 15, ey, gold[3])
        line(px, ex, ey - 15, ex, ey + 15, gold[3])
    elif spec["emblem"] == "spiral":
        for i in range(160):
            a = i / 12
            r = 1 + a * 1.25
            put(px, ex + r * math.cos(a), ey + r * math.sin(a), gold[4])
            put(px, ex + r * math.cos(a) + 1, ey + r * math.sin(a) + 1, gold[1])
    else:
        for i in range(120):
            a = i / 120 * math.tau
            x = ex + 10 * math.sin(a)
            y = ey + 7 * math.sin(2 * a)
            put(px, x, y, gold[4])
            put(px, x, y + 1, gold[1])
        line(px, ex - 14, ey + 9, ex + 14, ey - 9, gold[3])
    outline(px)
    return px


# ---------------------------------------------------------------- entity skins

def _geometry():
    """Width, height, body length, nose and tail length of each model, read from FishSpecies.java (one source)."""
    import re
    java = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "java", "io", "github",
                        "jojiuchiha03", "atzen", "fish", "FishSpecies.java")
    found = {}
    for m in re.finditer(r"^\s+([A-Z_]+)\([^,]+,[^,]+,[^,]+,[^,]+,[^,]+,\s*([\d.]+)f,\s*([\d.]+)f,\s*([\d.]+)f,\s*([\d.]+)f,\s*([\d.]+)f",
                         open(java, encoding="utf-8").read(), re.M):
        found[m.group(1).lower()] = tuple(float(m.group(i)) for i in range(2, 7))
    return found


GEOMETRY = _geometry()


def skin(name):
    spec = FISH[name]
    w, h, length, nose, tail = GEOMETRY[name]
    back, side, belly, fins = ramp(spec["back"]), ramp(spec["side"]), ramp(spec["belly"]), ramp(spec["fins"])
    px = canvas(side[2])
    rnd = random.Random(hash(name) & 0xFFFF)

    def flank(u, v):
        """u 0..1 along the fish, v 0 (back) .. 1 (belly)."""
        base = back if v < 0.3 else side if v < 0.72 else belly
        c = base[1 if v < 0.08 else 2 if v < 0.2 or v > 0.9 else 3]
        # the lateral line: a thin darker stripe along the flank
        if 0.44 < v < 0.5 and not spec.get("flat") and int(u * 40) % 2 == 0:
            c = side[1]
        pat = spec["pattern"]
        if pat == "bars" and v < 0.7 and int(u * 10) % 2 == 0:
            c = back[1]
        elif pat == "trout":
            if 0.42 < v < 0.58:
                c = ramp(spec["stripe"])[3]
            elif v < 0.4 and rnd.random() < 0.12:
                c = back[0]
        elif pat == "spots" and 0.15 < v < 0.75 and rnd.random() < 0.16:
            c = belly[3]
        elif pat == "scales" and (int(u * 24) + int(v * 8)) % 2 == 0:
            c = base[2]
        elif pat == "plates" and v < 0.8 and (int(u * 30) % 3 == 0) and int(v * 6) % 2 == 0:
            c = belly[2]
        elif pat == "glow" and rnd.random() < 0.06:
            c = (90, 240, 230, 255)
        elif pat == "scars" and rnd.random() < 0.05:
            c = belly[1]
        elif pat == "koi":
            if math.sin(u * 9.0 + 1.3) * math.cos(v * 5.0) > 0.35:
                c = ramp("#e8571c" if u < 0.5 else "#f0a020")[3]
        return c

    def box(u0, v0, bw, bh, bd, along):
        """Paints a model box's UV net; 'along' maps the box's length axis to the fish's u range."""
        a0, a1 = along
        d = bd
        faces = [
            (u0 + d, v0, bw, d, "top"), (u0 + d + bw, v0, bw, d, "bottom"),
            (u0, v0 + d, d, bh, "side"), (u0 + d, v0 + d, bw, bh, "front"),
            (u0 + d + bw, v0 + d, d, bh, "side_r"), (u0 + d + bw + d, v0 + d, bw, bh, "back"),
        ]
        for fx, fy, fw, fh, kind in faces:
            for y in range(int(fy), int(math.ceil(fy + fh))):
                for x in range(int(fx), int(math.ceil(fx + fw))):
                    if not (0 <= x < N and 0 <= y < N):
                        continue
                    lu = (x + 0.5 - fx) / max(fw, 0.01)
                    lv = (y + 0.5 - fy) / max(fh, 0.01)
                    if kind == "top":
                        c = back[2] if (x + y) % 5 else back[1]
                    elif kind == "bottom":
                        c = belly[3]
                    elif kind == "side":
                        c = flank(a0 + (1 - lu) * (a1 - a0), lv)
                    elif kind == "side_r":
                        c = flank(a0 + lu * (a1 - a0), lv)
                    else:
                        c = flank(a1 if kind == "back" else a0, lv)
                    px[y][x] = c

    total = nose + 3 + length
    # The body: five segments, laid out and shaped exactly as in AtzenFishModel (profile, texture).
    flat = w > h * 2
    if flat:
        fw, fh = (0.8, 1, 1, 0.85, 0.55), (1, 1, 1, 0.9, 0.8)
    elif length > h * 5:
        fw = fh = (1, 1, 0.95, 0.85, 0.7)
    else:
        fw, fh = (1, 0.95, 0.85, 0.7, 0.5), (1, 1, 0.88, 0.7, 0.5)
    depth = length / 5
    column = math.ceil(2 * depth + 2 * w) + 1
    start = (nose + 3) / total
    for k in range(5):
        u0 = (k % 3) * column
        v0 = 0 if k < 3 else 10
        a0 = start + (1 - start) * k / 5
        a1 = start + (1 - start) * (k + 1) / 5
        box(u0, v0, w * fw[k], h * fh[k], depth, (a0, a1))
    box(0, 20, w, h, 3, (nose / total, (nose + 3) / total))
    box(0, 32, w, h * 0.6, nose, (0, nose / total))
    # gill line on the head sides
    for y in range(20 + 3, int(20 + 3 + h)):
        put(px, 0, y, side[1])
        put(px, 3 + w + 2, y, side[1])
    # tail, fins, dorsal: fin colour with rays
    for y in range(49, 64):
        for x in range(0, 54):
            px[y][x] = fins[3] if (x + (y // 2)) % 3 else fins[2]
    # eyes: white ring, black pupil
    for y in range(58, 64):
        for x in range(54, 64):
            px[y][x] = (232, 224, 200, 255) if x < 60 else (16, 14, 20, 255)
    return px


# ---------------------------------------------------------------- main

def main():
    item = os.path.join(ROOT, "item")
    for name in FISH:
        save(os.path.join(item, name + ".png"), fish_item(name))
        save(os.path.join(item, name + "_bucket.png"), bucket(name))
        save(os.path.join(ROOT, "entity", "fish", name + ".png"), skin(name))
    for name in RODS:
        save(os.path.join(item, name + ".png"), rod(name))
    for name in TOMES:
        save(os.path.join(item, name + ".png"), tome(name))
    for name in REELS:
        save(os.path.join(item, name + ".png"), reel(name))
    for name in LINES:
        save(os.path.join(item, name + ".png"), line_spool(name))
    save(os.path.join(item, "carbon_fiber.png"), carbon_fiber())
    save(os.path.join(item, "worm_bait.png"), worm())
    save(os.path.join(item, "glow_bait.png"), glow())
    for name, draw in BAITS.items():
        save(os.path.join(item, name + ".png"), draw())
    for name in TROPHIES:
        save(os.path.join(item, name + ".png"), trophy_item(name))
    for name in SPOOLS:
        save(os.path.join(item, name + ".png"), spare_spool(name))
    for name in HOOKS:
        save(os.path.join(item, name + ".png"), hook_item(name))
    extras()
    print("textures written to", os.path.normpath(ROOT))


# ---------------------------------------------------------------- 0.8–0.14: nets, aids, modules, blocks

def landing_net(name):
    px = canvas()
    size = {"small_landing_net": 0.75, "carp_landing_net": 0.9, "big_game_landing_net": 1.0}[name]
    frame = ramp({"small_landing_net": "#8a5a2a", "carp_landing_net": "#6a6e78", "big_game_landing_net": "#3a3c42"}[name])
    mesh = ramp("#d8dccc" if name != "big_game_landing_net" else "#5a8a6a")
    cx, cy, r = 40, 22, 18 * size
    # the bag: a mesh hanging below the hoop
    for y in range(N):
        for x in range(N):
            dx, dy = (x + 0.5 - cx) / r, (y + 0.5 - cy) / (r * 1.5)
            if dx * dx + dy * dy <= 1 and y >= cy - 2 and ((x + y) % 4 == 0 or (x - y) % 4 == 0):
                px[y][x] = mesh[3] if (x + y) % 8 else mesh[2]
    for i in range(160):
        a = i / 160 * math.tau
        put(px, cx + r * math.cos(a), cy + r * 0.45 * math.sin(a), frame[3])
        put(px, cx + r * math.cos(a), cy + r * 0.45 * math.sin(a) + 1, frame[1])
    line(px, cx - r * 0.9, cy + 2, 6, 58, frame[2], 2.6)
    line(px, cx - r * 0.9, cy + 1, 6, 57, frame[4], 1)
    outline(px)
    return px


def pearl():
    px = canvas()
    shell = ramp("#e8e4f4")
    polygon(px, [(10, 40), (20, 16), (32, 10), (46, 14), (56, 36), (48, 52), (22, 54)],
            lambda x, y: shade(shell, 0.95 - ((x - 32) ** 2 + (y - 30) ** 2) / 900))
    for k, colour in enumerate(("#f4b8d8", "#b8e8f4", "#f4e8a8")):
        for i in range(30):
            a = i / 30 * 2.2 + 0.6
            put(px, 32 + (8 + k * 5) * math.cos(a + 2), 34 + (8 + k * 5) * math.sin(a + 2) * 0.7, hexc(colour) + (255,))
    disc(px, 34, 30, 5, ramp("#ffffff"))
    outline(px)
    return px


def groundbait():
    px = canvas()
    disc(px, 32, 36, 18, ramp("#8a6a3a"))
    rnd = random.Random(7)
    for _ in range(70):
        a, d = rnd.random() * math.tau, rnd.random() * 15
        put(px, 32 + d * math.cos(a), 36 + d * math.sin(a), rnd.choice((ramp("#e8c860")[3], ramp("#5a3a1a")[1], ramp("#c8a060")[4])))
    outline(px)
    return px


def echo_sounder():
    px = canvas()
    body = ramp("#3a3d44")
    polygon(px, [(12, 10), (52, 10), (52, 54), (12, 54)], lambda x, y: shade(body, 0.8 - (y - 10) / 120))
    screen = ramp("#1a4a6a")
    polygon(px, [(17, 15), (47, 15), (47, 41), (17, 41)], lambda x, y: screen[1] if (y % 4) else screen[2])
    for x in range(18, 47):
        y = int(34 + 4 * math.sin(x / 4))
        for yy in range(y, 41):
            put(px, x, yy, ramp("#c8a050")[2])
    for fx, fy in ((24, 22), (36, 27), (41, 20)):
        line(px, fx - 2, fy, fx + 2, fy, ramp("#5af0e6")[4], 2)
    for bx in (20, 32, 44):
        disc(px, bx, 48, 2.6, ramp("#c83a3a" if bx == 20 else "#9aa4ae"))
    outline(px)
    return px


def bait_box():
    px = canvas()
    box = ramp("#3a7a5a")
    polygon(px, [(8, 22), (56, 22), (56, 54), (8, 54)], lambda x, y: shade(box, 0.85 - (y - 22) / 70))
    polygon(px, [(6, 16), (58, 16), (56, 24), (8, 24)], box[3])
    for i, colour in enumerate(("#c83a3a", "#e8c040", "#e8d8b0", "#4a8ac8")):
        x = 12 + i * 11
        polygon(px, [(x, 27), (x + 9, 27), (x + 9, 50), (x, 50)], ramp("#2a5a40")[1])
        disc(px, x + 4.5, 38, 3, ramp(colour))
    polygon(px, [(28, 12), (36, 12), (36, 17), (28, 17)], ramp("#b8bec6")[3])
    outline(px)
    return px


def angler_tool():
    px = canvas()
    steel, grip = ramp("#b8bec6"), ramp("#c83a3a")
    line(px, 14, 50, 34, 30, grip[2], 6)
    line(px, 13, 49, 33, 29, grip[4], 1.5)
    line(px, 34, 30, 52, 12, steel[3], 3.5)
    line(px, 34, 30, 54, 22, steel[2], 3)
    line(px, 34, 30, 44, 10, steel[4], 2)
    disc(px, 34, 30, 3, steel)
    outline(px)
    return px


def lexicon():
    px = canvas()
    cover, pages, gold = ramp("#1e5a8a"), ramp("#efe4c4"), ramp("#e3b53c")
    polygon(px, [(14, 12), (52, 8), (56, 50), (18, 56)], lambda x, y: pages[2] if y % 3 else pages[1])
    polygon(px, [(8, 10), (48, 6), (52, 48), (12, 54)], lambda x, y: shade(cover, 0.75 - (x - 8) / 140 - (y - 6) / 160))
    polygon(px, [(8, 10), (14, 9), (18, 53), (12, 54)], cover[1])
    # a small golden fish on the cover
    polygon(px, [(22, 30), (30, 24), (40, 26), (44, 30), (40, 34), (30, 36)], gold[3])
    polygon(px, [(44, 30), (50, 25), (50, 35)], gold[2])
    put(px, 27, 29, gold[0])
    outline(px)
    return px


def fishing_crate():
    px = canvas()
    wood, dark, iron = ramp("#9a6a35"), ramp("#5b3a1e"), ramp("#8a8e96")
    polygon(px, [(8, 16), (56, 16), (56, 54), (8, 54)], lambda x, y: wood[2 + ((y - 16) // 9 + (x // 13)) % 2])
    for y in (16, 25, 34, 43, 53):
        line(px, 8, y, 55, y, dark[1], 1.2)
    line(px, 10, 52, 54, 18, dark[2], 2.5)
    for cx, cy in ((10, 18), (54, 18), (10, 52), (54, 52)):
        polygon(px, [(cx - 3, cy - 3), (cx + 3, cy - 3), (cx + 3, cy + 3), (cx - 3, cy + 3)], iron[3])
    # wet weed hanging off the lid
    for x0 in (18, 40):
        line(px, x0, 16, x0 + 2, 26, ramp("#4a7a3a")[3], 2)
    outline(px)
    return px


def fishy_note():
    px = canvas()
    paper, ink = ramp("#e8dcb8"), ramp("#3a4a6a")
    polygon(px, [(12, 10), (50, 8), (54, 54), (14, 56)], lambda x, y: shade(paper, 0.9 - ((x + y) % 17) / 60))
    for y in (18, 25, 32):
        line(px, 18, y, 44, y - 1, ink[2], 1)
    # a fish sketch and a cross where it bites
    polygon(px, [(20, 44), (28, 39), (36, 41), (38, 44), (36, 47), (28, 48)], ink[1])
    polygon(px, [(38, 44), (43, 40), (43, 48)], ink[1])
    line(px, 42, 30, 48, 36, ramp("#c83a3a")[3], 2)
    line(px, 48, 30, 42, 36, ramp("#c83a3a")[3], 2)
    outline(px)
    return px


MODULES = {}


def module_item(name):
    colour, symbol = MODULES[name]
    px = canvas()
    plate, accent = ramp("#5a6470"), ramp(colour)
    polygon(px, [(10, 14), (54, 14), (54, 50), (10, 50)], lambda x, y: shade(plate, 0.85 - (y - 14) / 90))
    for sx, sy in ((14, 18), (50, 18), (14, 46), (50, 46)):
        disc(px, sx, sy, 2, ramp("#b8bec6"))
    cx, cy = 32, 32
    if symbol == "dial":
        disc(px, cx, cy, 11, accent, ring=0.7)
        line(px, cx, cy, cx + 7, cy - 6, plate[0], 2)
    elif symbol == "bar":
        polygon(px, [(16, 28), (48, 28), (48, 36), (16, 36)], accent[3])
        line(px, 16, 30, 48, 30, accent[4])
    elif symbol == "bell":
        polygon(px, [(24, 40), (26, 26), (32, 21), (38, 26), (40, 40)], accent[3])
        disc(px, 32, 42, 2.5, accent)
    elif symbol == "ring":
        disc(px, cx, cy, 11, accent)
        disc(px, cx, cy, 6, plate)
    elif symbol == "balls":
        for i in range(6):
            a = i / 6 * math.tau
            disc(px, cx + 8 * math.cos(a), cy + 8 * math.sin(a), 3.2, accent)
    elif symbol == "hook":
        for i in range(40):
            a = i / 40 * math.pi * 1.2
            put(px, cx + 7 * math.cos(a), cy + 6 + 7 * math.sin(a), accent[4])
        line(px, cx + 7, cy + 6, cx + 7, cy - 12, accent[3], 2)
    elif symbol == "wave":
        for i in range(30):
            line(px, 17 + i, cy + 5 * math.sin(i / 3), 18 + i, cy + 5 * math.sin((i + 1) / 3), accent[3], 2)
    elif symbol == "float":
        disc(px, cx, cy + 3, 7, ramp("#f0f0f0"))
        polygon(px, [(cx - 7, cy + 2), (cx + 7, cy + 2), (cx + 5, cy - 4), (cx - 5, cy - 4)], accent[3])
        line(px, cx, cy - 4, cx, cy - 12, accent[4], 2)
    elif symbol == "waves":
        for r in (5, 9, 13):
            for i in range(40):
                a = -0.9 + i / 40 * 1.8
                put(px, 18 + r * math.cos(a), cy + r * math.sin(a), accent[4 if r == 5 else 3])
        disc(px, 18, cy, 2.5, accent)
    elif symbol == "crown":
        polygon(px, [(18, 40), (20, 24), (26, 32), (32, 20), (38, 32), (44, 24), (46, 40)], accent[3])
    else:  # cage
        for x in range(20, 45, 5):
            line(px, x, 22, x, 42, accent[3], 1.5)
        line(px, 20, 22, 44, 22, accent[4], 2)
        line(px, 20, 42, 44, 42, accent[4], 2)
        disc(px, 32, 32, 3.5, ramp("#e8c860"))
    outline(px)
    return px


def tile(base, pattern):
    """A 64px block texture (opaque)."""
    tones = ramp(base)
    px = [[tones[2]] * N for _ in range(N)]
    rnd = random.Random(hash(pattern) & 0xFFFF)
    for y in range(N):
        for x in range(N):
            if pattern == "planks":
                k = 1 if y % 16 in (0, 15) else 3 if (x + rnd.randrange(3)) % 23 == 0 else 2 + (rnd.random() < 0.2)
            elif pattern == "metal":
                k = 3 if (x + y) % 16 == 0 else 1 if y % 32 in (0, 31) or x % 32 in (0, 31) else 2
            else:
                k = 2 + (rnd.random() < 0.15) - (rnd.random() < 0.1)
            px[y][x] = tones[max(0, min(4, k))]
    return px


def net_texture():
    px = canvas()
    cord = ramp("#c8ccb8")
    for y in range(N):
        for x in range(N):
            if (x + y) % 8 in (0, 1) or (x - y) % 8 in (0, 1):
                px[y][x] = cord[3] if (x + y) % 16 < 8 else cord[2]
    return px


def awning(px, colours):
    for y in range(0, 18):
        for x in range(N):
            px[y][x] = ramp(colours[(x // 8) % 2])[3 if y < 14 else 1]


def market_side():
    px = tile("#8a6a3a", "planks")
    awning(px, ("#c83a3a", "#f0f0f0"))
    for i, fish in enumerate(("#9aa6b0", "#be8540", "#566e3a")):
        x = 10 + i * 17
        polygon(px, [(x, 40), (x + 6, 35), (x + 12, 40), (x + 6, 45)], ramp(fish)[3])
        polygon(px, [(x + 12, 40), (x + 15, 37), (x + 15, 43)], ramp(fish)[2])
    return px


def market_top():
    px = tile("#8a6a3a", "planks")
    for y in range(20, 44):
        for x in range(12, 52):
            px[y][x] = ramp("#a8d8e8")[3 if (x + y) % 5 else 4]
    return px


def workshop_side():
    px = tile("#5a4a3a", "planks")
    steel = ramp("#b8bec6")
    line(px, 10, 50, 30, 20, steel[3], 3)
    line(px, 34, 20, 54, 50, ramp("#8a5a2a")[3], 3)
    disc(px, 32, 32, 6, steel)
    return px


def workshop_top():
    px = tile("#6a6e78", "metal")
    line(px, 8, 32, 56, 32, ramp("#8a5a2a")[3], 3)
    disc(px, 44, 32, 6, ramp("#c9783a"))
    return px


def solid(colour, glow=False):
    tones = ramp(colour)
    return [[tones[4 if glow and (x + y) % 3 else 3] for x in range(16)] for y in range(16)]


def extras():
    item = os.path.join(ROOT, "item")
    block = os.path.join(ROOT, "block")
    for name in ("small_landing_net", "carp_landing_net", "big_game_landing_net"):
        save(os.path.join(item, name + ".png"), landing_net(name))
    save(os.path.join(item, "mother_of_pearl.png"), pearl())
    save(os.path.join(item, "groundbait.png"), groundbait())
    save(os.path.join(item, "echo_sounder.png"), echo_sounder())
    save(os.path.join(item, "bait_box.png"), bait_box())
    save(os.path.join(item, "fish_lexicon.png"), lexicon())
    save(os.path.join(item, "fishing_crate.png"), fishing_crate())
    save(os.path.join(item, "fishy_note.png"), fishy_note())
    for name in MODULES:
        save(os.path.join(item, name + "_module.png"), module_item(name))
    save(os.path.join(block, "net.png"), net_texture())
    save(os.path.join(block, "holder_wood.png"), tile("#8a6a3a", "planks"))
    save(os.path.join(block, "holder_metal.png"), tile("#4a4e58", "metal"))
    save(os.path.join(block, "rod_blank.png"), solid("#2a2d34"))
    save(os.path.join(block, "alarm_off.png"), solid("#5a2a2a"))
    save(os.path.join(block, "alarm_on.png"), solid("#ff3a3a", True))
    save(os.path.join(block, "fish_market_side.png"), market_side())
    save(os.path.join(block, "fish_market_top.png"), market_top())
    save(os.path.join(block, "workshop_side.png"), workshop_side())
    save(os.path.join(block, "workshop_top.png"), workshop_top())


if __name__ == "__main__":
    main()
