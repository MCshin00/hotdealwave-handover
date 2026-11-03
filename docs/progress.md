# 진행 상황

단계를 마칠 때와 단계 도중에 세션을 끝낼 때 갱신한다.

| 단계 | 내용 | 상태 | 결과 위치 |
|---|---|---|---|
| 0 | 저장소 준비: 규칙, Skill, 양식, 분석 대상 서브모듈 | 완료 | 이 저장소 |
| 1 | 최종 질문: 개발자가 지금 실제로 궁금한 것을 쓰고 고정 | 완료 | `eval/questions/final.csv` |
| 2 | 파서 추출(HotDealWave 기준선): REST·Feign·Kafka·설정 → `out/analysis.json`, 교차 검증 경고 | 완료 | `docs/reports/` (파서는 #18에서 삭제) |
| 3 | AI 추출과 검증(HotDealWave): 추출 Skill, `verifier`, 파서 기준선과 대조 | 완료 | `analysis/`, `verifier/`, `docs/reports/` |
| 4 | 배포된 서비스 응답 검증: 떠 있는 서버의 실제 응답을 추출과 비교. HotDealWave는 고정 커밋을 띄워 대신 | 완료 | `analysis/`, `verifier/`, `docs/reports/` |
| 5 | 업무 규칙·함정: AI 해석과 필요한 경고의 판정을 모아 쓰고 개발자가 확인 | 진행 중 | `knowledge/`, `eval/facts.csv`, `docs/reports/` |
| 6 | 서버와 문서 페이지: 조회 계층, 문서 페이지(Thymeleaf) | 시작 전 | `server/` |
| 7 | 검색·질의응답: `search_docs`, `/api/search`, `/api/ask`(고정 RAG), 검색·질문 페이지 | 시작 전 | `server/` |
| 8 | MCP: 관계 조회 3개와 `search_docs`를 `/mcp`로 노출, Claude Code·Codex에서 확인 | 시작 전 | `server/` |
| 9 | 비교 실험: 기본 코드 검색만 vs MCP 도구 추가 | 시작 전 | `eval/results/` |
| 10 | 두 번째 대상: 개발자의 공개 저장소에 같은 도구를 돌려 검증 경고와 깨진 곳 확인 | 시작 전 | `analysis/` |
| 11 | 배포(선택) | 시작 전 | |

각 단계는 개발자가 시작하라고 할 때 시작한다.

## 지금 위치

4단계 완료(#13). 엔드포인트 72개 중 61개를 불러 응답을 저장했고(정상 56, 오류 5), 11개는 이유와 함께 남겼다. 정상 응답의 필드 차이는 0개여서 서비스 파일 수정은 없다. 검증 경고는 12개(선언 목록 1, Kafka 발행↔구독 11)다. 3단계 뒤 바뀐 구조(#15·#18·#19)와 4단계 결과는 [4단계 보고서](reports/probe-hotdealwave.md)에 있다.

5단계 진행 중. #21에서 항목 형식·검증기 검사·`rules-msa-java-spring` Skill을 만들고 7개 서비스에 AI 해석 항목 124개를 썼다. DLQ 경고 2개는 판정해 판정 항목 1개로 옮겼고, 항목 경고는 0개다. 결과와 Skill을 고친 과정은 [5단계 보고서](reports/rules-hotdealwave.md)에 있다. #22에서 검증기가 주석 속 선언을 세지 않게 해 선언 목록 경고가 0개다. 다음 할 일: 6단계 전에 서비스 사이 중복·섞인 항목·덧말 정리(#23).
