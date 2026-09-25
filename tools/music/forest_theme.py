"""Спокойная фоновая мелодия «Лесная полянка» для главного экрана.

Синтез без сэмплов: мягкий пэд, бас, тихое арпеджио и мелодия «музыкальной шкатулки».
Трек бесшовно зацикливается: хвосты нот и реверберации переносятся в начало.

Запуск: python tools/music/forest_theme.py app/src/main/res/raw/music_forest.ogg
Нужны numpy, scipy, soundfile.
"""
import sys

import numpy as np
import soundfile as sf
from scipy.signal import fftconvolve

SR = 44100
BPM = 80
BEAT = 60 / BPM
BARS = 16
LENGTH = int(round(BARS * 4 * BEAT * SR))

NOTE = {"C": 0, "Db": 1, "D": 2, "Eb": 3, "E": 4, "F": 5, "Gb": 6, "G": 7, "Ab": 8, "A": 9, "Bb": 10, "B": 11}


def midi(name: str) -> int:
    pitch, octave = name[:-1], int(name[-1])
    return 12 * (octave + 1) + NOTE[pitch]


def freq(m: int) -> float:
    return 440.0 * 2 ** ((m - 69) / 12)


CHORDS = [
    ("F", ["F3", "A3", "C4"]), ("Dm", ["D3", "F3", "A3"]), ("Bb", ["Bb2", "D3", "F3"]), ("C", ["C3", "E3", "G3"]),
    ("F", ["F3", "A3", "C4"]), ("Am", ["A2", "C3", "E3"]), ("Bb", ["Bb2", "D3", "F3"]), ("C", ["C3", "E3", "G3"]),
    ("Dm", ["D3", "F3", "A3"]), ("Bb", ["Bb2", "D3", "F3"]), ("F", ["F3", "A3", "C4"]), ("C", ["C3", "E3", "G3"]),
    ("Dm", ["D3", "F3", "A3"]), ("Bb", ["Bb2", "D3", "F3"]), ("Gm", ["G2", "Bb2", "D3"]), ("C", ["C3", "E3", "G3"]),
]

# (такт, доля, длительность в долях, нота)
MELODY_BARS = [
    [(0, 1, "A4"), (1, 1, "C5"), (2, 1.5, "F5"), (3.5, 0.5, "E5")],
    [(0, 1.5, "D5"), (1.5, 0.5, "C5"), (2, 2, "A4")],
    [(0, 1, "Bb4"), (1, 1, "D5"), (2, 1, "F5"), (3, 1, "D5")],
    [(0, 2, "C5"), (2, 1, "G4"), (3, 0.5, "A4"), (3.5, 0.5, "Bb4")],
    [(0, 1, "A4"), (1, 1, "C5"), (2, 1, "F5"), (3, 1, "G5")],
    [(0, 1.5, "E5"), (1.5, 0.5, "C5"), (2, 2, "A4")],
    [(0, 1, "D5"), (1, 1, "C5"), (2, 1, "Bb4"), (3, 1, "A4")],
    [(0, 3, "G4")],
    [(0, 1, "F5"), (1, 0.5, "E5"), (1.5, 0.5, "D5"), (2, 2, "A4")],
    [(0, 1, "D5"), (1, 1, "F5"), (2, 1, "D5"), (3, 1, "Bb4")],
    [(0, 1.5, "C5"), (1.5, 0.5, "A4"), (2, 1, "F4"), (3, 1, "A4")],
    [(0, 1, "G4"), (1, 1, "C5"), (2, 2, "E5")],
    [(0, 1.5, "F5"), (1.5, 0.5, "E5"), (2, 1, "D5"), (3, 1, "A4")],
    [(0, 1, "Bb4"), (1, 1, "D5"), (2, 1, "F5"), (3, 1, "E5")],
    [(0, 1, "D5"), (1, 1, "Bb4"), (2, 1, "G4"), (3, 1, "A4")],
    [(0, 2, "G4"), (2, 1, "C5")],
]


def env_adsr(n: int, attack: float, release: float) -> np.ndarray:
    a = max(1, int(attack * SR))
    r = max(1, int(release * SR))
    e = np.ones(n + r)
    e[:a] = np.linspace(0, 1, a) ** 2
    e[n:] = np.linspace(1, 0, r) ** 2
    return e


def add(buf: np.ndarray, start: int, sig: np.ndarray) -> None:
    """Добавляет сигнал по кругу, чтобы хвосты попадали в начало петли."""
    idx = (np.arange(len(sig)) + start) % len(buf)
    np.add.at(buf, idx, sig)


