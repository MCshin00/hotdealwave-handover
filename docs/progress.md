# 진행 상황

단계를 마칠 때와 단계 도중에 세션을 끝낼 때 갱신한다.

| 단계 | 내용 | 상태 | 결과 위치 |
|---|---|---|---|
| 0 | 저장소 준비: 규칙, Skill, 양식, 분석 대상 서브모듈 | 완료 | 이 저장소 |
| 1 | 최종 질문: 개발자가 지금 실제로 궁금한 것을 쓰고 고정 | 완료 | `eval/questions/final.csv` |
| 2 | 파서 추출(HotDealWave 기준선): REST·Feign·Kafka·설정 → `out/analysis.json`, 교차 검증 경고 | 완료 | `extractor/` |
| 3 | AI 추출과 검증(HotDealWave): 추출 Skill, `verifier`, 파서 기준선과 대조 | 시작 전 | `analysis/`, `verifier/` |
| 4 | 업무 규칙·함정: AI 해석과 필요한 경고의 판정을 모아 쓰고 개발자가 확인 | 시작 전 | `knowledge/`, `eval/facts.csv` |
| 5 | 서버와 문서 페이지: 조회 계층, 문서 페이지(Thymeleaf) | 시작 전 | `server/` |
| 6 | 검색·질의응답: `search_docs`, `/api/search`, `/api/ask`(고정 RAG), 검색·질문 페이지 | 시작 전 | `server/` |
| 7 | MCP: 관계 조회 3개와 `search_docs`를 `/mcp`로 노출, Claude Code·Codex에서 확인 | 시작 전 | `server/` |
| 8 | 비교 실험: 기본 코드 검색만 vs MCP 도구 추가 | 시작 전 | `eval/results/` |
| 9 | 두 번째 대상: 개발자의 공개 저장소에 같은 도구를 돌려 검증 경고와 깨진 곳 확인 | 시작 전 | `analysis/` |
| 10 | 배포(선택) | 시작 전 | |

각 단계는 개발자가 시작하라고 할 때 시작한다.

## 지금 위치

2단계 완료. 파서 추출기가 REST·Feign·게이트웨이(#3)와 Kafka·설정·외부 자원(#5)을 `out/analysis.json`으로 뽑고, 고정 커밋에서 쓰이지 않는 처리를 걷어냈다(#7). 교차 검증 경고는 12개(선언 목록 1, Kafka 발행↔구독 11)다. 추출을 AI Skill + 검증으로 바꾸기로 해, 파서 결과는 HotDealWave 기준선으로 남긴다. 다음 할 일: 개발자가 시작하라고 하면 3단계(AI 추출과 검증) 이슈 등록.
