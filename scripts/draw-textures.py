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
    r = max(1.6, H * 0.12)
    for y in range(int(ey - r) - 1, int(ey + r) + 2):
        for x in range(int(ex - r) - 1, int(ex + r) + 2):
            d = math.hypot(x + 0.5 - ex, y + 0.5 - ey)
            if d <= r:
                put(px, x, y, (235, 226, 196, 255) if d > r * 0.55 else (20, 18, 24, 255))
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
    "river_rod": dict(shaft="#9a6a35", wrap="#2f9c8f", grip="#5b3a1e", reel="#c9783a", tip="#e8d6a8"),
    "iron_rod":  dict(shaft="#4a4f57", wrap="#b8bec6", grip="#a3322c", reel="#c7ccd2", tip="#e2e6ea"),
    "tide_rod":  dict(shaft="#234a9a", wrap="#e3b53c", grip="#6b3aa0", reel="#e3b53c", tip="#5ff2f0", crystal=True),
    "carbon_rod": dict(shaft="#26282c", wrap="#d8302c", grip="#3a3c42", reel="#b8bec6", tip="#e8e8ea", weave=True),
    "bamboo_rod": dict(shaft="#c8b060", wrap="#6a8a3a", grip="#8a6a3a", reel="#9a7a4a", tip="#e8dca0"),
    "spinning_rod": dict(shaft="#3a4a5a", wrap="#e0e0e0", grip="#c89a60", reel="#b8bec6", tip="#f0f0f0"),
    "carp_rod": dict(shaft="#2a3a2a", wrap="#c8a040", grip="#202020", reel="#3a3a3a", tip="#e8e0c0"),
    "fly_rod": dict(shaft="#7a4a2a", wrap="#e0c060", grip="#c89a60", reel="#5a7a4a", tip="#f0e8d0"),
    "big_game_rod": dict(shaft="#d8d0c0", wrap="#c83a2a", grip="#1a1a1a", reel="#d0a83a", tip="#ffffff"),
}


def rod(name):
    spec = RODS[name]
    px = canvas()
    shaft, wrap, grip, reel, tip = (ramp(spec[k]) for k in ("shaft", "wrap", "grip", "reel", "tip"))
    x0, y0, x1, y1 = 7, 58, 54, 8
    length = math.hypot(x1 - x0, y1 - y0)
    dx, dy = (x1 - x0) / length, (y1 - y0) / length
    nx, ny = -dy, dx
    for y in range(N):
        for x in range(N):
            px_, py_ = x + 0.5 - x0, y + 0.5 - y0
            along = px_ * dx + py_ * dy
            across = px_ * nx + py_ * ny
            if not (0 <= along <= length):
                continue
            t = along / length
            width = 4.6 - 2.8 * t
            if t < 0.28:
                width = 5.6
            if abs(across) > width / 2:
                continue
            k = across / (width / 2)
            light = lit(k * 0.9, -k * 0.3)
            if t < 0.28:
                c = shade(grip, light)
                if int(along) % 4 == 0:
                    c = grip[1]
            elif 0.42 < t < 0.45 or 0.62 < t < 0.645 or 0.8 < t < 0.82:
                c = shade(wrap, light + 0.1)
            elif t > 0.93:
                c = shade(tip, light + 0.2)
            else:
                c = shade(shaft, light)
                # carbon weave: a fine diagonal checker
                if spec.get("weave") and (int(along) + int(across * 2)) % 3 == 0:
                    c = shaft[min(4, shaft.index(c) + 1)] if c in shaft else c
            px[y][x] = c
    # butt cap
    disc(px, x0 + 1, y0 - 1, 3.2, wrap)
    # reel
    rx, ry = x0 + dx * 18 + nx * 6.5, y0 + dy * 18 + ny * 6.5
    line(px, x0 + dx * 18, y0 + dy * 18, rx, ry, reel[1], 2)
    disc(px, rx, ry, 5.2, reel, ring=0.55)
    disc(px, rx, ry, 2.0, reel)
    line(px, rx, ry, rx + 5, ry + 3, reel[0], 1.5)
    disc(px, rx + 5.5, ry + 3.3, 1.4, grip)
    # line from the tip, hook at the end
    lx, ly = x1 + 1, y1 + 1
    line_colour = (236, 240, 246, 255)
    line(px, lx, ly, lx + 4, ly + 30, line_colour)
    hx, hy = lx + 4, ly + 30
    hook = ramp("#b9c0c8")
    for a in range(0, 200, 10):
        r = math.radians(a)
        put(px, hx - 2.2 + 2.2 * math.cos(r), hy + 2.2 * math.sin(r) + 1, hook[1])
    put(px, hx - 4.4, hy, hook[3])
    if spec.get("crystal"):
        glow = ramp("#5ff2f0")
        polygon(px, [(x1 - 3, y1 + 3), (x1 + 2, y1 - 6), (x1 + 6, y1 - 1), (x1 + 2, y1 + 5)],
                lambda x, y: glow[4] if (x + y) % 4 == 0 else glow[3])
    outline(px)
    if spec.get("crystal"):
        for sx, sy in ((x1 + 8, y1 - 4), (x1 - 6, y1 - 2), (x1 + 4, y1 + 9)):
            put(px, sx, sy, (190, 255, 250, 255))
            put(px, sx + 1, sy, (120, 230, 240, 255))
            put(px, sx - 1, sy, (120, 230, 240, 255))
            put(px, sx, sy + 1, (120, 230, 240, 255))
            put(px, sx, sy - 1, (120, 230, 240, 255))
    return px


