#!/usr/bin/env bash
set -euo pipefail

# 실제 수집기를 쓰되 합성 입력만 전달하고 네트워크를 끈다.
base=$(cd "$(dirname "$0")" && pwd)
candidate=${PROMTAIL_CANDIDATE:-$base/promtail-candidate.yml}
# 학교 VM 은 sudo -n docker, 개발 머신은 docker 를 바로 쓴다. timeout 이 없으면 stdin EOF 로 끝난다.
docker_cmd=(docker)
docker info >/dev/null 2>&1 || docker_cmd=(sudo -n docker)
limit=()
command -v timeout >/dev/null && limit=(timeout 20)
work=$(mktemp -d /tmp/gwangsan-promtail-regression.XXXXXX)
trap 'rm -rf "$work"' EXIT

cat > "$work/header.yml" <<'YAML'
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
cat "$work/header.yml" > "$work/current.yml"
cat >> "$work/current.yml" <<'YAML'
      - multiline:
          firstline: '^\d{4}-\d{2}-\d{2}'
          max_wait_time: 3s
YAML
cat "$work/header.yml" > "$work/candidate.yml"
awk '/^    pipeline_stages:/{found=1; next} found{print}' \
  "$candidate" >> "$work/candidate.yml"

run_case() {
  local config=$1
  local mode=$2
  (
    if [[ "$mode" == agent ]]; then
      # 실제 API 컨테이너의 OTel Java agent stderr 형식. 기동 배너가 flush 된 뒤 며칠 후 exporter 경고가 와도
      # 새 Docker 시각을 써야 하고, 뒤따르는 예외·stack 줄은 그 경고 entry 에 붙어야 한다.
      printf '%s\n' '{"container":"gwangsan-container","timestamp":"2026-10-20T00:00:00Z","message":"[otel.javaagent 2026-10-20 00:00:00:000 +0000] [main] INFO io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 2.33.1"}'
      sleep 7
      printf '%s\n' '{"container":"gwangsan-container","timestamp":"2026-10-26T03:00:00Z","message":"[otel.javaagent 2026-10-26 03:00:00:000 +0000] [BatchSpanProcessor_WorkerThread-1] WARN io.opentelemetry.exporter.internal.http.HttpExporter - AGENT_EXPORT_WARN Failed to export spans."}'
      printf '%s\n' '{"container":"gwangsan-container","timestamp":"2026-10-26T03:00:01Z","message":"java.net.ConnectException: Failed to connect to alloy/172.18.0.5:4318"}'
      printf '%s\n' '{"container":"gwangsan-container","timestamp":"2026-10-26T03:00:01Z","message":"\tat okhttp3.internal.connection.RealConnection.connect(RealConnection.kt:1)"}'
      sleep 8
      exit 0  # 아래 공통 입력은 보내지 않는다(서브셸)
    fi
    if [[ "$mode" != fresh ]]; then
      printf '%s\n' '{"container":"gwangsan-chat","timestamp":"2026-10-20T00:00:00Z","message":"2026-10-20 INFO CHAT_SEED"}'
      # 버퍼를 비운 뒤에도 예전 시각이 남는 경우를 검증한다.
      # flush ticker 가 max_wait_time(3s) 주기라 최악 6s 뒤에 비워진다. 4s 는 간헐적으로 flush 전이었다.
      sleep 7
    fi
    printf '%s\n' '{"container":"gwangsan-chat","timestamp":"2026-10-26T00:00:00Z","message":"[Nest] INFO CHAT_NEW"}'
    printf '%s\n' '{"container":"gwangsan-container","timestamp":"2026-10-26T01:00:00Z","message":"2026-10-26 ERROR API_ERROR"}'
    printf '%s\n' '{"container":"gwangsan-container","timestamp":"2026-10-26T01:00:01Z","message":"    at synthetic.Stack.frame(Stack.java:1)"}'
    if [[ "$mode" == json ]]; then
      # 신형 JSON 로그: 직전 구형 스택트레이스에 붙지 않아야 하고, level 은 본문 단어가 아니라 JSON 필드에서 온다.
      printf '%s\n' '{"container":"gwangsan-container","timestamp":"2026-10-26T02:00:00Z","message":"{\"timestamp\":\"2026-10-26T02:00:00Z\",\"level\":\"INFO\",\"service\":\"gwangsan-api\",\"message\":\"JSON_API_INFO mentions ERROR word\",\"trace_id\":\"4bf92f3577b34da6a3ce929d0e0e4736\",\"span_id\":\"00f067aa0ba902b7\"}"}'
      printf '%s\n' '{"container":"gwangsan-container","timestamp":"2026-10-26T02:00:01Z","message":"{\"timestamp\":\"2026-10-26T02:00:01Z\",\"level\":\"ERROR\",\"service\":\"gwangsan-api\",\"message\":\"JSON_API_ERROR\",\"stack_trace\":\"java.lang.IllegalStateException\\n\\tat x.Y.z(Y.java:1)\"}"}'
      printf '%s\n' '{"container":"gwangsan-chat","timestamp":"2026-10-26T02:00:02Z","message":"{\"timestamp\":\"2026-10-26T02:00:02Z\",\"level\":\"warn\",\"service\":\"gwangsan-chat\",\"message\":\"JSON_CHAT_WARN\",\"trace_id\":\"4bf92f3577b34da6a3ce929d0e0e4736\"}"}'
      printf '%s\n' '{"container":"gwangsan-chat","timestamp":"2026-10-26T02:00:03Z","message":"{\"timestamp\":\"2026-10-26T02:00:03Z\",\"level\":\"verbose\",\"service\":\"gwangsan-chat\",\"message\":\"JSON_CHAT_ODD_LEVEL INFO\"}"}'
    fi
    # multiline flush ticker 는 max_wait_time(3s) 주기라 최악 2배가 걸린다. EOF 전에 flush 되도록 기다린다(없으면 간헐 실패).
    sleep 8
  ) | "${docker_cmd[@]}" run --rm -i --network none --memory 128m --cpus 0.5 \
      -v "$work:/test:ro" grafana/promtail:2.9.0 \
      -config.file="/test/$config.yml" -stdin -dry-run \
      > "$work/$config-$mode.out" 2> "$work/$config-$mode.err" || {
        if [[ ${#limit[@]} -gt 0 ]]; then return 0; fi
        cat "$work/$config-$mode.err" >&2; return 1; }
}

run_case current seeded
run_case current fresh
run_case candidate seeded
run_case candidate json
run_case candidate agent

grep -Eq '^2026-10-20T00:00:00[^[:space:]]*\{.*CHAT_NEW' "$work/current-seeded.out"
grep -Eq '^2026-10-26T00:00:00[^[:space:]]*\{.*CHAT_NEW' "$work/current-fresh.out"
grep -Eq '^2026-10-26T00:00:00[^[:space:]]*\{.*CHAT_NEW' "$work/candidate-seeded.out"
grep -Eq '^2026-10-26T01:00:00[^[:space:]]*\{.*API_ERROR' "$work/candidate-seeded.out"
grep -q '^    at synthetic.Stack.frame' "$work/candidate-seeded.out"
if grep -Eq '^2026-10-26T01:00:01[^[:space:]]*\{' "$work/candidate-seeded.out"; then
  echo 'FAIL: API stack trace was split into a separate entry' >&2
  exit 1
fi
json=$work/candidate-json.out
line() { grep -E "^$1[^[:space:]]*\\{" "$json" || { echo "FAIL: no entry at $1" >&2; cat "$json" >&2; exit 1; }; }
line 2026-10-26T02:00:00 | grep -q 'level="INFO"' || { echo 'FAIL: JSON level INFO not taken from field' >&2; exit 1; }
line 2026-10-26T02:00:01 | grep -q 'level="ERROR"' || { echo 'FAIL: JSON ERROR level' >&2; exit 1; }
line 2026-10-26T02:00:02 | grep -q 'level="WARN"' || { echo 'FAIL: chat warn not normalized to WARN' >&2; exit 1; }
line 2026-10-26T02:00:03 | grep -q 'level="INFO"' || { echo 'FAIL: unknown JSON level did not keep regex fallback' >&2; exit 1; }
# JSON 첫 줄이 앞선 API 스택트레이스 entry 에 합쳐지면 안 된다
if grep -E '^2026-10-26T01:00:00' "$json" | grep -q JSON_API_INFO; then
  echo 'FAIL: JSON line merged into previous multiline entry' >&2; exit 1
fi
if grep -Eq '(trace_id|span_id)=' "$json"; then echo 'FAIL: trace/span id became a label' >&2; exit 1; fi
labels=$(grep -oE '\{[^}]*\}' "$json" | grep -oE '[a-z_]+=' | sort -u | tr -d '=' | tr '\n' ' ')
[[ "$labels" == "container level " ]] || { echo "FAIL: unexpected label set: $labels" >&2; exit 1; }
agent=$work/candidate-agent.out
grep -Eq '^2026-10-26T03:00:00[^[:space:]]*\{.*AGENT_EXPORT_WARN' "$agent" || {
  echo 'FAIL: agent exporter warning lost its new Docker timestamp (merged into old flushed entry?)' >&2; cat "$agent" >&2; exit 1; }
grep -q '^java.net.ConnectException' "$agent" && grep -q 'RealConnection.connect' "$agent" || {
  echo 'FAIL: agent warning exception/stack lines missing' >&2; cat "$agent" >&2; exit 1; }
if grep -Eq '^2026-10-26T03:00:01[^[:space:]]*\{' "$agent"; then
  echo 'FAIL: agent stack lines split from the warning entry' >&2; exit 1
fi
printf '%s\n' 'PASS: current pipeline reuses old chat timestamp after flush' \
  'PASS: fresh unmatched chat stream passes through (mismatch alone is insufficient)' \
  'PASS: candidate preserves new chat timestamp and API multiline stack trace' \
  'PASS: JSON API/chat lines are separate entries, level from JSON field (warn->WARN, unknown->fallback), no trace labels' \
  'PASS: otel.javaagent stderr warning after flush keeps new timestamp and its stack lines'
