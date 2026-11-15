import importlib.util
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location("check_playback", Path(__file__).with_name("check-playback.py"))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class PlaybackCheckTest(unittest.TestCase):
    def evidence(self, positions):
        return "\n".join(f"D OnimekuPlayback: position_ms={p}" for p in positions)

    def test_advancing(self):
        self.assertTrue(module.playback_advanced(self.evidence(range(0, 7000, 1000))))

    def test_empty_or_stalled(self):
        self.assertFalse(module.playback_advanced(""))
        self.assertFalse(module.playback_advanced(self.evidence([1000] * 10)))

    def test_seek_is_not_playback(self):
        self.assertFalse(module.playback_advanced(self.evidence(range(0, 100000, 10000))))

    def test_reset_breaks_sequence(self):
        self.assertFalse(module.playback_advanced(self.evidence([0, 1000, 2000, 0, 1000, 2000])))


if __name__ == "__main__":
    unittest.main()
