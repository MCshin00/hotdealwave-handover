# Spring 마이크로서비스 인수인계 도구

문서도, 인수인계해 줄 사람도 없이 마이크로서비스 프로젝트를 넘겨받는 두 상황을 다룬다. 서비스는 떠 있는데 문서가 없거나, 팀이 떠나 저장소만 남아 서비스를 되살려야 한다. 이 도구는 Spring Boot·Spring Cloud 마이크로서비스 저장소의 고정 커밋을 분석해 API, 서비스 간 호출, 이벤트 흐름, 설정을 뽑고, 서버가 떠 있으면 응답으로 확인한 뒤, 그 결과를 서버 하나로 제공한다. 예시로 팀 프로젝트 [HotDealWave](https://github.com/sparta-final-12th-9700/HotDealWave)의 결과를 커밋해 두었다. 두 상황의 차이는 [결정 기록](docs/decisions.md#문제-상황)에 있다.

## 결과

개발자가 실험 전에 고정한 질문 21개로, 저장소 없이 이 도구의 문서로 답하는 쪽과 저장소를 가진 Codex를 비교했다([9단계 보고서](docs/reports/eval-hotdealwave.md)).

| 답하는 쪽 | 맞음 | 부분 | 틀림 | 확인 안 된 것을 확인된 것처럼 말한 답 | 걸린 시간(중앙값) |
|---|---|---|---|---|---|
| Codex + 저장소 | 21 | 0 | 0 | 1 | 71초 |
| Codex + MCP(저장소 없음) | 12 | 9 | 0 | 0 | 49초 |
| 질의응답(`/api/ask`) | 6 | 11 | 4 | 1 | 4초 |

- 저장소 없이 MCP 도구만 쓴 Codex는 틀린 답이 없었고, 확인 안 된 연결을 확인된 것처럼 말하지 않았다. 셸 명령 없이 도구만으로 답했고, 파일 인용 580개는 모두 고정 커밋에 있는 줄이었다.
- 콘솔·메일 계정·Grafana처럼 저장소 밖에만 있는 사실을 묻는 질문에는 세 쪽 모두 "저장소에 근거 없음"을 밝혔다.
- 맞음이 아닌 답 24개는 모두 원인을 찾았다. 13개는 아직 뽑지 않은 내용(오류 응답 본문, 배포 워크플로, 페이지 직렬화 설정 등) 때문이라, 다음에 뽑을 것이 정해져 있다.
- 채점은 Codex와 Opus가 따로 하고, 갈린 칸은 개발자가 정한 기준으로 판정했다. 판정 기록은 `eval/results/final/grades/`에 있다.

## 하는 일

```
작업 폴더의 source/ (대상 @고정 커밋, init이 받음)
  --> AI 추출(Skill)
  --> 작업 폴더의 services/*.json + relations.json + repository.json
  --> verify (Java 검증기) <-- knowledge/*.json (업무 규칙·함정)
  --> 작업 폴더의 out/analysis.json
  --> serve (Spring Boot) --+--> 문서 페이지
                            +--> 검색·질문 페이지
                            +--> /mcp
```

모든 결과에는 근거 `파일:줄`과 상태가 붙는다. 다루는 범위는 근거 위치로 가른다. 저장소 안에 근거가 있으면 API든 배포·운영 설정이든 뽑을 대상이고(아직 뽑지 않는 것은 [결정 기록](docs/decisions.md#문제-상황)에 있다), 저장소 밖에만 있는 사실은 "저장소에 근거 없음"으로 밝힌다. 저장소만으로 확정할 수 없는 연결은 추측하지 않는다.

업무 규칙·함정은 `rules-msa-java-spring` Skill이 추출 결과에서 출발해 코드·저장된 응답을 읽고 서비스별 항목 파일로 쓴다. 검증기가 이 항목도 `out/analysis.json`에 합친다. 아래는 추출의 상태이며, 업무 규칙·함정은 개발자가 정하는 `확인함`·`틀림`·`미확인`을 쓴다.

| 상태 | 뜻 |
|---|---|
| 확인됨 | 저장소 안의 코드·설정으로 확정된다 |
| 확인 안 됨 | 저장소 안에서 연결을 확정할 수 없다(예: 발행·구독 쪽 토픽 환경변수 이름이 다름) |
| 저장소에 근거 없음 | 저장소 밖에만 있는 사실이다(예: 환경변수 값, 클라우드 콘솔 설정) |

## 구조

```
spring-msa-handover/
|
+-- examples/hotdealwave/   # 예시 작업 폴더 (팀 원본 저장소 cc43016)
|                           #   추출 결과, 응답, 업무 규칙·함정, 검증 출력. source/는 init이 받음
+-- .claude/skills/         # 추출·응답 검증·업무 규칙 Skill
|
+-- cli/                    # init·verify·serve 명령. 검증기와 서버를 build/handover.jar 하나로 묶음
+-- verifier/               # 검증기 (Java 17, Jackson) -> out/analysis.json, 상태·경고
|
+-- server/                 # Spring Boot 서버 하나
|                           #   조회: 관계 조회 3개 + search_docs(BM25), 모델 호출 없음
|                           #   페이지: 문서·검색·질문 (Thymeleaf)
|                           #   /api/search, /api/ask(고정 RAG), /mcp(Streamable HTTP)
|
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
  -> 문서 조각을 찾는다 (서비스·종류 필터 + BM25와 임베딩 순위를 RRF로 합침)
  -> 조각과 질문으로 외부 모델을 한 번 부른다 (유료)
  -> 답의 근거가 조각에 있으면 답하고, 없으면 보류한다
```

## MCP 도구

| 도구 | 하는 일 |
|---|---|
| `find_endpoint` | 경로·ID·핸들러 이름으로 엔드포인트 본문(요청·응답 필드, 호출, 검증 범위, 업무 규칙·함정)을 낸다 |
| `trace_flow` | 엔드포인트·메시지 발행·구독에서 HTTP 호출·메시지 발행·구독을 따라간 단계와 부르는 곳을 낸다. 이어지지 않는 곳은 상태와 이유에서 멈춘다 |
| `find_subscribers` | 토픽 이름으로 발행·구독과 그 연결을 담은 토픽 본문을 낸다 |
| `search_docs` | 본문을 BM25로 찾아 상위 5개 절의 페이지 주소·절 제목·점수·미리보기를 낸다 |
| `get_page` | 페이지 주소로 본문 전체를 낸다 |

도구 결과는 페이지 본문과 같은 텍스트라 값마다 상태·이유·근거 `파일:줄`이 붙는다.

결정과 이유는 [docs/decisions.md](docs/decisions.md), 진행 상황은 [docs/progress.md](docs/progress.md)에 있다.

## 받기와 사용 순서

JDK 17 이상, Git, 코딩 에이전트 하나(Claude Code 또는 Codex)가 필요하다. 명령은 bash와 PowerShell에서 같다.

```bash
git clone https://github.com/MCshin00/spring-msa-handover.git
cd spring-msa-handover
./gradlew assemble
```

예시(HotDealWave)는 결과가 커밋되어 있어 대상 코드 없이 바로 띄운다.

```bash
java -jar build/handover.jar serve --workspace examples/hotdealwave
```

다른 저장소는 이렇게 쓴다.

1. 대상 받기: `java -jar build/handover.jar init --repo <경로 또는 GitHub 주소> --commit <커밋> --workspace <작업 폴더>`. 작업 폴더의 `source/`에 대상을 복제해 그 커밋으로 고정하고, `target.json`과 AI 세션용 지시 파일(`AGENTS.md`, `CLAUDE.md`)을 쓴다. 준 체크아웃은 바꾸지 않는다.
2. 추출: 작업 폴더에서 AI 세션을 열고 "추출해 줘"라고 한다. 세션은 서비스 목록과 서비스 수를 확인한 뒤 "서비스별 하위 에이전트로 동시에"와 "한 세션에서 차례로" 중 무엇으로 할지 사용자에게 묻고 답을 기다린 뒤 고른 대로 한다. 하위 에이전트가 없는 도구에서는 서비스 목록을 나눠 세션을 여러 개 연다고 안내한다. 지시 파일이 순서와 Skill 위치를 알려 주고, 세션은 마칠 때 `verify`를 불러 `out/analysis.json`을 쓴다. Claude Code에서 Skill을 슬래시 명령으로도 쓰려면 이 도구 폴더를 세션에 더한다(`claude --add-dir <도구 폴더>`).
3. 결과 보기: `java -jar <도구 폴더>/build/handover.jar serve --workspace <작업 폴더>`

## 검사

테스트는 예시의 대상 코드가 있어야 한다. 처음 한 번 받는다.

```bash
java -jar build/handover.jar init --repo https://github.com/sparta-final-12th-9700/HotDealWave --commit cc43016 --workspace examples/hotdealwave
./gradlew build
python scripts/check_shared_files.py
python -m unittest discover tests
```

검증기나 예시 결과를 고치면 예시 작업 폴더에서 `java -jar ../../build/handover.jar verify`로 검증 출력을 다시 만들어 함께 커밋한다. 검증기 테스트가 커밋된 출력이 지금 검증기로 다시 만든 것과 같은지 확인한다.

`verify`는 작업 폴더(생략하면 현재 폴더)의 `source/`가 `target.json`의 커밋이 아니거나 바뀐 파일이 있으면 멈춘다. 아니면 `services/*.json`, `relations.json`, `target.json`(대상 이름·고정 커밋)을 스키마로 먼저 검사해 `out/analysis.json`을 만들고 스키마·근거 줄·선언 목록·HTTP 대상·게이트웨이 경로·메시지·핸들러 호출·오류 연결 검사의 경고 수를 출력한다. 경고마다 이유 코드가 붙고, 출력의 `warningCodes`에 코드별 제목과 할 일, `confirmed`에 배열별 "확인됨"의 뜻이 있다. `responses/`에 저장한 응답이 있으면 엔드포인트마다 추출한 응답 필드와 키를 비교해 응답에만 있는 필드, 추출에만 있는 필드, 이름이 다른 필드와 비교 보류를 붙인다. 작업 폴더에 `knowledge/`가 있으면 항목 스키마·근거 줄 존재·영향 ID·서비스 사이 중복을 검사해 `knowledge` 배열과 경고를 같은 출력에 더한다. 디렉터리가 없으면 기존 출력과 같다. 개발자의 확인 상태는 바꾸지 않는다. 결과 구조는 [docs/format/README.md](docs/format/README.md)에 있다.

공유 파일 검사는 Git이 추적하는 파일에 비밀값이나 로컬 절대경로가 있으면 위치를 출력하고 실패한다. 비교 실험 답 원문(`eval/results/`)과 예시의 저장소 수준 결과·검증 출력은 분석 대상이 적어 둔 서버 경로를 인용할 수 있어 홈 경로만 보지 않는다.

### 다른 구성에 맞추려면 고칠 곳

추출은 Spring Boot·Spring Cloud MSA의 흔한 구성을 읽는다. 다른 구성은 아래를 함께 고친다. 읽는 규칙이 없는 방식은 그때까지 서비스 파일의 `unsupported`로 남고, 서비스 페이지와 흐름 결과에 "도구가 다루지 않는 방식"으로 나온다.

| 부분 | 지금 가정 | 고칠 파일 |
|---|---|---|
| 서비스·설정 파일 | 빌드 파일(`build.gradle`, `build.gradle.kts`, `pom.xml`)과 `spring.application.name`이 있는 모듈, `application*.yml`·`yaml`·`properties`, Java 소스 | 추출 Skill 가정 표, `verifier/.../SourceText.java`(근거 파일 종류·모듈 찾기·설정 키) |
| 게이트웨이 | Spring Cloud Gateway의 YAML·Java DSL 경로, `RewritePath`·`StripPrefix`, 앞부분 일치 허용 목록 또는 Security `permitAll` | 추출 Skill, `CrossChecks.java`(경로 필터·토큰 판정), `EvidenceChecks.java`(필터 근거) |
| 내부 호출 | OpenFeign, RestTemplate, RestClient, WebClient, HTTP 인터페이스 | 추출 Skill의 종류별 읽는 법과 검증기의 선언 줄 규칙(`CommunicationRules.java`) |
| 비동기 | Spring Kafka, RabbitMQ, Spring Cloud Stream | 추출 Skill의 종류별 읽는 법과 검증기의 선언 줄 규칙(`CommunicationRules.java`) |
| 오류 응답 | 오류 코드 enum, `@RestControllerAdvice`, 게이트웨이 필터 | 형식 문서와 스키마, 추출 Skill, `EvidenceChecks.java`·`ThrowChecks.java` |
| DTO JSON 이름 | `@JsonProperty`, record, Lombok·getter, public 필드 | 형식 문서, `EvidenceChecks.java`(`jsonName`) |
| 응답 확인 | 게이트웨이로 부르는 JSON API와 토큰 로그인 | 응답 확인 Skill, `ResponseComparison.java` |

Spring 밖 방식(예: gRPC)을 더할 때도 추출 Skill의 종류별 읽는 법과 `CommunicationRules.java`의 종류별 규칙을 고친다. 기존 역할 배열과 서버 흐름을 사용하며, 아직 규칙이 없는 방식은 `unsupported`에 남긴다.

## 서버

```bash
java -jar build/handover.jar serve --workspace examples/hotdealwave
```

검증하지 않고 작업 폴더의 `out/analysis.json`만 읽어 `http://localhost:8080`에 문서 페이지를 띄운다. 대상 코드가 없는 곳에도 이 파일 하나로 띄울 수 있다. 다른 포트는 `--port 8081`처럼 준다. 첫 화면은 서비스·토픽별 개수와 확인 안 된 연결 목록을 보여 준다. 첫 화면에서 서비스(`/services/{이름}`)와 토픽(`/topics/{토픽 환경변수 이름 또는 코드에 적힌 토픽 이름}`)으로 들어가고, 서비스 페이지에서 엔드포인트(`/endpoints/{ID}`)로 들어간다. 배포 파이프라인·컨테이너·모니터링 설정은 `/repository`에 역할별로 있다. 다시 띄우는 데 필요한 것(외부 자원, 채워야 할 환경변수, 저장소 밖 비밀값, 컨테이너, 띄울 때 확인할 연결, 운영 함정, 도구가 다루지 않은 위치)은 `/revival`에 모여 있다. 이 페이지는 다른 페이지의 내용을 모은 것이라 검색·질의응답에는 잡히지 않고, MCP `get_page`로 읽는다. 값마다 확인 상태와 근거 `파일:줄`이 붙고, 확인되지 않은 값은 상태와 이유가 대신 나온다. 근거는 접혀 있고, 펼치면 고정 커밋의 원본 줄이나 저장한 응답 원문(`/responses/{파일}`)의 그 줄로 가는 링크다.

`/api/search?q=찾을 말`은 상위 5개 절의 페이지 주소·절 제목·점수·미리보기를 JSON으로 낸다. `/search`는 같은 검색과 질문을 하는 페이지다.

질문(`/search`의 질문 칸, `POST /api/ask` 본문 `{"question": "..."}`)은 BM25와 임베딩 순위를 합친 검색 상위 8개를 근거로 모델을 한 번 부르고, 인용 검사를 통과하면 답과 인용한 페이지를, 아니면 보류와 이유를 낸다. 모델 키는 `OPENAI_API_KEY` 환경변수로 준다. 질문 한 번이 유료 호출 두 번(질문 임베딩, 답)이다. 첫 질문 때 절마다 임베딩을 만들어 작업 폴더의 `out/embeddings.json`에 두고, 그 뒤에는 본문이 바뀐 절만 다시 만든다.

MCP는 서버를 띄운 채 클라이언트에 등록한다.

```bash
claude mcp add --transport http hotdealwave http://localhost:8080/mcp
```

```bash
codex mcp add hotdealwave --url http://localhost:8080/mcp
```

## 비교 실험

[결정 문서 "비교 실험"](docs/decisions.md#비교-실험)의 세 조건을 질문 파일로 실행한다. 기준선(`base`)·MCP는 Codex CLI가 필요하다. 기준선은 이 저장소 밖에 둔 HotDealWave 고정 커밋 체크아웃을 읽고, MCP는 질문마다 새로 만든 원본 코드가 없는 작업 폴더에서 도구만 쓴다. MCP·질의응답은 서버가 떠 있어야 하고, 질의응답은 유료 모델을 질문마다 한 번 부른다.

```bash
git clone https://github.com/sparta-final-12th-9700/HotDealWave ../HotDealWave-cc43016
git -C ../HotDealWave-cc43016 checkout cc43016b907331a3ce8471475cbf44412e8a6954
python scripts/run_eval.py eval/questions/tuning.csv base --checkout ../HotDealWave-cc43016 --timeout 300
python scripts/run_eval.py eval/questions/tuning.csv mcp --timeout 300
python scripts/run_eval.py eval/questions/tuning.csv ask --timeout 120
python scripts/check_citations.py eval/results/tuning
```

결과는 `eval/results/<질문 파일 이름>/<조건>/<id>.json`(답, 걸린 시간, 토큰, 결과, 부른 MCP 도구)이다. 기계 검사는 답의 `파일:줄`이 고정 커밋(응답 근거는 이 저장소)에 있는지와 같은 답 줄의 코드 이름이 그 줄에 있는지를 `check.csv`에 남긴다. 이름 확인은 참고용이고, 답이 맞는지는 판단하지 않는다. `repo-mcp`는 체크아웃과 MCP를 함께 준 #41 실행 결과이고, 그 스크립트는 `7274f6f`에 있다.

AI 교차 채점은 Codex CLI와 Claude Code CLI가 필요하다. 질문마다 기준선·MCP·질의응답의 답을 섞어 A·B·C로 한 세션에 넘기고, 채점자는 고정 커밋 체크아웃과 응답 근거 복사본만 읽는다.

```bash
python -m scripts.grade_eval grade eval/results/tuning codex --checkout ../HotDealWave-cc43016 --timeout 900 --jobs 2
python -m scripts.grade_eval grade eval/results/tuning opus --checkout ../HotDealWave-cc43016 --timeout 900 --jobs 2
python -m scripts.grade_eval merge eval/results/tuning
```

채점은 `grades/<채점자>/<id>.json`(답 순서, 답마다 정확도·확인 범위·이유·근거)에 남는다. `merge`는 두 채점이 같은 값과 "갈림"을 `grades/grades.csv`에, 갈린 답 전부와 갈리지 않은 답 가운데 무작위 5개를 `grades/review.md`에 쓴다. `grades/judgments.csv`(판정한 칸의 정확도·확인 범위·판정 경로·원인·메모)가 있으면 그 칸은 판정값을, 나머지는 두 채점이 같은 값을 `grades.csv`의 최종 정확도·최종 확인 범위로 쓴다. 판정하지 않은 칸 가운데 맞음이 아닌 칸의 원인은 `grades/causes.csv`에, 판정 기록은 `grades/judge-*.md`에 있다.
