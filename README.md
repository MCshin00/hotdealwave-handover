# HotDealWave 인수인계 도구

팀원이 모두 떠나고 저장소만 남은 마이크로서비스 프로젝트를 새 사람이 넘겨받는 상황을 가정한다. 이 도구는 팀 프로젝트 [HotDealWave](https://github.com/sparta-final-12th-9700/HotDealWave)(Spring Boot, Spring Cloud, Kafka)의 고정 커밋을 정적으로 분석해 API, 서비스 간 호출, 이벤트 흐름, 설정을 뽑고, 그 결과를 서버 하나로 제공한다.

## 하는 일

```
target/HotDealWave --> extractor --> out/analysis.json --> server --+--> 문서 페이지      사람이 읽는다 (업무 규칙·함정 포함)
   @cc43016             (Java)                         (Spring Boot) +--> 검색·질문 페이지  사람이 묻고, 고정 RAG가 답한다
                                                                     +--> /mcp              Claude Code·Codex가 도구로 부른다
```

모든 결과에는 근거 `파일:줄`과 상태가 붙는다. 저장소만으로 확정할 수 없는 연결은 추측하지 않는다.

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
+-- extractor/              # 추출기 (Java 17, JavaParser)
|                           #   REST·Feign·Kafka·설정 -> out/analysis.json, 교차 검증 경고
|
+-- server/                 # Spring Boot 서버 하나
|                           #   조회: 관계 조회 3개 + search_docs(BM25), 모델 호출 없음
|                           #   페이지: 문서·검색·질문 (Thymeleaf)
|                           #   /api/search, /api/ask(고정 RAG), /mcp(Streamable HTTP)
|
+-- knowledge/              # 업무 규칙·함정 (AI 해석과 판정 결과, 확인 상태 포함)
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

## MCP 도구 (예정)

| 도구 | 하는 일 |
|---|---|
| `find_endpoint` | 경로·이름으로 엔드포인트와 요청·응답 필드를 찾는다 |
| `trace_flow` | 엔드포인트나 이벤트에서 시작해 Feign 호출·Kafka 발행·구독을 따라간다 |
| `find_subscribers` | 토픽 환경변수 이름으로 발행자와 구독자를 찾는다 |
| `search_docs` | 문서 페이지를 키워드로 검색한다 (BM25) |

결정과 이유는 [docs/decisions.md](docs/decisions.md), 진행 상황은 [docs/progress.md](docs/progress.md)에 있다.

## 받기

```bash
git clone --recurse-submodules https://github.com/MCshin00/hotdealwave-handover.git
```

이미 받았다면 `git submodule update --init`으로 분석 대상을 받는다.

## 검사

```bash
python scripts/check_shared_files.py
```

커밋된 파일에 비밀값이나 로컬 절대경로가 있으면 위치를 출력하고 실패한다. 서버 실행과 테스트 명령은 각 단계에서 더한다.
