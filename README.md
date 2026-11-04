# HotDealWave 인수인계 도구

문서도, 인수인계해 줄 사람도 없이 마이크로서비스 프로젝트를 넘겨받는 두 상황을 다룬다. 서비스는 떠 있는데 문서가 없거나, 팀이 떠나 저장소만 남아 서비스를 되살려야 한다. 이 도구는 팀 프로젝트 [HotDealWave](https://github.com/sparta-final-12th-9700/HotDealWave)(Spring Boot, Spring Cloud, Kafka)의 고정 커밋을 분석해 API, 서비스 간 호출, 이벤트 흐름, 설정을 뽑고, 서버가 떠 있으면 응답으로 확인한 뒤, 그 결과를 서버 하나로 제공한다. 두 상황의 차이는 [결정 기록](docs/decisions.md#문제-상황)에 있다.

## 하는 일

```
target/HotDealWave (@cc43016)
  --> AI 추출(Skill)
  --> analysis/hotdealwave/services/*.json + relations.json
  --> verifier (Java) <-- knowledge/hotdealwave/*.json (업무 규칙·함정)
  --> out/analysis.json
  --> server (Spring Boot) --+--> 문서 페이지
                            +--> 검색·질문 페이지
                            +--> /mcp
```

모든 결과에는 근거 `파일:줄`과 상태가 붙는다. 저장소만으로 확정할 수 없는 연결은 추측하지 않는다.

업무 규칙·함정은 `rules-msa-java-spring` Skill이 추출 결과에서 출발해 코드·저장된 응답을 읽고 서비스별 항목 파일로 쓴다. 검증기가 이 항목도 `out/analysis.json`에 합친다. 아래는 추출의 상태이며, 업무 규칙·함정은 개발자가 정하는 `확인함`·`틀림`·`미확인`을 쓴다.

| 상태 | 뜻 |
|---|---|
| 확인됨 | 저장소 안의 코드·설정으로 확정된다 |
| 확인 안 됨 | 저장소 안에서 연결을 확정할 수 없다(예: 발행·구독 쪽 토픽 환경변수 이름이 다름) |
| 저장소에 근거 없음 | 저장소 밖에만 있는 사실이다(예: 환경변수 값, 클라우드 콘솔 설정) |

## 구조

```
hotdealwave-handover/
|
+-- target/HotDealWave/     # 분석 대상 서브모듈 (팀 원본 저장소 cc43016, 읽기만)
|
+-- analysis/               # AI 추출 결과
+-- .claude/skills/         # 추출·응답 검증·업무 규칙 Skill
|
+-- verifier/               # 검증기 (Java 17, Jackson) -> out/analysis.json, 상태·경고
|
+-- server/                 # Spring Boot 서버 하나
|                           #   조회: 관계 조회 3개 + search_docs(BM25), 모델 호출 없음
|                           #   페이지: 문서·검색·질문 (Thymeleaf)
|                           #   /api/search, /api/ask(고정 RAG), /mcp(Streamable HTTP)
|
+-- knowledge/<대상>/      # 서비스별 업무 규칙·함정 JSON (AI 해석·판정, 개발자 확인 상태)
+-- eval/                   # 최종 질문, 판정한 사실 목록, 결과
+-- out/                    # 생성 결과물 (커밋하지 않음)
```

### 질문이 처리되는 길

MCP와 질의응답은 같은 조회 계층을 쓰고, 답을 쓰는 모델의 위치만 다르다.

```
[MCP 경로] Claude Code·Codex에서 질문
  -> 모델이 /mcp의 도구를 고른다
  -> 조회 계층이 analysis.json에서 찾는다
  -> 결과(파일:줄, 상태)를 보고 클라이언트의 모델이 답한다

[RAG 경로] 질문 페이지 또는 /api/ask
  -> search_docs가 문서 조각을 찾는다 (서비스·종류 필터 + BM25)
  -> 조각과 질문으로 외부 모델을 한 번 부른다 (유료)
  -> 답의 근거가 조각에 있으면 답하고, 없으면 보류한다
```

## MCP 도구

| 도구 | 하는 일 |
|---|---|
| `find_endpoint` | 경로·ID·핸들러 이름으로 엔드포인트 본문(요청·응답 필드, 호출, 검증 범위, 업무 규칙·함정)을 낸다 |
| `trace_flow` | 엔드포인트·Kafka 발행·구독에서 Feign 호출·Kafka 발행·구독을 따라간 단계와 부르는 곳을 낸다. 이어지지 않는 곳은 상태와 이유에서 멈춘다 |
| `find_subscribers` | 토픽 이름으로 발행·구독과 그 연결을 담은 토픽 본문을 낸다 |
| `search_docs` | 본문을 BM25로 찾아 상위 5개 절의 페이지 주소·절 제목·점수·미리보기를 낸다 |
| `get_page` | 페이지 주소로 본문 전체를 낸다 |

도구 결과는 페이지 본문과 같은 텍스트라 값마다 상태·이유·근거 `파일:줄`이 붙는다.

결정과 이유는 [docs/decisions.md](docs/decisions.md), 진행 상황은 [docs/progress.md](docs/progress.md)에 있다.

## 받기

```bash
git clone --recurse-submodules https://github.com/MCshin00/hotdealwave-handover.git
```

이미 받았다면 `git submodule update --init`으로 분석 대상을 받는다.

## 검사

JDK 17 이상이 필요하다.

```bash
./gradlew build
./gradlew test
./gradlew :verifier:run
# 선택: 입력·대상·출력 경로 변경
./gradlew :verifier:run --args="analysis/hotdealwave target/HotDealWave out/analysis.json"
python scripts/check_shared_files.py
python -m unittest discover tests
# 4단계: 저장한 응답과 추출의 응답 필드 비교
python scripts/compare_responses.py analysis/hotdealwave analysis/hotdealwave/responses
```

`./gradlew :verifier:run`은 AI 추출 디렉터리 `analysis/hotdealwave`의 `services/*.json`과 `relations.json`을 스키마로 먼저 검사해 `out/analysis.json`을 만들고 스키마·근거 줄·선언 목록·Feign 대상·게이트웨이 경로·Kafka·핸들러 호출 검사의 경고 수를 출력한다. 같은 기준 위치의 `knowledge/hotdealwave/`가 있으면 항목 스키마·근거 줄 존재·영향 ID·서비스 사이 중복을 검사해 `knowledge` 배열과 경고를 같은 출력에 더한다. 디렉터리가 없으면 기존 출력과 같다. 개발자의 확인 상태는 바꾸지 않는다. 결과 구조는 [docs/format/README.md](docs/format/README.md)에 있다.

공유 파일 검사는 Git이 추적하는 파일에 비밀값이나 로컬 절대경로가 있으면 위치를 출력하고 실패한다. 비교 실험 답 원문(`eval/results/`)은 분석 대상이 적어 둔 서버 경로를 인용할 수 있어 홈 경로만 보지 않는다. 응답 비교는 엔드포인트별로 응답에만 있는 필드, 추출에만 있는 필드, 이름이 다른 필드와 비교 보류를 출력한다.

## 서버

```bash
./gradlew :server:bootRun
```

검증기를 먼저 돌려 `out/analysis.json`을 새로 만든 뒤 `http://localhost:8080`에 문서 페이지를 띄운다. 첫 화면은 서비스·토픽별 개수와 확인 안 된 연결 목록을 보여 준다. 첫 화면에서 서비스(`/services/{이름}`)와 토픽(`/topics/{토픽 환경변수 이름 또는 코드에 적힌 토픽 이름}`)으로 들어가고, 서비스 페이지에서 엔드포인트(`/endpoints/{ID}`)로 들어간다. 값마다 확인 상태와 근거 `파일:줄`이 붙고, 확인되지 않은 값은 상태와 이유가 대신 나온다. 근거는 접혀 있고, 펼치면 고정 커밋의 원본 줄이나 저장한 응답 원문(`/responses/{파일}`)의 그 줄로 가는 링크다.

`/api/search?q=찾을 말`은 상위 5개 절의 페이지 주소·절 제목·점수·미리보기를 JSON으로 낸다. `/search`는 같은 검색과 질문을 하는 페이지다.

질문(`/search`의 질문 칸, `POST /api/ask` 본문 `{"question": "..."}`)은 검색 상위 5개를 근거로 모델을 한 번 부르고, 인용 검사를 통과하면 답과 인용한 페이지를, 아니면 보류와 이유를 낸다. 모델 키는 `OPENAI_API_KEY` 환경변수로 준다. 질문 한 번이 유료 호출 한 번이다.

MCP는 서버를 띄운 채 클라이언트에 등록한다.

```bash
claude mcp add --transport http hotdealwave http://localhost:8080/mcp
```

```bash
codex mcp add hotdealwave --url http://localhost:8080/mcp
```

## 비교 실험

[결정 문서 "비교 실험"](docs/decisions.md#비교-실험)의 세 조건을 질문 파일로 실행한다. 기준선(`base`)·MCP는 Codex CLI가 필요하다. 기준선은 이 저장소 밖에 둔 HotDealWave 고정 커밋 체크아웃을 읽고, MCP는 저장소 없이 질문마다 새 빈 폴더에서 도구만 쓴다. MCP·질의응답은 서버가 떠 있어야 하고, 질의응답은 유료 모델을 질문마다 한 번 부른다.

```bash
git clone https://github.com/sparta-final-12th-9700/HotDealWave ../HotDealWave-cc43016
git -C ../HotDealWave-cc43016 checkout cc43016b907331a3ce8471475cbf44412e8a6954
python scripts/run_eval.py eval/questions/tuning.csv base --checkout ../HotDealWave-cc43016 --timeout 300
python scripts/run_eval.py eval/questions/tuning.csv mcp --timeout 300
python scripts/run_eval.py eval/questions/tuning.csv ask --timeout 120
python scripts/check_citations.py eval/results/tuning
```

결과는 `eval/results/<질문 파일 이름>/<조건>/<id>.json`(답, 걸린 시간, 토큰, 결과, 부른 MCP 도구)이다. 기계 검사는 답의 `파일:줄`이 고정 커밋(응답 근거는 이 저장소)에 있는지와 같은 답 줄의 코드 이름이 그 줄에 있는지를 `check.csv`에 남긴다. 이름 확인은 참고용이고, 답이 맞는지는 판단하지 않는다. `repo-mcp`는 체크아웃과 MCP를 함께 준 #41 실행 결과이고, 그 스크립트는 `7274f6f`에 있다.
