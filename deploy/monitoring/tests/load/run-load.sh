#!/usr/bin/env bash
# 같은 격리 스택·같은 워크로드로 baseline 과 instrumented 를 번갈아 측정한다.
#   MODE=baseline     API_JAR=<base jar>  CHAT_DIR=<base chat build>
#   MODE=instrumented API_JAR=<integrated jar> OTEL_AGENT=<agent jar> CHAT_DIR=<integrated chat build>
#   CLIENT_DIR=<socket.io-client 가 설치된 chat 체크아웃> (기본 CHAT_DIR)
#   MODE=instrumented-off  위와 같지만 OTEL_SDK_DISABLED=true (롤백 스위치 비용 확인)
# 시나리오: CLIENTS="10 50 100" RATE=1 DURATION=60 WARMUP=10 REPEATS=3 OUT=<dir>
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
: "${MODE:?}" "${API_JAR:?}" "${CHAT_DIR:?}" "${OUT:?}"
CLIENTS=${CLIENTS:-10} RATE=${RATE:-1} DURATION=${DURATION:-60} WARMUP=${WARMUP:-10} REPEATS=${REPEATS:-1}
export API_JAR CHAT_DIR
case "$MODE" in
  baseline) export CHAT_CMD="node dist/src/main.js" OTEL_SDK_DISABLED=true API_JAVA_TOOL_OPTIONS= CHAT_METRICS_PORT=off ;;
  instrumented|instrumented-off)
    : "${OTEL_AGENT:?}"; export OTEL_AGENT
    export CHAT_CMD="node --require ./dist/src/instrumentation.js dist/src/main.js"
    export API_JAVA_TOOL_OPTIONS="-javaagent:/app/opentelemetry-javaagent.jar" CHAT_METRICS_PORT=9464
    if [[ $MODE == instrumented ]]; then export OTEL_SDK_DISABLED=false; else export OTEL_SDK_DISABLED=true; fi ;;
  *) echo "unknown MODE $MODE" >&2; exit 2 ;;
esac
mkdir -p "$OUT"
dc() { docker compose -p gw-load -f "$here/compose.load.yaml" "$@"; }
trap 'dc logs --no-color api chat > "$OUT/$MODE-stack.log" 2>&1 || true; dc down -v >/dev/null 2>&1 || true' EXIT
dc down -v >/dev/null 2>&1 || true
dc up -d
for _ in $(seq 180); do curl -fs http://127.0.0.1:28080/ >/dev/null 2>&1 || [[ $(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:28080/) =~ ^[2-4] ]] && break; sleep 1; done
for _ in $(seq 60); do (exec 3<>/dev/tcp/127.0.0.1/23000) 2>/dev/null && break; sleep 1; done
{
  echo "mode=$MODE clients=$CLIENTS rate=$RATE duration=$DURATION warmup=$WARMUP repeats=$REPEATS"
  echo "api_jar_sha256=$(shasum -a 256 "$API_JAR" | cut -d' ' -f1)"
  [[ -n ${OTEL_AGENT:-} ]] && echo "agent_sha256=$(shasum -a 256 "$OTEL_AGENT" | cut -d' ' -f1)"
  echo "chat_main_sha256=$(shasum -a 256 "$CHAT_DIR/dist/src/main.js" | cut -d' ' -f1)"
  docker compose -p gw-load -f "$here/compose.load.yaml" images
  docker info --format 'docker_cpus={{.NCPU}} docker_mem={{.MemTotal}}'
} > "$OUT/$MODE-env.txt"
for c in $CLIENTS; do
  node "$here/chat-load.mjs" --label "$MODE" --clients "$c" --rate "$RATE" --duration "$DURATION" \
    --warmup "$WARMUP" --repeats "$REPEATS" --out "$OUT" --chat-dir "${CLIENT_DIR:-$CHAT_DIR}"
  [[ $MODE == instrumented ]] && curl -fs http://127.0.0.1:29091/actuator/prometheus > "$OUT/$MODE-c$c-api-metrics.txt" || true
  [[ $MODE == instrumented ]] && curl -fs http://127.0.0.1:29464/metrics > "$OUT/$MODE-c$c-chat-metrics.txt" || true
done
