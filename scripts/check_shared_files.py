"""커밋된 파일에서 비밀값과 로컬 절대경로를 찾으면 내용은 숨기고 위치만 출력한 뒤 실패한다."""
import os
import re
import subprocess
import sys
from pathlib import Path

PATTERNS = [
    re.compile(rb"sk-[A-Za-z0-9_-]{20,}"),  # OpenAI·Anthropic 형식 키
    re.compile(rb"(?<![A-Za-z0-9])[A-Za-z]:[\\/]"),  # Windows 드라이브 경로
]
HOME = re.compile(rb"/(?:Users|home)/[^/\s]+/")  # macOS·Linux 홈 경로
# 비교 실험 답 원문, 예시의 저장소 수준 추출 결과와 그것을 합친 검증 출력은 분석 대상의 배포 스크립트에 적힌
# 서버 경로를 인용할 수 있어 홈 경로만 보지 않는다.
ANSWERS = "eval/results/"


def quotes_target(name: str) -> bool:
    return name.startswith(ANSWERS) or (name.startswith("examples/")
                                        and name.endswith(("/repository.json", "/out/analysis.json")))


def main() -> int:
    entries = subprocess.check_output(["git", "ls-files", "-s", "-z"])
    failed = False
    for entry in filter(None, entries.split(b"\0")):
        _, raw_path = entry.split(b"\t", 1)
        name = os.fsdecode(raw_path)
        path = Path(name)
        if path.name == ".env" or path.name.startswith(".env."):
            print(f"{name}:1")
            failed = True
            continue
        patterns = PATTERNS if quotes_target(name) else PATTERNS + [HOME]
        for number, line in enumerate(path.read_bytes().splitlines(), 1):
            if any(pattern.search(line) for pattern in patterns):
                print(f"{name}:{number}")
                failed = True
    return int(failed)


if __name__ == "__main__":
    sys.exit(main())
