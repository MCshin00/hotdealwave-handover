"""비교 실험 AI 교차 채점: 질문마다 세 조건의 답을 가려 한 채점 세션에 넘기고, 두 채점을 맞춰 본다.

grade: <결과 디렉터리>/grades/<채점자>/<id>.json 을 쓴다. 채점자는 codex, opus.
merge: 두 채점을 맞춰 grades/grades.csv 와 개발자 확인 목록 grades/review.md 를 쓴다.
       grades/judgments.csv 가 있으면 판정한 칸의 값을 최종값으로 쓴다.
"""
import argparse
from concurrent.futures import ThreadPoolExecutor
import csv
import json
import os
from pathlib import Path
import random
import shutil
import subprocess
import time

from scripts.run_eval import check_checkout, summarize

ROOT = Path(__file__).resolve().parents[1]
RESPONSES = ROOT / "examples/hotdealwave/responses"
CONDITIONS = ("base", "mcp", "ask")
LABELS = ("A", "B", "C")
CODEX_MODEL = "gpt-6-astra"
SCHEMA = {
    "type": "object", "additionalProperties": False, "required": ["grades"],
    "properties": {"grades": {"type": "array", "items": {
        "type": "object", "additionalProperties": False,
        "required": ["label", "accuracy", "scope", "reason", "evidence"],
        "properties": {
            "label": {"type": "string", "enum": list(LABELS)},
            "accuracy": {"type": "string", "enum": ["맞음", "부분", "틀림"]},
            "scope": {"type": "string", "enum": ["밝힘", "일부", "안 밝힘"]},
            "reason": {"type": "string"},
            "evidence": {"type": "array", "items": {"type": "string"}},
        }}}},
}

PROMPT = """이 디렉터리는 HotDealWave 저장소의 고정 커밋(cc43016)이다. 아래 질문에 대한 답 A·B·C를 채점한다.
- 답이 인용한 `responses/` 응답 파일은 `{responses}` 아래 같은 경로에 있다. 이 두 곳 밖의 파일은 열지 않는다.
- 파일을 고치지 않는다.
- 세 답은 같은 질문에 서로 다른 방식으로 쓴 답이다. 답의 출처나 말투가 아니라, 주장과 근거 `파일:줄`을 코드와 대조한 결과로만 매긴다.

답마다 두 값을 매긴다.
- accuracy(정확도)
  - 맞음: 질문의 핵심에 답했고 주장이 코드와 맞으며 틀린 주장이 없다.
  - 부분: 맞는 핵심이 있으나 빠진 핵심이나 코드와 다른 주장이 있다.
  - 틀림: 핵심 주장이 코드와 다르거나 질문에 답하지 못했다.
  - 저장소에 근거가 없는 것을 묻는 질문은 저장소에 근거가 없다고 밝히면 맞음이다.
- scope(확인 범위): 코드로 확인되는 것과, 확인되지 않는 연결이나 저장소 밖에서 정해지는 값을 구분했는가.
  - 밝힘: 구분해 밝혔다. 확인되지 않는 것이 없는 질문이면 확인된 것을 그대로 말한 답도 밝힘이다.
  - 일부: 일부만 구분했다.
  - 안 밝힘: 확인되지 않는 연결이나 저장소 밖 값을 확인된 것처럼 말했다.
- reason: 한두 문장. evidence: 대조한 `파일:줄`.

# 질문

{question}

{answers}"""


def order(question_id, seed):
    """질문마다 시드로 정해지는 답 순서. A·B·C → 조건."""
    return dict(zip(LABELS, random.Random(f"{seed}:{question_id}").sample(CONDITIONS, len(CONDITIONS))))


def grading_prompt(question, records, label_order, responses):
    answers = "".join(f"## 답 {label}\n\n{records[condition]['answer'] or '(답 없음)'}\n\n"
                      for label, condition in label_order.items())
    return PROMPT.format(responses=responses, question=question, answers=answers)


def by_condition(graded, label_order):
    return {label_order[g["label"]]: {k: g[k] for k in ("accuracy", "scope", "reason", "evidence")}
            for g in graded["grades"]}


def grader_command(grader, checkout, responses, schema, last_message):
    if grader == "codex":
        return ["codex", "exec", "--ignore-user-config", "--ephemeral", "--json",
                "-s", "read-only", "-c", 'approval_policy="never"', "-c", 'windows.sandbox="elevated"',
                "--disable", "apps", "--disable", "memories", "-c", 'web_search="disabled"',
                "-m", CODEX_MODEL, "-c", 'model_reasoning_effort="high"',
                "-C", str(checkout), "--output-schema", str(schema), "-o", str(last_message), "-"]
    return ["claude", "-p", "--model", "opus", "--effort", "high", "--tools", "Read,Grep,Glob",
            "--strict-mcp-config", "--no-session-persistence", "--setting-sources", "project",
            "--output-format", "json", "--json-schema", json.dumps(SCHEMA), "--add-dir", str(responses)]


