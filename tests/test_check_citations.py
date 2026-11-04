"""python -m unittest discover tests: 답의 `파일:줄`이 고정 커밋과 응답 파일에 있는지."""
import unittest

from scripts.check_citations import check

CONTROLLER = "order/src/main/java/com/sparta/hotdeal/order/presentation/controller/OrderController.java"
DTO = "order/src/main/java/com/sparta/hotdeal/order/application/dtos/order/req/ReqPostOrderDto.java"
RESPONSE = "analysis/hotdealwave/responses/order-service.OrderController.createOrder.json"


class CheckCitationsTest(unittest.TestCase):
    def test_existing_and_missing_lines(self):
        found = check(f"`createOrder`는 `{CONTROLLER}:43`에 있다. 없는 줄은 `{CONTROLLER}:99999`다.")
        self.assertEqual([(c["path"], c["start"], c["exists"]) for c in found],
                         [(CONTROLLER, 43, "있음"), (CONTROLLER, 99999, "없음")])
        self.assertEqual(found[0]["name"], "일치")

    def test_name_not_on_cited_line(self):
        self.assertEqual(check(f"`createOrder`는 `{CONTROLLER}:40`이다.")[0]["name"], "불일치")

    def test_short_form_and_range_use_the_last_path_on_the_line(self):
        found = check(f"`basketList`는 필수다 `{DTO}:16`–`:17`, 컨트롤러는 `{CONTROLLER}:40`, `:43`")
        self.assertEqual([(c["path"], c["start"], c["end"], c["exists"]) for c in found],
                         [(DTO, 16, 17, "있음"), (CONTROLLER, 40, 40, "있음"), (CONTROLLER, 43, 43, "있음")])
        self.assertEqual(found[0]["name"], "일치")

    def test_response_evidence_is_read_from_this_repository(self):
        found = check(f"`couponId` 값은 `{RESPONSE}:10`에 있다.")
        self.assertEqual((found[0]["exists"], found[0]["name"]), ("있음", "일치"))

    def test_root_file_citation(self):
        found = check("`ACTIVE_PROFILE`은 `docker-compose.yml:32`, 없는 줄은 `docker-compose.yml:99999`")
        self.assertEqual([(c["path"], c["exists"]) for c in found],
                         [("docker-compose.yml", "있음"), ("docker-compose.yml", "없음")])
        self.assertEqual(found[0]["name"], "일치")

    def test_root_file_citation_is_not_a_name(self):
        self.assertEqual(check("근거: `docker-compose.yml:32`")[0]["name"], "이름 없음")

    def test_answer_without_citations(self):
        self.assertEqual(check("저장소에서 확인할 수 없습니다."), [])


if __name__ == "__main__":
    unittest.main()
