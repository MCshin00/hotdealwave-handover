"""python -m unittest discover tests: 비교 실험 답을 조건을 가려 채점하고 두 채점을 맞춰 본다."""
import unittest

from scripts import grade_eval

RECORDS = {"base": {"answer": "기준선의 답"}, "mcp": {"answer": "문서 도구의 답"}, "ask": {"answer": "질의응답의 답"}}


class GradeEvalTest(unittest.TestCase):
    def test_prompt_hides_conditions_behind_shuffled_labels(self):
        order = grade_eval.order("Q01", seed=7)
        self.assertEqual(sorted(order.values()), ["ask", "base", "mcp"])
        self.assertEqual(order, grade_eval.order("Q01", seed=7))
        prompt = grade_eval.grading_prompt("createOrder 요청 필드는?", RECORDS, order, "responses")
        for name in ("base", "mcp", "ask", "condition", "기준선", "MCP", "질의응답"):
            self.assertNotIn(name, prompt.replace(RECORDS["base"]["answer"], "")
                             .replace(RECORDS["mcp"]["answer"], "").replace(RECORDS["ask"]["answer"], ""))
        for label, condition in order.items():
            self.assertIn(f"## 답 {label}\n\n{RECORDS[condition]['answer']}\n", prompt)

    def test_grades_go_back_to_conditions(self):
        order = {"A": "mcp", "B": "ask", "C": "base"}
        graded = {"grades": [
            {"label": "B", "accuracy": "틀림", "scope": "밝힘", "reason": "r", "evidence": []},
            {"label": "A", "accuracy": "맞음", "scope": "밝힘", "reason": "r", "evidence": ["a.java:1"]},
            {"label": "C", "accuracy": "부분", "scope": "일부", "reason": "r", "evidence": []},
        ]}
        grades = grade_eval.by_condition(graded, order)
        self.assertEqual({c: g["accuracy"] for c, g in grades.items()}, {"mcp": "맞음", "ask": "틀림", "base": "부분"})

    def test_merge_keeps_agreement_and_lists_splits_with_five_samples(self):
        def grade(accuracy, scope="밝힘"):
            return {"accuracy": accuracy, "scope": scope, "reason": "", "evidence": []}
        codex = {f"Q0{i}": {"base": grade("맞음")} for i in range(1, 8)}
        opus = {f"Q0{i}": {"base": grade("맞음")} for i in range(1, 8)}
        opus["Q03"]["base"] = grade("부분")
        rows = grade_eval.merge(codex, opus)
        self.assertEqual([(r["id"], r["정확도"], r["확인 범위"]) for r in rows if r["id"] in ("Q02", "Q03")],
                         [("Q02", "맞음", "밝힘"), ("Q03", "갈림", "밝힘")])
        review = grade_eval.review_list(rows, seed=7)
        self.assertEqual(len(review), 6)
        self.assertIn(("Q03", "base"), [(r["id"], r["조건"]) for r in review])
        self.assertEqual(review, grade_eval.review_list(rows, seed=7))

    def test_judgment_settles_split_and_agreement_stays(self):
        def grade(accuracy):
            return {"accuracy": accuracy, "scope": "밝힘", "reason": "", "evidence": []}
        codex = {"Q01": {"mcp": grade("맞음"), "ask": grade("맞음")}}
        opus = {"Q01": {"mcp": grade("맞음"), "ask": grade("틀림")}}
        judgments = {("Q01", "ask"): {"정확도": "부분", "확인 범위": "밝힘", "판정 경로": "개발자"}}
        rows = grade_eval.merge(codex, opus, judgments)
        self.assertEqual([(r["조건"], r["정확도"], r["최종 정확도"], r["판정 경로"]) for r in rows],
                         [("mcp", "맞음", "맞음", "AI 두 채점 일치"), ("ask", "갈림", "부분", "개발자")])


if __name__ == "__main__":
    unittest.main()
