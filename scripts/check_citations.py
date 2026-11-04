"""비교 실험 답의 근거 `파일:줄`이 고정 커밋(응답 근거는 이 저장소)에 있는지 기계로 본다.

같은 답 줄에 백틱으로 쓴 코드 이름이 인용한 줄에 하나라도 있으면 "일치"다. 답이 맞는지는 판단하지 않는다.
"""
import argparse
import csv
from functools import cache
import json
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / "target/HotDealWave"
COMMIT = "cc43016b907331a3ce8471475cbf44412e8a6954"

# 경로:줄, 또는 앞 인용과 같은 파일인 `:줄`. 범위는 `:16`–`:17`, `:16-18`처럼 쓴다.
CITE = re.compile(r"(?:(?P<path>(?:[\w.\-]+/)+[\w.\-]+\.\w+)|(?<=`)):(?P<start>\d+)"
                  r"(?:`?\s*[–-]\s*`?:?(?P<end>\d+)(?![\w.]))?")
CODE = re.compile(r"`([^`]+)`")
WORD = re.compile(r"[A-Za-z_][A-Za-z0-9_]*")


@cache
def read(path):
    """인용한 파일의 줄 목록. 없는 파일은 None."""
    if path.startswith("analysis/"):
        file = ROOT / path
        return file.read_text(encoding="utf-8").splitlines() if file.is_file() else None
    shown = subprocess.run(["git", "-C", str(TARGET), "show", f"{COMMIT}:{path}"], capture_output=True)
    return shown.stdout.decode("utf-8").splitlines() if shown.returncode == 0 else None


def names(line):
    """답 줄의 백틱 안에서 경로·줄 번호가 아닌 코드 이름."""
    return {word for span in CODE.findall(line) if "/" not in span and not span.startswith(":")
            for word in WORD.findall(span)}


def check(answer):
    found = []
    for line in (answer or "").splitlines():
        path = None
        line_names = names(line)
        for match in CITE.finditer(line):
            path = match["path"] or path
            if path is None:
                continue
            start = int(match["start"])
            end = int(match["end"] or start)
            lines = read(path)
            exists = lines is not None and 1 <= start <= end <= len(lines)
            if not line_names:
                name = "이름 없음"
            elif exists and any(word in text for text in lines[start - 1:end] for word in line_names):
                name = "일치"
            else:
                name = "불일치"
            found.append({"path": path, "start": start, "end": end, "exists": "있음" if exists else "없음", "name": name})
    return found


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("results", type=Path, help="run_eval.py 결과 디렉터리(예: eval/results/final)")
    args = parser.parse_args()
    rows = []
    for file in sorted(args.results.glob("*/*.json")):
        record = json.loads(file.read_text(encoding="utf-8"))
        found = check(record["answer"])
        missing = [f"{c['path']}:{c['start']}" for c in found if c["exists"] == "없음"]
        mismatched = [f"{c['path']}:{c['start']}" for c in found if c["name"] == "불일치"]
        rows.append({"id": record["id"], "조건": record["condition"], "결과": record["status"],
                     "인용": len(found), "있음": len(found) - len(missing), "없음": len(missing),
                     "이름 불일치": len(mismatched), "없는 근거": "; ".join(missing),
                     "이름 불일치 근거": "; ".join(mismatched)})
    with (args.results / "check.csv").open("w", encoding="utf-8", newline="") as out:
        writer = csv.DictWriter(out, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    for condition in sorted({row["조건"] for row in rows}):
        part = [row for row in rows if row["조건"] == condition]
        print(f"{condition}: 답 {len(part)}, 인용 없음 {sum(row['인용'] == 0 for row in part)}, "
              f"인용 {sum(row['인용'] for row in part)}, 없음 {sum(row['없음'] for row in part)}, "
              f"이름 불일치 {sum(row['이름 불일치'] for row in part)}")


if __name__ == "__main__":
    main()
