#!/usr/bin/env bash
# 합성 OTLP span 1개를 Alloy(테스트 config)→Tempo 3.1.1 로 보내 /api/traces 로 조회한다.
# 앱 없이 수집 경로만 검증한다. 네트워크/컨테이너는 끝나면 지운다.
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
net=gw-trace-smoke-$$
trap 'docker rm -f $net-tempo $net-alloy >/dev/null 2>&1; docker network rm $net >/dev/null 2>&1' EXIT
docker network create $net >/dev/null
docker run -d --name $net-tempo --network $net --network-alias tempo -p 127.0.0.1::3200 \
  -v "$here/../tempo.yml:/etc/tempo/tempo.yml:ro" grafana/tempo:3.1.1 -config.file=/etc/tempo/tempo.yml -target=all >/dev/null
docker run -d --name $net-alloy --network $net -p 127.0.0.1::4318 -e TEMPO_TUNNEL_ENDPOINT=http://tempo:4318 \
  -v "$here/alloy-test.alloy:/etc/alloy/config.alloy:ro" grafana/alloy:v1.21.2 run /etc/alloy/config.alloy >/dev/null
tempo=http://$(docker port $net-tempo 3200 | head -1)
alloy=http://$(docker port $net-alloy 4318 | head -1)
for _ in $(seq 60); do curl -fs "$tempo/ready" >/dev/null 2>&1 && break; sleep 1; done
trace=$(openssl rand -hex 16); span=$(openssl rand -hex 8); now=$(date +%s)000000000
body='{"resourceSpans":[{"resource":{"attributes":[{"key":"service.name","value":{"stringValue":"gwangsan-smoke"}}]},"scopeSpans":[{"spans":[{"traceId":"'$trace'","spanId":"'$span'","name":"smoke","kind":1,"startTimeUnixNano":"'$now'","endTimeUnixNano":"'$now'"}]}]}]}'
for _ in $(seq 30); do curl -fs -XPOST -H 'content-type: application/json' "$alloy/v1/traces" -d "$body" >/dev/null 2>&1 && break; sleep 1; done
for _ in $(seq 60); do
  if curl -fs "$tempo/api/v2/traces/$trace" | grep -q gwangsan-smoke; then echo "PASS trace $trace visible via Alloy->Tempo"; exit 0; fi
  sleep 1
done
echo "FAIL trace $trace not found"; docker logs --tail 30 $net-alloy; docker logs --tail 30 $net-tempo; exit 1
