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

FISH = {
    # length, height, hump (head bluntness), back, side, belly, fins, pattern, tail
    "river_trout": dict(length=40, height=12, back="#5f6b52", side="#b9c2bd", belly="#efe9e0", fins="#8f8a78",
                        stripe="#e0839a", pattern="trout", tail="square"),
    "perch":       dict(length=36, height=16, back="#4f6a2a", side="#b7b54a", belly="#efe1b0", fins="#e5672b",
                        pattern="bars", tail="fork", spiny=True),
    "pike":        dict(length=48, height=10, back="#3f5228", side="#7f8c3e", belly="#e6e2b6", fins="#a07a3a",
                        pattern="spots", tail="fork", snout=1.6),
    "carp":        dict(length=36, height=19, back="#6b4a1c", side="#c48d2e", belly="#efd27a", fins="#c1542a",
                        pattern="scales", tail="fork"),
    "catfish":     dict(length=44, height=13, back="#3c3446", side="#7a6f8a", belly="#d9d2c8", fins="#5a5068",
                        pattern="none", tail="round", barbels=True, flathead=True),
    "golden_koi":  dict(length=36, height=15, back="#f2f0ea", side="#fbf8f2", belly="#ffffff", fins="#f5c24a",
                        pattern="koi", tail="veil"),
}


def fish_fields(spec, seed):
    """Returns body(u,v)->(inside, nx, ny, part) in fish space: u from 0 (snout) to 1 (tail root), v in -1..1."""
    rnd = random.Random(seed)
    spots = [(rnd.uniform(0.18, 0.95), rnd.uniform(-0.8, 0.6), rnd.uniform(0.05, 0.09)) for _ in range(14)]
    koi = [(rnd.uniform(0.15, 0.9), rnd.uniform(-0.9, 0.4), rnd.uniform(0.12, 0.22)) for _ in range(5)]
    return spots, koi


def body_half_height(spec, u):
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
    pts = [at(d0, top(d0) + 1), at(d0 + 0.08, top(d0) - H * 0.26), at(d1 - 0.05, top(d1) - H * 0.14), at(d1, top(d1) + 1)]
    polygon(px, pts, lambda x, y: fins[2] if (x + y) % 3 else fins[1])
    if spec.get("spiny"):
        for k in range(5):
            uu = d0 + 0.03 + k * 0.075
            x0, y0 = at(uu, top(uu))
            x1, y1 = at(uu + 0.02, top(uu) - H * 0.32)
            line(px, x0, y0, x1, y1, fins[1])
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


def fish_item(name):
    px = canvas()
    spec = FISH[name]
    L = spec["length"]
    total = L * (1.36 if spec["tail"] == "veil" else 1.24)
    scale = 56 / total
    ox = (N - total * scale * 0.98) / 2 + 1
    draw_fish(px, ox, 37, scale, -0.24, name, seed=sum(map(ord, name)))
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
    total = spec["length"] * (1.36 if spec["tail"] == "veil" else 1.24)
    scale = 40 / total
    draw_fish(px, 11, 30, scale, -0.08, name, seed=sum(map(ord, name)))
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

GEOMETRY = {  # width, height, body length, nose, tail length (model units = texture pixels)
    "river_trout": (2, 3.5, 8, 1, 4), "perch": (2, 4.8, 6.5, 1, 4), "pike": (1.6, 2.8, 10, 2.5, 4),
    "carp": (3, 4.8, 6.5, 1, 4), "catfish": (3, 2.8, 9, 1, 4), "golden_koi": (2, 3.5, 6.5, 1, 5),
}


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
    save(os.path.join(item, "worm_bait.png"), worm())
    save(os.path.join(item, "glow_bait.png"), glow())
    save(os.path.join(item, "hearty_bait.png"), hearty())
    print("textures written to", os.path.normpath(ROOT))


if __name__ == "__main__":
    main()
