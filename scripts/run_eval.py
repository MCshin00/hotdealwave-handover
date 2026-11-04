"""비교 실험: 질문 파일의 질문마다 조건 하나를 실행하고 답·시간·토큰을 남긴다.

조건
- base: Codex, 이 저장소가 없는 HotDealWave 고정 커밋 체크아웃, 기본 도구만
- mcp: base와 같은 Codex 설정에 서버의 /mcp를 더하고, 저장소 대신 질문마다 새 빈 폴더에서 실행한다
- ask: 서버의 /api/ask를 한 번 부른다(유료 모델 호출)

결과는 <out>/<조건>/<id>.json이다. 기계 검사는 check_citations.py가 한다.
"""
import argparse
import csv
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import time
import urllib.error
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
COMMIT = "cc43016b907331a3ce8471475cbf44412e8a6954"
MODEL = "gpt-6-astra"
REASONING = "high"

PROMPT = """이 디렉터리는 HotDealWave 저장소다. 아래 질문에 답하라.
- 주장마다 근거를 저장소 루트 기준 `파일:줄`로 붙인다.
- 저장소에서 확인할 수 없는 것은 확인할 수 없다고 밝힌다.
- 파일을 고치지 않는다.

질문: {question}
"""
# 저장소 없이 MCP만 쓰는 조건. 도구 안내는 팀이 MCP를 등록할 때 함께 남기는 안내에 해당한다.
MCP_PROMPT = """HotDealWave 저장소는 이 디렉터리에 없다. `hotdealwave` MCP 도구로 이 저장소를 분석한 문서(엔드포인트·흐름·Kafka 연결, 확인 상태와 근거)를 볼 수 있다. 아래 질문에 답하라.
- 주장마다 도구가 보여 준 근거 `파일:줄`을 저장소 루트 기준 경로 그대로 붙인다.
- 저장소에서 확인할 수 없는 것은 확인할 수 없다고 밝힌다.

질문: {question}
"""


def prompt(condition, question):
    return (MCP_PROMPT if condition == "mcp" else PROMPT).format(question=question)


def codex_command(condition, folder, last_message, server):
    """두 조건은 MCP 서버 설정만 다르다. 사용자 설정·앱 연결·기억·웹 검색은 끈다."""
    command = ["codex", "exec", "--ignore-user-config", "--ephemeral", "--json",
               "-s", "read-only", "-c", 'approval_policy="never"', "-c", 'windows.sandbox="elevated"',
               "--disable", "apps", "--disable", "memories", "-c", 'web_search="disabled"',
               "-m", MODEL, "-c", f'model_reasoning_effort="{REASONING}"', "-c", 'service_tier="fast"',
               "-C", str(folder), "-o", str(last_message)]
    if condition == "mcp":  # 빈 폴더는 Git 저장소가 아니다. 승인 정책이 never라 도구 호출도 승인 없이 통과시킨다.
        command += ["--skip-git-repo-check", "-c", f'mcp_servers.hotdealwave.url="{server}/mcp"',
                    "-c", 'mcp_servers.hotdealwave.default_tools_approval_mode="approve"']
    return command + ["-"]


def summarize(lines):
    """codex exec --json 이벤트에서 토큰, 이 서버의 MCP 도구 호출(성공·실패), 셸 명령 수."""
    summary = {"tokens": {"input": 0, "cachedInput": 0, "output": 0}, "mcpTools": [], "mcpFailed": [], "commands": 0}
    for line in lines:
        event = json.loads(line)
        item = event.get("item", {})
        if event["type"] == "turn.completed":
            usage = event["usage"]
            summary["tokens"] = {"input": usage["input_tokens"], "cachedInput": usage["cached_input_tokens"],
                                 "output": usage["output_tokens"]}
        elif event["type"] == "item.completed" and item.get("type") == "mcp_tool_call" and item["server"] == "hotdealwave":
            succeeded = item.get("status") == "completed" and not item.get("error")
            summary["mcpTools" if succeeded else "mcpFailed"].append(item["tool"])
        elif event["type"] == "item.completed" and item.get("type") == "command_execution":
            summary["commands"] += 1
    return summary


def ask_record(reply):
    status = {"답": "완료", "보류": "보류"}.get(reply["status"], "오류")
    return {"status": status, "answer": reply["answer"], "reason": reply["reason"],
            "citations": reply["citations"], "tokens": {"input": reply["inputTokens"], "output": reply["outputTokens"]}}