def run(command, prompt, cwd, timeout):
    command[0] = shutil.which(command[0]) or command[0]
    process = subprocess.Popen(command, cwd=cwd, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                               stderr=subprocess.DEVNULL)
    try:
        output, _ = process.communicate(prompt.encode("utf-8"), timeout=timeout)
        return ("완료" if process.returncode == 0 else f"오류(종료 코드 {process.returncode})"), output
    except subprocess.TimeoutExpired:
        if os.name == "nt":
            subprocess.run(["taskkill", "/T", "/F", "/PID", str(process.pid)], capture_output=True)
        else:
            process.kill()
        return "시간 초과", process.communicate()[0]


def grade_question(grader, question_id, records, checkout, seed, timeout):
    label_order = order(question_id, seed)
    # tempfile 폴더는 만든 사용자만 읽을 수 있어 Codex 샌드박스가 응답 파일을 열지 못한다.
    # 체크아웃 옆에 일반 권한 폴더를 만들고 끝나면 지운다.
    directory = checkout.parent / f"grade-{grader}-{question_id}-{os.getpid()}"
    directory.mkdir()
    try:
        shutil.copytree(RESPONSES, directory / "responses")
        schema, last_message = directory / "schema.json", directory / "answer.json"
        schema.write_text(json.dumps(SCHEMA), encoding="utf-8")
        prompt = grading_prompt(records["base"]["question"], records, label_order, directory.as_posix())
        started = time.monotonic()
        status, output = run(grader_command(grader, checkout, directory, schema, last_message),
                             prompt, checkout, timeout)
        seconds = round(time.monotonic() - started, 1)
        text = output.decode("utf-8", "replace")
        graded, usage = None, None
        if status == "완료" and grader == "codex" and last_message.exists():
            graded = json.loads(last_message.read_text(encoding="utf-8"))
            usage = summarize(line for line in text.splitlines() if line.startswith("{"))["tokens"]
        elif status == "완료" and grader == "opus":
            reply = json.loads(text)
            graded, usage = reply.get("structured_output"), {**reply.get("usage", {}), "costUSD": reply.get("total_cost_usd")}
    finally:
        shutil.rmtree(directory, ignore_errors=True)
    if graded:  # 응답 파일을 복사본 절대경로로 적은 근거를 저장소 기준 경로로 돌린다.
        text = json.dumps(graded, ensure_ascii=False)
        for prefix in (directory.as_posix() + "/", json.dumps(str(directory) + "\\")[1:-1]):
            text = text.replace(prefix, "")
        graded = json.loads(text)
    grades = by_condition(graded, label_order) if graded else None
    return {"id": question_id, "grader": grader, "status": status if grades else f"{status}, 채점 없음",
            "seconds": seconds, "order": label_order, "grades": grades, "usage": usage, "seed": seed}


def load_records(results):
    records = {}
    for condition in CONDITIONS:
        for file in sorted((results / condition).glob("*.json")):
            record = json.loads(file.read_text(encoding="utf-8"))
            records.setdefault(record["id"], {})[condition] = record
    return records


def merge(codex, opus, judgments=None):
    """judgments: (id, 조건) -> judgments.csv 행. 판정한 칸은 그 값을, 두 채점이 같은 칸은 그 값을 최종값으로 쓴다."""
    judgments = judgments or {}
    rows = []
    for question_id in sorted(codex):
        for condition in CONDITIONS:
            if condition not in codex[question_id]:
                continue
            a, b = codex[question_id][condition], opus[question_id][condition]
            row = {"id": question_id, "조건": condition,
                   "정확도": a["accuracy"] if a["accuracy"] == b["accuracy"] else "갈림",
                   "확인 범위": a["scope"] if a["scope"] == b["scope"] else "갈림",
                   "codex 정확도": a["accuracy"], "opus 정확도": b["accuracy"],
                   "codex 확인 범위": a["scope"], "opus 확인 범위": b["scope"]}
            judged = judgments.get((question_id, condition))
            if judged:
                row.update({"최종 정확도": judged["정확도"], "최종 확인 범위": judged["확인 범위"],
                            "판정 경로": judged["판정 경로"]})
            else:
                agreed = "갈림" not in (row["정확도"], row["확인 범위"])
                row.update({"최종 정확도": row["정확도"], "최종 확인 범위": row["확인 범위"],
                            "판정 경로": "AI 두 채점 일치" if agreed else ""})
            rows.append(row)
    return rows


def load_judgments(path):
    if not path.exists():
        return {}
    with path.open(encoding="utf-8", newline="") as file:
        return {(r["id"], r["조건"]): r for r in csv.DictReader(file)}


def review_list(rows, seed):
    """갈린 답 전부와, 갈리지 않은 답 가운데 시드로 고른 5개."""
    split = [r for r in rows if "갈림" in (r["정확도"], r["확인 범위"])]
    agreed = [r for r in rows if r not in split]
    return split + random.Random(seed).sample(agreed, min(5, len(agreed)))


NAMES = {"base": "기준선", "mcp": "MCP", "ask": "질의응답"}


def cell(text):
    return text.replace("|", "\\|").replace("\n", " ")