def tine(f: float, dur: float, vel: float) -> np.ndarray:
    """Нота музыкальной шкатулки / калимбы: быстрая атака, мягкое затухание."""
    n = int((dur + 1.6) * SR)
    t = np.arange(n) / SR
    body = np.sin(2 * np.pi * f * t) + 0.18 * np.sin(2 * np.pi * 2 * f * t) * np.exp(-t * 6)
    bell = 0.08 * np.sin(2 * np.pi * 2.76 * f * t) * np.exp(-t * 9)
    e = (1 - np.exp(-t * 400)) * np.exp(-t * 2.2)
    return vel * (body + bell) * e


def pad_voice(f: float, dur: float, vel: float) -> np.ndarray:
    n = int(dur * SR)
    e = env_adsr(n, 0.9, 1.4)
    t = np.arange(len(e)) / SR
    s = sum(np.sin(2 * np.pi * f * d * t + ph) for d, ph in ((1.0, 0), (1.0025, 1.3), (0.9975, 2.1)))
    s += 0.25 * np.sin(2 * np.pi * 2 * f * t)
    trem = 1 + 0.08 * np.sin(2 * np.pi * 0.25 * t)
    return vel * s * e * trem / 3


def bass(f: float, dur: float, vel: float) -> np.ndarray:
    n = int(dur * SR)
    e = env_adsr(n, 0.04, 0.6)
    t = np.arange(len(e)) / SR
    return vel * (np.sin(2 * np.pi * f * t) + 0.15 * np.sin(2 * np.pi * 2 * f * t)) * e * np.exp(-t * 0.35)


def reverb_ir(seconds: float, seed: int) -> np.ndarray:
    rng = np.random.default_rng(seed)
    n = int(seconds * SR)
    t = np.arange(n) / SR
    ir = rng.standard_normal(n) * np.exp(-t * 3.2)
    kernel = np.ones(24) / 24
    ir = np.convolve(ir, kernel, mode="same")
    ir[0] = 0
    return ir / np.sqrt(np.sum(ir ** 2))


def circular_reverb(dry: np.ndarray, ir: np.ndarray) -> np.ndarray:
    wet = fftconvolve(dry, ir)
    out = wet[:len(dry)].copy()
    tail = wet[len(dry):]
    out[:len(tail)] += tail
    return out


def render() -> np.ndarray:
    bar_samples = 4 * BEAT * SR
    left = np.zeros(LENGTH)
    right = np.zeros(LENGTH)
    rng = np.random.default_rng(7)

    for bar, (_, notes) in enumerate(CHORDS):
        start = int(bar * bar_samples)
        for i, name in enumerate(notes):
            v = pad_voice(freq(midi(name) + 12), 4 * BEAT, 0.055)
            pan = 0.35 + 0.15 * i
            add(left, start, v * (1 - pan))
            add(right, start, v * pan)
        b = bass(freq(midi(notes[0]) - 12 if midi(notes[0]) >= midi("C3") else midi(notes[0])), 4 * BEAT, 0.055)
        add(left, start, b)
        add(right, start, b)
        arp = [notes[0], notes[1], notes[2], notes[1]]
        for k in range(8):
            if k in (0, 5):
                continue
            m = midi(arp[k % 4]) + 24
            s = tine(freq(m), 0.4, 0.035 * (0.85 + 0.3 * rng.random()))
            add(left, int(start + k * BEAT / 2 * SR), s * 0.4)
            add(right, int(start + k * BEAT / 2 * SR), s * 0.6)

    for bar, notes in enumerate(MELODY_BARS):
        for beat, dur, name in notes:
            start = int(bar * bar_samples + beat * BEAT * SR + rng.integers(0, 220))
            s = tine(freq(midi(name) + 12), dur * BEAT, 0.13 * (0.9 + 0.2 * rng.random()))
            add(left, start, s * 0.55)
            add(right, start, s * 0.45)

    wet_l = circular_reverb(left, reverb_ir(2.4, 1))
    wet_r = circular_reverb(right, reverb_ir(2.4, 2))
    mix = np.stack([left * 0.8 + wet_l * 0.28, right * 0.8 + wet_r * 0.28], axis=1)

    mix = np.tanh(mix * 1.1) / 1.1
    peak = np.max(np.abs(mix))
    return mix * (10 ** (-4 / 20) / peak)


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "music_forest.ogg"
    audio = render()
    sf.write(out, audio, SR, format="OGG", subtype="VORBIS")
    rms = 20 * np.log10(np.sqrt(np.mean(audio ** 2)))
    print(f"{out}: {LENGTH / SR:.1f}s, rms {rms:.1f} dBFS")
