# 진행 상황

단계를 마칠 때와 단계 도중에 세션을 끝낼 때 갱신한다.

| 단계 | 내용 | 상태 | 결과 위치 |
|---|---|---|---|
| 0 | 저장소 준비: 규칙, Skill, 양식, 분석 대상 서브모듈 | 완료 | 이 저장소 |
| 1 | 최종 질문: 개발자가 지금 실제로 궁금한 것을 쓰고 고정 | 시작 전 | `eval/questions/final.csv` |
| 2 | 추출기: REST·Feign·Kafka·설정 → `out/analysis.json`, 맞대기 검사 경고 | 시작 전 | `extractor/` |
| 3 | 판정: 경고를 판정해 사실 목록 작성, 추출 오류 수정 | 시작 전 | `eval/facts.csv` |
| 4 | 업무 규칙·함정: AI 해석과 판정 결과를 모아 쓰고 개발자가 확인 | 시작 전 | `knowledge/` |
| 5 | 서버와 문서 페이지: 조회 계층, 문서 페이지(Thymeleaf) | 시작 전 | `server/` |
| 6 | 검색·질의응답: `search_docs`, `/api/search`, `/api/ask`(고정 RAG), 검색·질문 페이지 | 시작 전 | `server/` |
| 7 | MCP: 관계 조회 3개와 `search_docs`를 `/mcp`로 노출, Claude Code·Codex에서 확인 | 시작 전 | `server/` |
| 8 | 비교 실험: 기본 코드 검색만 vs MCP 도구 추가 | 시작 전 | `eval/results/` |
| 9 | 배포(선택) | 시작 전 | |

각 단계는 개발자가 시작하라고 할 때 시작한다.

## 지금 위치

0단계 완료. 검증은 맞대기 검사와 판정으로, 구조는 Spring Boot 서버 하나로 정하고, 업무 규칙·함정 층을 더했다([decisions.md](decisions.md)). 다음 할 일: 개발자가 시작하라고 하면 1단계(최종 질문) 이슈 등록.
