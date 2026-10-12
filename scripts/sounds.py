#!/usr/bin/env python3
"""Synthesises the fishing sounds of Atzen and writes them as mono WAV files.

    python3 scripts/sounds.py OUTDIR

Every sound is built from sine waves, filtered noise and envelopes; no samples.
Minecraft needs Ogg Vorbis, so convert afterwards, for example:
    for f in OUTDIR/*.wav; do ffmpeg -i "$f" -ac 1 -c:a libvorbis -q:a 5 "${f%.wav}.ogg"; done
and copy the .ogg files to src/main/resources/assets/atzen/sounds/. Mono matters: only
mono sounds are placed in the world. The events are listed in assets/atzen/sounds.json.
"""
import math
import os
import random
import struct
import sys
import wave

RATE = 44100


def write(path, samples):
    peak = max(1e-9, max(abs(s) for s in samples))
    scale = 0.89 / peak
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s * scale)) * 32767)) for s in samples))


def lowpass(samples, cutoff):
    out, y = [], 0.0
    for i, s in enumerate(samples):
        c = cutoff(i) if callable(cutoff) else cutoff
        y += (1 - math.exp(-2 * math.pi * c / RATE)) * (s - y)
        out.append(y)
    return out


def highpass(samples, cutoff):
    low = lowpass(samples, cutoff)
    return [s - l for s, l in zip(samples, low)]


def noise(n, seed):
    rnd = random.Random(seed)
    return [rnd.uniform(-1, 1) for _ in range(n)]


def fade(samples, attack=0.003, release=0.02):
    n = len(samples)
    a, r = max(1, int(attack * RATE)), max(1, int(release * RATE))
    for i in range(min(a, n)):
        samples[i] *= i / a
    for i in range(min(r, n)):
        samples[n - 1 - i] *= i / r
    return samples


def blank(seconds):
    return [0.0] * int(seconds * RATE)


def mix(base, other, at=0.0, gain=1.0):
    start = int(at * RATE)
    for i, s in enumerate(other):
        if start + i < len(base):
            base[start + i] += s * gain
    return base


def splash(seconds, seed, body=900, size=1.0):
    """Water: a short bright slap, then a fizz of droplets that dies away."""
    n = int(seconds * RATE)
    raw = noise(n, seed)
    slap = lowpass(raw, lambda i: body + 5000 * math.exp(-i / RATE * 30))
    fizz = highpass(noise(n, seed + 1), 2500)
    rnd = random.Random(seed + 2)
    drops = [0.0] * n
    for _ in range(int(14 * size)):
        t0 = rnd.uniform(0.04, seconds * 0.8)
        f = rnd.uniform(700, 1800)
        for i in range(int(0.04 * RATE)):
            k = int(t0 * RATE) + i
            if k < n:
                t = i / RATE
                drops[k] += math.sin(2 * math.pi * (f + 3000 * t) * t) * math.exp(-t * 90) * 0.3
    out = []
    for i in range(n):
        t = i / RATE
        out.append(slap[i] * 2.2 * math.exp(-t * 14) + fizz[i] * 0.35 * math.exp(-t * 6) + drops[i])
    return fade(out, 0.001, 0.05)


def bite():
    """The float is pulled under: a hollow plop."""
    n = int(0.35 * RATE)
    out = []
    for i in range(n):
        t = i / RATE
        f = 520 * math.exp(-t * 9) + 140
        out.append(math.sin(2 * math.pi * f * t) * math.exp(-t * 14))
    return mix(fade(out, 0.002, 0.05), splash(0.3, 3, 700, 0.4), 0.0, 0.5)


def hook_set():
    """The strike: the rod whips back with a whoosh, the line goes taut."""
    n = int(0.4 * RATE)
    whoosh = lowpass(noise(n, 7), lambda i: 400 + 3000 * math.sin(math.pi * min(1, i / n * 1.6)))
    out = []
    for i in range(n):
        t = i / RATE
        env = math.sin(math.pi * min(1.0, t / 0.25)) if t < 0.25 else math.exp(-(t - 0.25) * 30)
        twang = math.sin(2 * math.pi * 330 * t) * math.exp(-max(0, t - 0.22) * 18) * (t > 0.22) * 0.5
        out.append(whoosh[i] * env * 1.6 + twang)
    return fade(out, 0.005, 0.04)


def warning():
    """The fish gathers itself: a low churn of bubbles rising in pitch."""
    n = int(0.9 * RATE)
    rnd = random.Random(9)
    out = [0.0] * n
    for _ in range(26):
        t0 = rnd.uniform(0, 0.8)
        f0 = 200 + 500 * t0 + rnd.uniform(-60, 60)
        for i in range(int(0.06 * RATE)):
            k = int(t0 * RATE) + i
            if k < n:
                t = i / RATE
                out[k] += math.sin(2 * math.pi * (f0 + 2500 * t) * t) * math.exp(-t * 60) * 0.5
    rumble = lowpass(noise(n, 10), 180)
    for i in range(n):
        out[i] += rumble[i] * 3 * math.sin(math.pi * i / n)
    return fade(out, 0.02, 0.1)


