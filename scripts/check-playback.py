"""Require sustained playback progress, not just a successful emulator launch."""
import re
import sys
from pathlib import Path


def playback_advanced(text):
    positions = [int(p) for p in re.findall(r"OnimekuPlayback[^\n]*position_ms=(\d+)", text)]
    # Consecutive samples must advance naturally: a seek alone is not proof.
    steps = 0
    for previous, current in zip(positions, positions[1:]):
        steps = steps + 1 if 100 <= current - previous <= 2500 else 0
        if steps >= 5:
            return True
    return False


if __name__ == "__main__":
    if not playback_advanced(Path(sys.argv[1]).read_text(errors="replace")):
        sys.exit("FAIL: no sustained playback progress in logcat")
    print("PASS: playback position advanced across at least five consecutive samples")
