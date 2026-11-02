#!/usr/bin/env bash
set -euo pipefail

# promtail-candidate.yml의 JSON 로그 대응 회귀 테스트. 네트워크·Docker socket·운영 Loki 미사용.
# test-promtail-pipeline.sh와 같은 방식: 합성 Docker 수집 결과(JSON)를 실제 Promtail 2.9.0 -dry-run에 넣는다.
base=$(cd "$(dirname "$0")/.." && pwd)
work=$(mktemp -d "${TMPDIR:-/tmp}/gwangsan-promtail-json.XXXXXX")
trap 'rm -rf "$work"' EXIT
docker=${DOCKER:-docker}

cat > "$work/candidate.yml" <<'YAML'
server:
  http_listen_port: 0
  grpc_listen_port: 0
positions:
  filename: /tmp/positions.yaml
clients:
  - url: http://127.0.0.1:1/loki/api/v1/push
scrape_configs:
  - job_name: fixture
    pipeline_stages:
      - json:
          expressions:
            timestamp: timestamp
            message: message
            container: container
      - labels:
          container:
      - timestamp:
          source: timestamp
          format: RFC3339Nano
      - output:
          source: message
YAML
awk '/^    pipeline_stages:/{found=1; next} found{print}' \
  "$base/promtail-candidate.yml" >> "$work/candidate.yml"

line() { # container timestamp message(JSON 문자열로 인코딩)
  python3 -c 'import json,sys; print(json.dumps({"container":sys.argv[1],"timestamp":sys.argv[2],"message":sys.argv[3]}))' "$@"
}
tid=4bf92f3577b34da6a3ce929d0e0e4736
{
  line gwangsan-container 2026-10-27T01:00:00Z '2026-10-27 ERROR LEGACY_API_ERROR'
  line gwangsan-container 2026-10-27T01:00:01Z '    at synthetic.Stack.frame(Stack.java:1)'
  line gwangsan-container 2026-10-27T01:00:02Z "{\"timestamp\":\"2026-10-27T01:00:02Z\",\"level\":\"INFO\",\"service\":\"gwangsan-api\",\"message\":\"JSON_API_INFO mentions ERROR word\",\"trace_id\":\"$tid\",\"span_id\":\"00f067aa0ba902b7\"}"
  line gwangsan-container 2026-10-27T01:00:02.5Z '[otel.javaagent 2026-10-27 01:00:02:500 +0000] [OkHttp http://alloy:4318/...] WARN io.opentelemetry.exporter.internal.http.HttpExporter - SYNTHETIC_AGENT_WARN'
  line gwangsan-chat 2026-10-27T01:00:03Z "{\"timestamp\":\"2026-10-27T01:00:03Z\",\"level\":\"warn\",\"service\":\"gwangsan-chat\",\"message\":\"JSON_CHAT_WARN\",\"trace_id\":\"$tid\"}"
  line gwangsan-chat 2026-10-27T01:00:04Z '{"level":"30","message":"JSON_CHAT_NUMERIC_LEVEL"}'
  line gwangsan-chat 2026-10-27T01:00:05Z '[Nest] 1  - 10/27/2026, 10:00:05 AM     LOG LEGACY_NEST_LINE'
} | $docker run --rm -i --network none --memory 128m --cpus 0.5 \
    -v "$work:/test:ro" grafana/promtail:2.9.0 \
    -config.file=/test/candidate.yml -stdin -dry-run > "$work/out" 2> "$work/err" || true
# 마지막 multiline flush 대기는 stdin EOF가 처리한다.

fail() { echo "FAIL: $1" >&2; cat "$work/out" >&2; exit 1; }
entry() { grep -F "$1" "$work/out" | head -1; }

e=$(entry JSON_API_INFO) || true
[[ -n "$e" ]] || fail 'JSON API 줄 누락'
[[ "$e" == 2026-10-27T01:00:02* ]] || fail 'JSON API 줄이 이전 스택트레이스에 합쳐짐(시각 재사용)'
[[ "$e" == *'level="INFO"'* ]] || fail 'JSON level 필드 대신 본문 단어로 level 추출'
[[ "$e" != *trace_id=* && "$e" != *span_id=* ]] || fail 'trace_id/span_id가 라벨이 됨'
[[ "$e" == *"\"trace_id\":\"$tid\""* ]] || fail 'trace_id 본문 보존 실패'

e=$(entry LEGACY_API_ERROR)
[[ "$e" == 2026-10-27T01:00:00* && "$e" == *'level="ERROR"'* ]] || fail '구형 API 오류 처리 변경'
grep -q '^    at synthetic.Stack.frame' "$work/out" || fail '구형 스택트레이스가 오류 항목에 붙지 않음'
if grep -Eq '^2026-10-27T01:00:01[^[:space:]]*' "$work/out"; then fail '스택 프레임이 별도 항목으로 분리'; fi

e=$(entry SYNTHETIC_AGENT_WARN)
[[ "$e" == 2026-10-27T01:00:02.5* ]] || fail 'Java agent stderr 줄이 앞 항목에 합쳐짐(시각 재사용)'

e=$(entry JSON_CHAT_WARN)
[[ "$e" == 2026-10-27T01:00:03* && "$e" == *'level="WARN"'* && "$e" == *'container="gwangsan-chat"'* ]] || fail '채팅 JSON warn → WARN 정규화 실패'

e=$(entry JSON_CHAT_NUMERIC_LEVEL)
[[ "$e" != *level=* ]] || fail '허용 밖 level 값이 라벨이 됨'

e=$(entry LEGACY_NEST_LINE)
[[ "$e" == 2026-10-27T01:00:05* && "$e" != *level=* ]] || fail '구형 Nest 줄 처리 변경'

printf '%s\n' \
  'PASS: API JSON 줄은 독립 항목, 자체 시각 유지, level은 JSON 필드 기준' \
  'PASS: 구형 API 오류+스택트레이스 한 항목 유지(기존 동작)' \
  'PASS: 채팅 JSON warn→WARN, 허용 밖 level은 라벨 미생성' \
  'PASS: trace_id/span_id는 본문 필드로만 보존(라벨 아님)' \
  'PASS: 구형 Nest 줄 처리 불변' \
  'PASS: OTel Java agent stderr 줄은 독립 항목·자체 시각'
