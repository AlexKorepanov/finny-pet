"""Короткие звуки заданий: «верно», «не совсем» и «урок пройден».

Синтез без сэмплов, только стандартная библиотека Python.
«Не совсем» нарочно мягкий и низкий: он подсказывает, а не ругает.

Запуск: python3 tools/sounds/task_sounds.py app/src/main/res/raw
"""
import math
import os
import struct
import sys
import wave

SR = 44100


def bell(f: float, dur: float, vel: float, decay: float) -> list[float]:
    """Колокольчик: основной тон, октава и лёгкий «звон» сверху."""
    n = int(dur * SR)
    out = []
    for i in range(n):
        t = i / SR
        attack = 1 - math.exp(-t * 900)
        e = attack * math.exp(-t * decay)
        s = (
            math.sin(2 * math.pi * f * t)
            + 0.35 * math.sin(2 * math.pi * 2 * f * t) * math.exp(-t * 8)
            + 0.12 * math.sin(2 * math.pi * 3.01 * f * t) * math.exp(-t * 14)
        )
        out.append(vel * s * e)
    return out


def soft_low(f: float, dur: float, vel: float) -> list[float]:
    """Мягкий низкий тон без резких гармоник: треугольник, быстро затухающий."""
    n = int(dur * SR)
    out = []
    for i in range(n):
        t = i / SR
        attack = 1 - math.exp(-t * 300)
        e = attack * math.exp(-t * 9)
        phase = (f * t) % 1.0
        tri = 4 * abs(phase - 0.5) - 1
        s = 0.7 * tri + 0.3 * math.sin(2 * math.pi * f * t)
        out.append(vel * s * e)
    return out


def mix(length: float, parts: list[tuple[float, list[float]]]) -> list[float]:
    buf = [0.0] * int(length * SR)
    for start, sig in parts:
        offset = int(start * SR)
        for i, v in enumerate(sig):
            if offset + i < len(buf):
                buf[offset + i] += v
    fade = int(0.02 * SR)
    for i in range(fade):
        buf[-1 - i] *= i / fade
    peak = max(abs(v) for v in buf) or 1.0
    return [v / peak * 0.8 for v in buf]


def note(name: str) -> float:
    names = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}
    m = 12 * (int(name[-1]) + 1) + names[name[0]] + (1 if "#" in name else 0)
    return 440.0 * 2 ** ((m - 69) / 12)


def correct() -> list[float]:
    return mix(0.7, [
        (0.00, bell(note("E6"), 0.45, 0.8, 9)),
        (0.09, bell(note("A6"), 0.6, 1.0, 7)),
    ])


def wrong() -> list[float]:
    return mix(0.45, [
        (0.00, soft_low(note("D4"), 0.2, 1.0)),
        (0.13, soft_low(note("A3"), 0.3, 1.0)),
    ])


def lesson_done() -> list[float]:
    return mix(1.4, [
        (0.00, bell(note("C5"), 0.5, 0.7, 6)),
        (0.12, bell(note("E5"), 0.5, 0.7, 6)),
        (0.24, bell(note("G5"), 0.5, 0.7, 6)),
        (0.38, bell(note("C6"), 1.0, 0.9, 3.5)),
        (0.38, bell(note("E6"), 1.0, 0.45, 3.5)),
    ])


def write(path: str, samples: list[float]) -> None:
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1.0, min(1.0, v)) * 32767)) for v in samples))
    print(f"{path}: {len(samples) / SR:.2f}s")


if __name__ == "__main__":
    out_dir = sys.argv[1] if len(sys.argv) > 1 else "."
    write(os.path.join(out_dir, "sfx_correct.wav"), correct())
    write(os.path.join(out_dir, "sfx_wrong.wav"), wrong())
    write(os.path.join(out_dir, "sfx_lesson_done.wav"), lesson_done())
