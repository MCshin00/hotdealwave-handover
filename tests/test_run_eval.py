"""python -m unittest discover tests: 비교 실험 조건별 실행 명령과 실행 기록 정리."""
import json
from pathlib import Path
import unittest

from scripts import run_eval

ROOT = Path(__file__).resolve().parents[1]


class RunEvalTest(unittest.TestCase):
    def test_base_and_mcp_differ_only_by_the_mcp_server(self):
        base = run_eval.codex_command("base", "checkout", "answer.md", "http://localhost:8080")
        mcp = run_eval.codex_command("mcp", "checkout", "answer.md", "http://localhost:8080")
        added = ["-c", 'mcp_servers.hotdealwave.url="http://localhost:8080/mcp"',
                 "-c", 'mcp_servers.hotdealwave.default_tools_approval_mode="approve"']
        self.assertNotIn(added[1], base)
        self.assertEqual(mcp, base[:-1] + added + base[-1:])
        for setting in ("--ignore-user-config", "--ephemeral", "read-only", "apps", "memories", 'web_search="disabled"'):
            self.assertIn(setting, base)

    def test_mcp_prompt_adds_only_the_tool_line(self):
        base = run_eval.prompt("base", "createOrder 요청 필드는?").splitlines()
        mcp = run_eval.prompt("mcp", "createOrder 요청 필드는?").splitlines()
        self.assertEqual([line for line in mcp if line not in base],
                         ["- `hotdealwave` MCP 도구로 이 저장소를 분석한 문서(엔드포인트·흐름·Kafka 연결, 확인 상태와 근거)를 볼 수 있다."])
        self.assertEqual([line for line in mcp if line in base], base)

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
            run_eval.check_checkout(ROOT / "target/HotDealWave")


if __name__ == "__main__":
    unittest.main()
