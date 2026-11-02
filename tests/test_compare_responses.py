"""python -m unittest discover -s tests -v: 쿠폰 목록 추출과 저장 응답 비교."""
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]


class CompareResponsesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        extracted = json.loads((ROOT / "analysis/hotdealwave/services/coupon-service.json").read_text(encoding="utf-8"))
        cls.endpoint = next(
            endpoint for endpoint in extracted["endpoints"]
            if any(line.startswith("coupon/") and line.endswith("CouponController.java:39")
                   for line in endpoint["evidence"])
        )

    def compare(self, used_name):
        (ROOT / "out").mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(dir=ROOT / "out") as directory:
            folder = Path(directory)
            services = folder / "services"
            services.mkdir()
            extracted = services / "coupon-service.json"
            extracted.write_text(json.dumps({"endpoints": [self.endpoint]}), encoding="utf-8")
            responses = folder / "responses"
            responses.mkdir()
            record = {
                "request": {"method": "GET", "path": "/api/v1/coupons", "body": None},
                "statusCode": 200,
                "body": {"message": "ok", "data": [{
                    "couponId": "00000000-0000-0000-0000-000000000001", "name": "test",
                    "discountAmount": 100, "minOrderAmount": 1000,
                    "expirationDate": "2026-01-01", used_name: False,
                    "usedDate": None, "dailyIssuedDate": None,
                }]},
                "reason": None,
            }
            (responses / (self.endpoint["id"] + ".json")).write_text(json.dumps(record), encoding="utf-8")
            return subprocess.run(
                [sys.executable, "-X", "utf8", str(ROOT / "scripts/compare_responses.py"),
                 str(folder), str(responses)],
                capture_output=True, encoding="utf-8", cwd=ROOT,
            )

    def test_used_matches_json_name_inside_generic_list(self):
        result = self.compare("used")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertNotIn("$.data[].used", result.stdout)
        self.assertNotIn("응답에만 있음", result.stdout)
        self.assertNotIn("추출에만 있음", result.stdout)
        self.assertNotIn("이름 다름", result.stdout)

    def test_is_used_is_reported_as_a_name_difference(self):
        result = self.compare("isUsed")
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("이름 다름: $.data[].isUsed ↔ $.data[].used", result.stdout)
        self.assertNotIn("응답에만 있음", result.stdout)
        self.assertNotIn("추출에만 있음", result.stdout)


if __name__ == "__main__":
    unittest.main()
