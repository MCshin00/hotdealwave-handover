# 개발

도구를 고치는 사람을 위한 검사·검증 출력·비교 실험 안내다. 쓰는 법은 [README](../README.md), 결정과 이유는 [결정 기록](decisions.md), 진행 상황은 [진행 상황](progress.md)에 있다.

## 검사

테스트는 예시의 대상 코드가 있어야 한다. 처음 한 번 받는다.

```bash
java -jar build/handover.jar init --repo https://github.com/sparta-final-12th-9700/HotDealWave --commit cc43016 --workspace examples/hotdealwave
./gradlew build
python scripts/check_shared_files.py
python -m unittest discover tests
```

검증기나 예시 결과를 고치면 예시 작업 폴더에서 `java -jar ../../build/handover.jar verify`로 검증 출력을 다시 만들어 함께 커밋한다. 검증기 테스트가 커밋된 출력이 지금 검증기로 다시 만든 것과 같은지 확인한다.

공유 파일 검사는 Git이 추적하는 파일에 비밀값이나 로컬 절대경로가 있으면 위치를 출력하고 실패한다. 비교 실험 답 원문(`eval/results/`)과 예시의 저장소 수준 결과·검증 출력은 분석 대상이 적어 둔 서버 경로를 인용할 수 있어 홈 경로만 보지 않는다.

## verify가 하는 일

`verify`는 작업 폴더(생략하면 현재 폴더)의 `source/`가 `target.json`의 커밋이 아니거나 바뀐 파일이 있으면 멈춘다. `relations.json`이 없으면 관계 단계를 먼저 하라고 하고 멈춘다. 아니면 `services/*.json`, `relations.json`, `target.json`(대상 이름·고정 커밋)을 스키마로 먼저 검사해 `out/analysis.json`을 만들고 스키마·근거 줄·선언 목록·HTTP 대상·게이트웨이 경로·메시지·핸들러 호출·오류 연결 검사의 경고 수를 출력한다.

- 경고마다 이유 코드가 붙고, 출력의 `warningCodes`에 코드별 제목과 할 일, `confirmed`에 배열별 "확인됨"의 뜻이 있다.
- `responses/`에 저장한 응답이 있으면 엔드포인트마다 추출한 응답 필드와 키를 비교해 응답에만 있는 필드, 추출에만 있는 필드, 이름이 다른 필드와 비교 보류를 붙인다.
- 작업 폴더에 `knowledge/`가 있으면 항목 스키마·근거 줄 존재·영향 ID·서비스 사이 중복을 검사해 `knowledge` 배열과 경고를 같은 출력에 더한다. 개발자의 확인 상태는 바꾸지 않는다.

결과 구조는 [형식 문서](format/README.md)에 있다.

## 비교 실험

[결정 기록 "비교 실험"](decisions.md#비교-실험)의 세 조건을 질문 파일로 실행한다. 기준선(`base`)·MCP는 Codex CLI가 필요하다. 기준선은 이 저장소 밖에 둔 HotDealWave 고정 커밋 체크아웃을 읽고, MCP는 질문마다 새로 만든 원본 코드가 없는 작업 폴더에서 도구만 쓴다. MCP·질의응답은 서버가 떠 있어야 하고, 질의응답은 유료 모델을 질문마다 부른다.

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