def review_markdown(review, records, graded, seed):
    """갈린 것 종류별로 묶고, 항목마다 두 채점을 나란히 놓는다. 답 원문은 접는다."""
    groups = [("정확도가 갈린 답", [r for r in review if r["정확도"] == "갈림"]),
              ("확인 범위만 갈린 답", [r for r in review if r["정확도"] != "갈림" and r["확인 범위"] == "갈림"]),
              (f"두 채점이 같은 답 가운데 무작위 표본(시드 {seed})", [r for r in review if "갈림" not in (r["정확도"], r["확인 범위"])])]
    lines = ["# 개발자 확인 목록", "",
             "정확도: 맞음·부분·틀림. 확인 범위: 밝힘·일부·안 밝힘. 갈림은 두 채점자의 값이 다르다는 표시다.", "",
             "| 묶음 | 질문 | 조건 | 정확도 Codex / Opus | 확인 범위 Codex / Opus |", "|---|---|---|---|---|"]
    for number, (_, rows) in enumerate(groups, 1):
        for row in rows:
            lines.append(f"| {number} | [{row['id']}](#{row['id'].lower()}-{row['조건']}) | {NAMES[row['조건']]} | "
                         f"{row['codex 정확도']} / {row['opus 정확도']} | {row['codex 확인 범위']} / {row['opus 확인 범위']} |")
    for number, (title, rows) in enumerate(groups, 1):
        lines += ["", f"## {number}. {title} ({len(rows)})"]
        for row in rows:
            record = records[row["id"]][row["조건"]]
            codex, opus = (graded[g][row["id"]][row["조건"]] for g in ("codex", "opus"))
            evidence = lambda g: ", ".join(f"`{e}`" for e in g["evidence"][:4]) + (f" 외 {len(g['evidence']) - 4}개" if len(g["evidence"]) > 4 else "")
            lines += ["", f'<a id="{row["id"].lower()}-{row["조건"]}"></a>', "",
                      f"### {row['id']} {NAMES[row['조건']]}", "", f"> {cell(record['question'])}", "",
                      "| | Codex | Opus |", "|---|---|---|",
                      f"| 정확도 | {codex['accuracy']} | {opus['accuracy']} |",
                      f"| 확인 범위 | {codex['scope']} | {opus['scope']} |",
                      f"| 이유 | {cell(codex['reason'])} | {cell(opus['reason'])} |",
                      f"| 대조한 근거 | {evidence(codex)} | {evidence(opus)} |", "",
                      "<details><summary>답 원문</summary>", "", record["answer"] or "(답 없음)", "", "</details>"]
    return "\n".join(lines) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    grade = sub.add_parser("grade")
    grade.add_argument("results", type=Path, help="run_eval.py 결과 디렉터리(예: eval/results/final)")
    grade.add_argument("grader", choices=["codex", "opus"])
    grade.add_argument("--checkout", type=Path, required=True, help="HotDealWave 고정 커밋 체크아웃")
    grade.add_argument("--timeout", type=int, required=True, help="질문당 시간 제한(초)")
    grade.add_argument("--jobs", type=int, default=1, help="동시에 돌릴 채점 세션 수")
    grade.add_argument("--seed", type=int, default=45)
    combine = sub.add_parser("merge")
    combine.add_argument("results", type=Path)
    combine.add_argument("--seed", type=int, default=45)
    args = parser.parse_args()

    records = load_records(args.results)
    out = args.results / "grades"
    if args.command == "grade":
        checkout = check_checkout(args.checkout)
        (out / args.grader).mkdir(parents=True, exist_ok=True)

        def one(question_id):
            result = grade_question(args.grader, question_id, records[question_id], checkout, args.seed, args.timeout)
            (out / args.grader / f"{question_id}.json").write_text(
                json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            print(f"{question_id} {args.grader}: {result['status']}, {result['seconds']}초", flush=True)

        with ThreadPoolExecutor(max_workers=args.jobs) as pool:
            list(pool.map(one, sorted(records)))
        return

    graded = {grader: {f.stem: json.loads(f.read_text(encoding="utf-8"))["grades"]
                       for f in sorted((out / grader).glob("*.json"))} for grader in ("codex", "opus")}
    rows = merge(graded["codex"], graded["opus"], load_judgments(out / "judgments.csv"))
    with (out / "grades.csv").open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    (out / "review.md").write_text(review_markdown(review_list(rows, args.seed), records, graded, args.seed),
                                   encoding="utf-8")
    for condition in CONDITIONS:
        part = [r for r in rows if r["조건"] == condition]
        count = lambda key, value: sum(r[key] == value for r in part)
        print(f"{condition}: 최종 정확도 맞음 {count('최종 정확도', '맞음')}·부분 {count('최종 정확도', '부분')}·"
              f"틀림 {count('최종 정확도', '틀림')}·갈림 {count('최종 정확도', '갈림')}, "
              f"최종 확인 범위 밝힘 {count('최종 확인 범위', '밝힘')}·일부 {count('최종 확인 범위', '일부')}·"
              f"안 밝힘 {count('최종 확인 범위', '안 밝힘')}·갈림 {count('최종 확인 범위', '갈림')}")


if __name__ == "__main__":
    main()