def check_checkout(checkout):
    """이 저장소 밖의, 고정 커밋에서 바뀐 것 없는 체크아웃이어야 한다."""
    checkout = Path(checkout).resolve()
    if checkout == ROOT or ROOT in checkout.parents:
        raise SystemExit(f"체크아웃이 이 저장소 안에 있음: {checkout}")
    head = subprocess.run(["git", "-C", str(checkout), "rev-parse", "HEAD"], capture_output=True, text=True).stdout.strip()
    if head != COMMIT:
        raise SystemExit(f"체크아웃이 고정 커밋이 아님: {head or '저장소 아님'}")
    if subprocess.run(["git", "-C", str(checkout), "status", "--porcelain"], capture_output=True, text=True).stdout:
        raise SystemExit("체크아웃에 바뀐 파일이 있음")
    return checkout


def run_codex(condition, question, checkout, server, timeout):
    with tempfile.TemporaryDirectory() as directory:
        last_message = Path(directory) / "answer.md"
        folder = checkout
        if condition == "mcp":
            folder = Path(directory) / "empty"
            folder.mkdir()
        started = time.monotonic()
        command = codex_command(condition, folder, last_message, server)
        command[0] = shutil.which(command[0]) or command[0]  # Windows에서는 codex.cmd
        process = subprocess.Popen(command, stdin=subprocess.PIPE,
                                   stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
        try:
            output, _ = process.communicate(prompt(condition, question).encode("utf-8"), timeout=timeout)
            status = "완료" if process.returncode == 0 else f"오류(종료 코드 {process.returncode})"
        except subprocess.TimeoutExpired:
            if os.name == "nt":
                subprocess.run(["taskkill", "/T", "/F", "/PID", str(process.pid)], capture_output=True)
            else:
                process.kill()
            output, _ = process.communicate()
            status = "시간 초과"
        seconds = round(time.monotonic() - started, 1)
        answer = last_message.read_text(encoding="utf-8") if status == "완료" and last_message.exists() else None
    if answer is not None and checkout:  # 절대경로로 인용한 답을 저장소 루트 기준으로 맞춘다.
        for prefix in (str(checkout) + os.sep, checkout.as_posix() + "/"):
            answer = answer.replace(prefix, "")
    lines = [line for line in output.decode("utf-8", "replace").splitlines() if line.startswith("{")]
    return {"status": status, "seconds": seconds, **summarize(lines), "answer": answer,
            "settings": {"model": MODEL, "reasoning": REASONING, "serviceTier": "fast", "timeout": timeout}}


def run_ask(question, server, timeout):
    request = urllib.request.Request(f"{server}/api/ask", data=json.dumps({"question": question}).encode("utf-8"),
                                     headers={"Content-Type": "application/json"})
    started = time.monotonic()
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            record = ask_record(json.loads(response.read().decode("utf-8")))
    except (urllib.error.URLError, TimeoutError) as error:
        record = {"status": "오류", "answer": None, "reason": str(error), "citations": [], "tokens": None}
    return {**record, "seconds": round(time.monotonic() - started, 1)}


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("questions", type=Path, help="질문 CSV(id, 질문 열)")
    parser.add_argument("condition", choices=["base", "mcp", "ask"])
    parser.add_argument("--checkout", type=Path, help="HotDealWave 고정 커밋 체크아웃(base)")
    parser.add_argument("--server", default="http://localhost:8080", help="문서 서버 주소(mcp, ask)")
    parser.add_argument("--timeout", type=int, required=True, help="질문당 시간 제한(초)")
    parser.add_argument("--out", type=Path, help="결과 디렉터리. 기본은 eval/results/<질문 파일 이름>")
    args = parser.parse_args()

    checkout = check_checkout(args.checkout) if args.condition == "base" else None
    if args.condition != "base":
        try:
            urllib.request.urlopen(f"{args.server}/api/search?q=order", timeout=10).close()
        except urllib.error.URLError as error:
            raise SystemExit(f"서버에 연결할 수 없음: {args.server} ({error})")
    out = (args.out or ROOT / "eval/results" / args.questions.stem) / args.condition
    out.mkdir(parents=True, exist_ok=True)
    with args.questions.open(encoding="utf-8", newline="") as file:
        questions = list(csv.DictReader(file))
    for row in questions:
        if args.condition == "ask":
            record = run_ask(row["질문"], args.server, args.timeout)
        else:
            record = run_codex(args.condition, row["질문"], checkout, args.server, args.timeout)
        record = {"id": row["id"], "condition": args.condition, "question": row["질문"], **record}
        (out / f"{row['id']}.json").write_text(json.dumps(record, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        tools = f", 도구 성공 {len(record['mcpTools'])}·실패 {len(record['mcpFailed'])}" if args.condition == "mcp" else ""
        print(f"{row['id']} {args.condition}: {record['status']}, {record['seconds']}초{tools}", flush=True)


if __name__ == "__main__":
    main()