def reel(seed):
    """One tooth of the reel's ratchet: a dry, metallic click."""
    n = int(0.03 * RATE)
    raw = highpass(noise(n, seed), 1800)
    out = []
    for i in range(n):
        t = i / RATE
        out.append(raw[i] * math.exp(-t * 700) + 0.4 * math.sin(2 * math.pi * (4100 + seed * 90) * t) * math.exp(-t * 300))
    return fade(out, 0.0003, 0.004)


def creak():
    """The line under strain: a rough, rising groan of fibre on the rod tip."""
    n = int(0.45 * RATE)
    out, phase = [], 0.0
    rnd = random.Random(12)
    for i in range(n):
        t = i / RATE
        f = 160 + 120 * t / 0.45 + rnd.uniform(-25, 25)
        phase += 2 * math.pi * f / RATE
        # stick-slip: a buzzy sawtooth with irregular grain
        saw = (phase / math.pi) % 2 - 1
        out.append(saw * (0.6 + 0.4 * math.sin(2 * math.pi * 31 * t)) * math.sin(math.pi * t / 0.45))
    out = lowpass(out, 2400)
    return fade(out, 0.01, 0.05)


def slack():
    """A limp line slapping the water."""
    return splash(0.25, 14, 500, 0.2)


def leap():
    """The fish breaks the surface and falls back."""
    out = blank(0.9)
    mix(out, splash(0.35, 20, 1200, 0.6), 0.0, 0.8)
    mix(out, splash(0.55, 21, 800, 1.2), 0.32, 1.0)
    return out


def snap():
    """The line breaks: a high twang that is cut off, then a loose whip."""
    n = int(0.6 * RATE)
    crack = highpass(noise(n, 30), 3000)
    out = []
    for i in range(n):
        t = i / RATE
        twang = math.sin(2 * math.pi * 1250 * t * (1 - t * 0.6)) * math.exp(-t * 9) * 0.8
        out.append(crack[i] * math.exp(-t * 120) * 1.5 + twang)
    whip = lowpass(noise(n, 31), 1500)
    for i in range(n):
        t = i / RATE
        if t > 0.08:
            out[i] += whip[i] * 1.5 * math.exp(-(t - 0.08) * 12) * math.sin(math.pi * min(1, (t - 0.08) / 0.1))
    return fade(out, 0.0005, 0.05)


def catch():
    """Landed: the fish comes out of the water, then a short bright rising arpeggio."""
    out = blank(1.2)
    mix(out, splash(0.4, 40, 1100, 0.7), 0.0, 0.7)
    for k, (f, at) in enumerate(((659.3, 0.18), (830.6, 0.3), (987.8, 0.42), (1318.5, 0.54))):
        n = int(0.5 * RATE)
        note = []
        for i in range(n):
            t = i / RATE
            note.append((math.sin(2 * math.pi * f * t) + 0.3 * math.sin(4 * math.pi * f * t)) * math.exp(-t * (6 if k == 3 else 10)))
        mix(out, fade(note, 0.002, 0.05), at, 0.45)
    return fade(out, 0.001, 0.08)


def escape():
    """Gone: a deep swirl and a few sinking bubbles."""
    out = blank(0.8)
    mix(out, splash(0.3, 50, 600, 0.3), 0.0, 0.6)
    for k, at in enumerate((0.15, 0.3, 0.42, 0.55)):
        n = int(0.08 * RATE)
        f0 = 600 - k * 110
        mix(out, [math.sin(2 * math.pi * (f0 - 1500 * (i / RATE)) * (i / RATE)) * math.exp(-(i / RATE) * 40) for i in range(n)], at, 0.5)
    return fade(out, 0.002, 0.08)


def bait():
    """Bait slips onto the hook: a soft, wet squish."""
    n = int(0.18 * RATE)
    raw = lowpass(noise(n, 60), lambda i: 600 + 1800 * math.sin(math.pi * i / n))
    out = [raw[i] * math.sin(math.pi * i / n) ** 0.5 * 2.5 for i in range(n)]
    return fade(out, 0.002, 0.03)


def main():
    out = sys.argv[1] if len(sys.argv) > 1 else "sounds-wav"
    os.makedirs(out, exist_ok=True)
    sounds = {
        "bite": bite(), "hook_set": hook_set(), "warning": warning(), "creak": creak(), "slack": slack(),
        "leap": leap(), "snap": snap(), "catch": catch(), "escape": escape(), "bait": bait(),
    }
    for k in range(3):
        sounds[f"reel{k + 1}"] = reel(k + 1)
    for name, samples in sounds.items():
        write(os.path.join(out, name + ".wav"), samples)
    print(len(sounds), "wav files in", out)


if __name__ == "__main__":
    main()
