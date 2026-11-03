# 진행 상황

단계를 마칠 때와 단계 도중에 세션을 끝낼 때 갱신한다.

| 단계 | 내용 | 상태 | 결과 위치 |
|---|---|---|---|
| 0 | 저장소 준비: 규칙, Skill, 양식, 분석 대상 서브모듈 | 완료 | 이 저장소 |
| 1 | 최종 질문: 개발자가 지금 실제로 궁금한 것을 쓰고 고정 | 완료 | `eval/questions/final.csv` |
| 2 | 파서 추출(HotDealWave 기준선): REST·Feign·Kafka·설정 → `out/analysis.json`, 교차 검증 경고 | 완료 | `docs/reports/` (파서는 #18에서 삭제) |
| 3 | AI 추출과 검증(HotDealWave): 추출 Skill, `verifier`, 파서 기준선과 대조 | 완료 | `analysis/`, `verifier/`, `docs/reports/` |
| 4 | 배포된 서비스 응답 검증: 떠 있는 서버의 실제 응답을 추출과 비교. HotDealWave는 고정 커밋을 띄워 대신 | 완료 | `analysis/`, `verifier/`, `docs/reports/` |
| 5 | 업무 규칙·함정: AI 해석과 필요한 경고의 판정을 모아 쓰고 개발자가 확인 | 진행 중: 형식·검증기·Skill 구현 | `docs/format/`, `verifier/`, `.claude/skills/rules-msa-java-spring/` (실제 항목·판정은 후속 작업) |
| 6 | 서버와 문서 페이지: 조회 계층, 문서 페이지(Thymeleaf) | 시작 전 | `server/` |
| 7 | 검색·질의응답: `search_docs`, `/api/search`, `/api/ask`(고정 RAG), 검색·질문 페이지 | 시작 전 | `server/` |
| 8 | MCP: 관계 조회 3개와 `search_docs`를 `/mcp`로 노출, Claude Code·Codex에서 확인 | 시작 전 | `server/` |
| 9 | 비교 실험: 기본 코드 검색만 vs MCP 도구 추가 | 시작 전 | `eval/results/` |
| 10 | 두 번째 대상: 개발자의 공개 저장소에 같은 도구를 돌려 검증 경고와 깨진 곳 확인 | 시작 전 | `analysis/` |
| 11 | 배포(선택) | 시작 전 | |

각 단계는 개발자가 시작하라고 할 때 시작한다.

## 지금 위치

4단계 완료(#13). 결과는 [4단계 보고서](reports/probe-hotdealwave.md)에 있다.

5단계 #21의 앞 세 확인 조건(항목 형식·검증기·`rules-msa-java-spring`)을 `feat/21-knowledge`에서 구현했다. Skill 본문과 Codex 연결 파일을 대상 코드·추출 결과를 읽기 전에 먼저 커밋한 뒤, 대표 테스트 실패를 확인하고 스키마·검증기를 구현했다. 서비스별 입력은 `knowledge/<대상>/<spring.application.name>.json`, 검증 출력은 기존 `out/analysis.json`의 `knowledge` 배열이다. 확인 상태는 그대로 보존한다.

- `./gradlew :verifier:test --tests '*KnowledgeTest'`: 구현 전 2개 실패(항목 출력·영향 ID 경고 없음), 구현 후 2개 통과. 코드·응답의 범위 밖 근거 줄, 없는 영향 ID, 필수 필드 누락과 확인 상태 보존을 검사한다.
- `./gradlew test`: 16개 통과. `python -m unittest discover tests`: 3개 통과.
- `python scripts/check_shared_files.py`: 통과. 새 파일도 스테이징해 검사에 포함했다.
- 항목 디렉터리 없이 `./gradlew :verifier:run`: schema 0, evidence-lines 0, declarations 1, feign-targets 0, gateway-routes 0, kafka-declarations 0, kafka-topics 11. 수정 전후 출력 파일의 SHA-256도 같았다.

실제 `knowledge/hotdealwave/` 항목은 만들지 않았다. 다음 세션에서 Skill로 8개 서비스 실행, 필요한 경고 판정, 개발자 확인, 보고서·PR을 진행한다. 이번 세션은 로컬 커밋까지만 하며 푸시·PR·이슈 상태 변경은 하지 않는다.
