"""python -m unittest discover tests: 비교 실험 조건별 실행 명령과 실행 기록 정리."""
import json
from pathlib import Path
import unittest
from unittest import mock

from scripts import run_eval

ROOT = Path(__file__).resolve().parents[1]


class RunEvalTest(unittest.TestCase):
    def test_base_and_mcp_differ_only_by_the_mcp_server(self):
        base = run_eval.codex_command("base", "folder", "answer.md", "http://localhost:8080")
        mcp = run_eval.codex_command("mcp", "folder", "answer.md", "http://localhost:8080")
        added = ["--skip-git-repo-check",
                 "-c", 'mcp_servers.hotdealwave.url="http://localhost:8080/mcp"',
                 "-c", 'mcp_servers.hotdealwave.default_tools_approval_mode="approve"']
        self.assertNotIn(added[2], base)
        self.assertEqual(mcp, base[:-1] + added + base[-1:])
        for setting in ("--ignore-user-config", "--ephemeral", "read-only", "apps", "memories", 'web_search="disabled"'):
            self.assertIn(setting, base)

    def test_mcp_runs_in_a_new_folder_without_source_and_base_in_the_checkout(self):
        folders = []

        class FakeProcess:
            returncode = 0

            def __init__(self, command, **_):
                folder = Path(command[command.index("-C") + 1])
                folders.append((folder, folder.is_dir() and not any(folder.iterdir())))

            def communicate(self, *_, **__):
                return b"", b""

        with mock.patch.object(run_eval.subprocess, "Popen", FakeProcess):
            run_eval.run_codex("mcp", "질문", None, "http://localhost:8080", 10)
            run_eval.run_codex("mcp", "질문", None, "http://localhost:8080", 10)
            run_eval.run_codex("base", "질문", ROOT / "checkout", "http://localhost:8080", 10)
        (first, first_has_nothing), (second, second_has_nothing), (base, _) = folders
        self.assertTrue(first_has_nothing and second_has_nothing)
        self.assertNotEqual(first, second)
        self.assertEqual(base, ROOT / "checkout")

    def test_mcp_prompt_says_there_is_no_repository(self):
        self.assertEqual(run_eval.prompt("mcp", "createOrder 요청 필드는?"), """\
HotDealWave 저장소는 이 디렉터리에 없다. `hotdealwave` MCP 도구로 이 저장소를 분석한 문서(엔드포인트·흐름·Kafka 연결, 확인 상태와 근거)를 볼 수 있다. 아래 질문에 답하라.
- 주장마다 도구가 보여 준 근거 `파일:줄`을 저장소 루트 기준 경로 그대로 붙인다.
- 저장소에서 확인할 수 없는 것은 확인할 수 없다고 밝힌다.

질문: createOrder 요청 필드는?
""")
        self.assertIn("이 디렉터리는 HotDealWave 저장소다.", run_eval.prompt("base", "createOrder 요청 필드는?"))

    def test_events_give_tokens_tools_and_commands(self):
        events = [
            {"type": "item.completed", "item": {"type": "command_execution", "command": "rg createOrder"}},
            {"type": "item.completed", "item": {"type": "mcp_tool_call", "server": "hotdealwave", "tool": "trace_flow",
                                                "status": "completed", "error": None}},
            {"type": "item.completed", "item": {"type": "mcp_tool_call", "server": "hotdealwave", "tool": "search_docs",
                                                "status": "failed", "error": {"message": "approval required"}}},
            {"type": "item.completed", "item": {"type": "mcp_tool_call", "server": "codex", "tool": "list_mcp_resources",
                                                "status": "completed", "error": None}},
            {"type": "turn.completed", "usage": {"input_tokens": 100, "cached_input_tokens": 60, "output_tokens": 7}},
        ]
        summary = run_eval.summarize(json.dumps(event) for event in events)
        self.assertEqual(summary, {"tokens": {"input": 100, "cachedInput": 60, "output": 7},
                                   "mcpTools": ["trace_flow"], "mcpFailed": ["search_docs"], "commands": 1})

    def test_ask_reply_keeps_abstain(self):
        reply = {"status": "보류", "answer": None, "citations": [], "reason": "모델 보류",
                 "inputTokens": 67104, "outputTokens": 46}
        record = run_eval.ask_record(reply)
        self.assertEqual((record["status"], record["answer"], record["reason"]), ("보류", None, "모델 보류"))
        self.assertEqual(record["tokens"], {"input": 67104, "output": 46})

    def test_checkout_inside_this_repository_is_refused(self):
        with self.assertRaises(SystemExit):
            run_eval.check_checkout(ROOT / "examples/hotdealwave/source")


if __name__ == "__main__":
    unittest.main()