# ---------------------------------------------------------------- reels, lines, carbon fibre

REELS = {"copper_reel": "#c9783a", "iron_reel": "#b8bec6", "diamond_reel": "#4fd8d0", "baitcaster_reel": "#c83a3a",
         "multiplier_reel": "#d0a83a", "fly_reel": "#6a8a5a", "echo_reel": "#1e5a6a", "tide_reel": "#3ab8a8",
         "netherite_reel": "#4a4446", "ancient_reel": "#5a4a2a", "reed_reel": "#5a8a2a", "current_reel": "#2a3a5a",
         "deep_reel": "#0e3a44"}
LINES = {"nylon_line": "#e8eef2", "braided_line": "#5fa04a", "carbon_line": "#3a3d44"}


def reel(name):
    """A reel seen from the side. Spinning reels: spool, foot, crank. Baitcaster: low and wide. Multiplier: a big
    drum between two plates. Fly reel: a flat disc with holes. Boss reels get their trophy's glint."""
    if name == "baitcaster_reel":
        return baitcaster(name)
    if name in ("multiplier_reel", "current_reel", "ancient_reel"):
        return multiplier(name)
    if name == "fly_reel":
        return fly_reel(name)
    px = canvas()
    metal = ramp(REELS[name])
    dark = ramp("#3a3c42")
    polygon(px, [(26, 8), (38, 8), (35, 22), (29, 22)], lambda x, y: shade(metal, 0.8 - (x - 26) / 30))   # foot
    disc(px, 32, 36, 19, metal, ring=0.62)
    disc(px, 32, 36, 11, ramp("#e8e2d0"))                                                                     # wound line
    for k in range(4):
        a = k * math.pi / 2 + 0.4
        line(px, 32 + 12 * math.cos(a), 36 + 12 * math.sin(a), 32 + 17 * math.cos(a), 36 + 17 * math.sin(a), metal[1], 1.5)
    disc(px, 32, 36, 4.5, metal)
    line(px, 32, 36, 52, 50, dark[2], 3)                                                                      # crank arm
    disc(px, 53, 51, 4.2, dark)
    if name == "diamond_reel":
        for sx, sy in ((14, 18), (50, 22), (18, 54)):
            put(px, sx, sy, (210, 255, 250, 255))
    outline(px)
    glints(px, name)
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
    "maggot_bait": maggot, "corn_bait": lambda: blob_bait("#f0c83a", 12, 4, (4, 5.5)), "bread_bait": bread,
    "boilie_bait": lambda: blob_bait("#c87a3a", 4, 7, (8, 10), (20, 40)),
    "baitfish_bait": lambda: fish_bait("#5a7a8a", "#e8eef2"), "fish_strip_bait": lambda: fish_bait("#5a6a7a", "#f0d8d0", True),
    "larva_bait": larva, "shrimp_bait": shrimp,
    "spinner": lambda: lure("spinner", "#d8d8d8", "#e04030"), "spoon": lambda: lure("spoon", "#e8c040", "#d84030"),
    "wobbler": lambda: lure("wobbler", "#3a8a3a", "#f0e8a0"), "soft_lure": lambda: lure("soft_lure", "#8ad040", "#d8c040"),
    "popper": lambda: lure("popper", "#f0f0e8", "#e04040"), "jig": lambda: lure("jig", "#e8a030", "#c0c4cc"),
    "streamer": lambda: lure("streamer", "#4a8a3a", "#e8e0a0"), "dry_fly": lambda: lure("dry_fly", "#a87a4a", "#e8e0d0"),
    "nymph": lambda: lure("nymph", "#6a5a3a", "#d8b040"),
    "reed_king_lure": lambda: lure("wobbler", "#2a5a1a", "#e8c040"),
    "ancient_bait": lambda: boss_bait("ancient_bait"), "current_bait": lambda: boss_bait("current_bait"),
    "deep_light_bait": lambda: boss_bait("deep_light_bait"),
}
SPOOLS = {"shallow_spool": "#9aa4ae", "deep_spool": "#5a6470", "light_spool": "#c9783a", "reinforced_spool": "#3a3c42",
          "fast_gear_spool": "#c83a3a", "power_gear_spool": "#d0a83a"}


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


HOOKS = {"small_single_hook": 0.6, "single_hook": 0.8, "large_single_hook": 1.0, "treble_hook": 0.75, "barbless_hook": 0.8,
         "carp_hook": 0.85, "offset_hook": 0.85, "circle_hook": 1.0}


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

TOMES = {
    "steady_line_tome": dict(cover="#2f7a4a", emblem="knot"),
    "reel_power_tome":  dict(cover="#a8322a", emblem="spiral"),
    "calm_waters_tome": dict(cover="#2f55a8", emblem="wave"),
}


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
        c = base[2 if v < 0.15 or v > 0.9 else 3]
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
    box(0, 0, w, h, length, ((nose + 3) / total, 1))
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
    save(os.path.join(item, "hearty_bait.png"), hearty())
    for name, draw in BAITS.items():
        save(os.path.join(item, name + ".png"), draw())
    for name in TROPHIES:
        save(os.path.join(item, name + ".png"), trophy_item(name))
    for name in SPOOLS:
        save(os.path.join(item, name + ".png"), spare_spool(name))
    for name in ("fluoro_leader", "steel_leader"):
        save(os.path.join(item, name + ".png"), leader(name))
    for name in HOOKS:
        save(os.path.join(item, name + ".png"), hook_item(name))
    print("textures written to", os.path.normpath(ROOT))


if __name__ == "__main__":
    main()
