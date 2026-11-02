"""저장 응답의 키를 추출 response 구조와 비교한다. 파일은 수정하지 않는다."""
import argparse
import json
from pathlib import Path
import re


def normalized_name(name):
    """is + 대문자 접두사, 밑줄, 대소문자만 정리한다."""
    return re.sub(r"^is(?=[A-Z])", "", name).replace("_", "").casefold()


def compare(schema, samples, path="$"):
    """같은 배열 위치의 객체들은 관측한 키의 합집합으로 비교한다."""
    samples = [sample for sample in samples if sample is not None]
    if not samples:
        return [f"비교 보류: {path} (본문 또는 값이 null)"]

    kind = schema["kind"]
    fields = schema.get("fields", [])
    arguments = schema.get("arguments", [])
    if kind in ("container", "array") and not fields:
        if len(arguments) != 1 or not all(isinstance(sample, list) for sample in samples):
            return [f"비교 보류: {path} (추출 인자와 응답 배열을 대응할 수 없음)"]
        elements = [element for sample in samples for element in sample]
        if not elements:
            return [f"비교 보류: {path}[] (빈 배열)"]
        return compare(arguments[0], elements, path + "[]")

    if kind in ("scalar", "enum"):
        return []  # 값·타입 검사가 아니라 필드 이름 비교다.
    if not fields:
        return [f"비교 보류: {path} (추출에 내부 구조 없음: {schema['type']})"]
    if not all(isinstance(sample, dict) for sample in samples):
        return [f"비교 보류: {path} (추출 객체와 응답 구조가 다름)"]

    expected = {field["jsonName"]["value"]: field["schema"] for field in fields
                if field["jsonName"]["value"] is not None}
    actual = set().union(*(sample.keys() for sample in samples))
    extra = actual - expected.keys()
    missing = expected.keys() - actual
    differences = []
    if any(field["jsonName"]["value"] is None for field in fields):
        differences.append(f"비교 보류: {path} (JSON 이름이 미확정인 추출 필드 있음)")

    # 같은 부모 안에서 정규화 이름이 유일하게 짝지어질 때만 이름 차이로 낸다.
    renamed = {}
    for name in sorted(extra):
        matches = [old for old in missing if normalized_name(name) == normalized_name(old)]
        if len(matches) == 1 and sum(normalized_name(other) == normalized_name(name) for other in extra) == 1:
            renamed[name] = matches[0]
    for name, old in renamed.items():
        differences.append(f"이름 다름: {path}.{name} ↔ {path}.{old}")
    for name in sorted(extra - renamed.keys()):
        differences.append(f"응답에만 있음: {path}.{name}")
    for name in sorted(missing - set(renamed.values())):
        differences.append(f"추출에만 있음: {path}.{name}")

    for name in sorted(actual & expected.keys() | renamed.keys()):
        child = expected[renamed.get(name, name)]
        # null인 스칼라는 키가 관측되었으므로 일치한다. 객체 내부는 관측할 수 없다.
        if child["kind"] in ("scalar", "enum"):
            continue
        differences.extend(compare(child, [sample[name] for sample in samples if name in sample],
                                   path + "." + name))
    return differences


def compare_files(extracted, responses):
    endpoints = []
    for service_file in sorted((extracted / "services").glob("*.json")):
        endpoints.extend(json.loads(service_file.read_text(encoding="utf-8"))["endpoints"])
    for endpoint in endpoints:
        endpoint_id = endpoint["id"]
        filename = responses / (endpoint_id + ".json")
        if not filename.is_file():
            print(f"[{endpoint_id}] 비교 보류: 응답 파일 없음")
            continue
        record = json.loads(filename.read_text(encoding="utf-8"))
        status = record["statusCode"]
        if status is None:
            print(f"[{endpoint_id}] 미호출: {record['reason']}")
            continue
        if not 200 <= status < 300:
            print(f"[{endpoint_id}] 비교 보류: HTTP {status} (정상 응답과 비교하지 않음)")
            continue
        differences = compare(endpoint["response"], [record["body"]])
        for difference in differences or ["차이 없음"]:
            print(f"[{endpoint_id}] {difference}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("extracted", type=Path, help="추출 결과 디렉터리 (services/*.json)")
    parser.add_argument("responses", type=Path, help="엔드포인트별 응답 디렉터리")
    args = parser.parse_args()
    if not args.responses.is_dir():
        parser.error("responses 디렉터리가 없습니다")
    try:
        compare_files(args.extracted, args.responses)
    except (OSError, ValueError, KeyError, TypeError) as error:
        parser.error(f"입력 파일 형식 또는 읽기 오류: {error}")


if __name__ == "__main__":
    main()
