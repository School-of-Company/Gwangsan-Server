#!/usr/bin/env bash
# 실제 aws/config.alloy 메트릭 경로 검증(로컬 자체 수신기). 학교/운영 Prometheus 로 보내지 않는다.
#   API_JAR=… OTEL_AGENT=… CHAT_DIR=… CLIENT_DIR=… OUT=… [GAP_SECONDS=75] [KEEP=1] ./run-metrics.sh
# KEEP=1 이면 검증 뒤 스택을 남긴다(정리: docker compose -p gw-metrics -f compose.metrics.yaml down -v).
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
: "${API_JAR:?}" "${OTEL_AGENT:?}" "${CHAT_DIR:?}" "${OUT:?}"
export API_JAR OTEL_AGENT CHAT_DIR
CLIENT_DIR=${CLIENT_DIR:-$CHAT_DIR} GAP_SECONDS=${GAP_SECONDS:-75}
# PROM_QUERY: 검증 쿼리를 보낼 Prometheus(원격이 C sandbox 면 http://127.0.0.1:19090)
prom=${PROM_QUERY:-http://127.0.0.1:39090}
dc() { docker compose -p gw-metrics -f "$here/compose.metrics.yaml" "$@"; }
mkdir -p "$OUT"
cleanup() {
  dc logs --no-color alloy > "$OUT/alloy.log" 2>&1 || true
  dc logs --no-color api chat > "$OUT/apps.log" 2>&1 || true
  [[ ${KEEP:-0} == 1 ]] || dc down -v >/dev/null 2>&1 || true
}
trap cleanup EXIT
dc down -v >/dev/null 2>&1 || true
dc up -d
up() { curl -fsG "$prom/api/v1/query" --data-urlencode "query=up{job=\"$1\",environment=\"test\"}==1" | grep -q '"value"'; }
for _ in $(seq 240); do up gwangsan-api && up gwangsan-chat && break; sleep 2; done
up gwangsan-api && up gwangsan-chat || { echo 'targets never became up' >&2; exit 1; }
load() { node "$here/../load/chat-load.mjs" --project gw-metrics --url http://127.0.0.1:33000 --label "$1" \
  --clients 5 --rate 1 --duration "$2" --warmup 0 --repeats 1 --out "$OUT" --chat-dir "$CLIENT_DIR"; }
# 부하 결과에 오류·미응답·미커밋이 있으면 실패로 남긴다(조용히 통과시키지 않는다).
load_ok() { python3 - "$OUT/load-$1-c5-r1-d$2x1.json" <<'PY'
import json, sys
r = json.load(open(sys.argv[1]))["repeats"][0]
bad = {k: r[k] for k in ("socketErrors", "unansweredEchoes", "uncommitted") if r[k]}
print(json.dumps({"file": sys.argv[1].rsplit("/", 1)[-1], "emitted": r["emitted"], "committed": r["committed"], **bad}))
sys.exit(1 if bad or r["committed"] != r["emitted"] else 0)
PY
}
load metrics-steady 60
load_ok metrics-steady 60 || load_rc=1
sleep 45
python3 "$here/verify.py" "$prom" "$OUT" steady || steady_rc=$?
# 원격 경로(relay)만 멈추고(앱·Alloy scrape·수신기는 계속) WAL 재전송으로 구간이 채워지는지 본다.
load metrics-gap $((GAP_SECONDS + 60)) > "$OUT/gap-load.out" 2>&1 &
lp=$!
sleep 20
g0=$(date +%s); docker pause gw-metrics-relay-1 >/dev/null
sleep "$GAP_SECONDS"
g1=$(date +%s)
python3 "$here/verify.py" "$prom" "$OUT" rawcount "$g0" "$g1" || gap_rc=1
docker unpause gw-metrics-relay-1 >/dev/null
wait $lp || load_rc=1
load_ok metrics-gap $((GAP_SECONDS + 60)) || load_rc=1
sleep 60
python3 "$here/verify.py" "$prom" "$OUT" gap "$g0" "$g1" || gap_rc=$?
restarts=$(docker inspect -f '{{.RestartCount}}' gw-metrics-alloy-1)
oom=$(docker inspect -f '{{.State.OOMKilled}}' gw-metrics-alloy-1)
running=$(docker inspect -f '{{.State.Running}}' gw-metrics-alloy-1)
[[ $restarts == 0 && $oom == false && $running == true ]] || { echo "FAIL alloy restarts=$restarts oom=$oom running=$running" >&2; alloy_rc=1; }
{
  echo "alloy_restarts=$(docker inspect -f '{{.RestartCount}}' gw-metrics-alloy-1) oom=$(docker inspect -f '{{.State.OOMKilled}}' gw-metrics-alloy-1)"
  docker stats --no-stream --format '{{.Name}} {{.CPUPerc}} {{.MemUsage}}' gw-metrics-alloy-1 gw-metrics-api-1 gw-metrics-chat-1
  echo "config_alloy_sha256=$(shasum -a 256 "$here/../../aws/config.alloy" | cut -d' ' -f1)"
  echo "api_jar_sha256=$(shasum -a 256 "$API_JAR" | cut -d' ' -f1) gap=${g0}-${g1}"
} > "$OUT/runtime.txt"
cat "$OUT/runtime.txt"
echo "steady_rc=${steady_rc:-0} gap_rc=${gap_rc:-0} load_rc=${load_rc:-0} alloy_rc=${alloy_rc:-0}"
exit $(( ${steady_rc:-0} | ${gap_rc:-0} | ${load_rc:-0} | ${alloy_rc:-0} ))
