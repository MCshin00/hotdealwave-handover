import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parent.parent / "scripts" / "check_shared_files.py"
DRIVE = b"X" + b":/"  # 이 파일 자체가 검사에 걸리지 않게 나눠 쓴다


class CheckSharedFilesTest(unittest.TestCase):
    def run_check(self, files: dict[str, bytes]) -> subprocess.CompletedProcess:
        with tempfile.TemporaryDirectory() as folder:
            for name, content in files.items():
                Path(folder, name).write_bytes(content)
            subprocess.run(["git", "init", "-q"], cwd=folder, check=True)
            subprocess.run(["git", "add", "."], cwd=folder, check=True)
            return subprocess.run([sys.executable, str(SCRIPT)], cwd=folder, capture_output=True, text=True)

    def test_binary_bytes_shaped_like_a_drive_path_pass(self):
        result = self.run_check({"capture.png": b"\x89PNG\r\n\x1a\n\x00\x00 " + DRIVE + b"\x00"})
        self.assertEqual(0, result.returncode, result.stdout)

    def test_text_drive_path_still_fails(self):
        result = self.run_check({"capture.png": b"\x89PNG\x00", "notes.md": b"ok\n" + DRIVE + b"work/app\n"})
        self.assertEqual(1, result.returncode)
        self.assertEqual("notes.md:2", result.stdout.strip())

    def test_env_file_fails_even_when_binary(self):
        result = self.run_check({".env": b"\x00"})
        self.assertEqual(1, result.returncode)
        self.assertEqual(".env:1", result.stdout.strip())


if __name__ == "__main__":
    unittest.main()
