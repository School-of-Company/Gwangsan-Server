#!/usr/bin/env bash
# 준비한 Loki TLS·경로별 인증 프록시를 격리 Loki 2.9.0 앞에서 검증한다. 운영 Loki 미접속.
# 사용: bash loki-hardening/test-proxy.sh
set -uo pipefail
conf=$(cd "$(dirname "$0")" && pwd)/nginx.conf
T=$(mktemp -d); mkdir -p $T/tls
openssl req -x509 -newkey rsa:2048 -nodes -days 1 -subj /CN=ssh.gsmsv.site -addext subjectAltName=DNS:ssh.gsmsv.site -keyout $T/tls/server.key -out $T/tls/server.crt 2>/dev/null
printf 'gwangsan-promtail:%s\n' "$(openssl passwd -apr1 pushpw)" > $T/htpasswd.push
printf 'gwangsan-grafana:%s\n' "$(openssl passwd -apr1 querypw)" > $T/htpasswd.query
chmod -R a+rX $T
docker network create lh-net >/dev/null
docker run -d --name lh-loki --network lh-net grafana/loki:2.9.0 -config.file=/etc/loki/local-config.yaml >/dev/null
docker run -d --name lh-nginx --network container:lh-loki -v $conf:/etc/nginx/nginx.conf:ro -v $T:/etc/gwangsan-loki:ro nginx:1.28-alpine >/dev/null
sleep 15
c() { docker run --rm --network container:lh-loki -v $T/tls:/ca:ro curlimages/curl:8.11.1 -s -o /dev/null -w '%{http_code}' --cacert /ca/server.crt --resolve ssh.gsmsv.site:26129:127.0.0.1 "$@"; }
U=https://ssh.gsmsv.site:26129
B="{\"streams\":[{\"stream\":{\"container\":\"t\"},\"values\":[[\"$(date +%s)000000000\",\"x\"]]}]}"
H='Content-Type: application/json'
echo "push 무인증: $(c -X POST -H "$H" -d "$B" $U/loki/api/v1/push) (기대 401)"
echo "push 조회계정: $(c -u gwangsan-grafana:querypw -X POST -H "$H" -d "$B" $U/loki/api/v1/push) (기대 401)"
echo "push 전송계정: $(c -u gwangsan-promtail:pushpw -X POST -H "$H" -d "$B" $U/loki/api/v1/push) (기대 204)"
echo "조회 전송계정: $(c -u gwangsan-promtail:pushpw $U/loki/api/v1/labels) (기대 401)"
echo "조회 조회계정: $(c -u gwangsan-grafana:querypw $U/loki/api/v1/labels) (기대 200)"
echo "/config 관리: $(c -u gwangsan-grafana:querypw $U/config) (기대 404)"
echo "평문 HTTP: $(docker run --rm --network container:lh-loki curlimages/curl:8.11.1 -s -o /dev/null -w '%{http_code}' http://127.0.0.1:26129/ready) (기대 400: TLS 포트에 평문)"
docker rm -f lh-loki lh-nginx >/dev/null; docker network rm lh-net >/dev/null; rm -rf $T
