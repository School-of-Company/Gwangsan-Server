#!/usr/bin/env bash
set -euo pipefail

# 메트릭 추가 이후 구성. 기존 컨테이너는 자동 교체하지 않는다(운영 문서의 교체 절차 참고).
base=/home/ubuntu/gwangsan-telemetry
sudo -n docker network inspect gwangsan-observability >/dev/null
sudo -n systemctl is-active --quiet gwangsan-tempo-tunnel.service
sudo -n systemctl is-active --quiet gwangsan-metrics-tunnel.service
test -f "$base/alloy/config.alloy"
if sudo -n docker container inspect gwangsan-alloy >/dev/null 2>&1; then
  echo 'gwangsan-alloy already exists; inspect it before recreating' >&2
  exit 1
fi

# 선택 비밀 파일(DB/Redis exporter). 없으면 db-redis.alloy도 두지 않는다.
# 비밀 파일은 owner 65534, mode 400, 디렉터리 500(65534)이라 ubuntu로는 읽을 수 없다. 존재는 sudo로,
# 읽기 가능 여부는 실제 컨테이너 UID(65534)로 확인한다. validate는 비밀 파일을 읽지 않기 때문이다.
image=grafana/alloy:v1.21.2@sha256:89a5a320168387336b9ac3c850609c3ba7757eadd4ae5f1d819deb35e9854b18
secret_mount=()
if [[ -f "$base/alloy/db-redis.alloy" ]]; then
  sudo -n test -f "$base/secrets/mysql_exporter_dsn"
  sudo -n test -f "$base/secrets/redis_exporter_password"
  secret_mount=(-v "$base/secrets:/run/secrets:ro")
  sudo -n docker run --rm --network none --user 65534:65534 --read-only "${secret_mount[@]}" \
    --entrypoint sh "$image" -c 'test -r /run/secrets/mysql_exporter_dsn && test -r /run/secrets/redis_exporter_password'
fi

# 실행과 같은 읽기 전용 마운트·사용자로 검증한다(선택 exporter의 비밀 파일 경로 포함).
sudo -n docker run --rm --network none --memory 320m --cpus 0.5 \
  --user 65534:65534 --read-only \
  --env-file "$base/alloy.env" \
  -v "$base/alloy:/etc/alloy:ro" "${secret_mount[@]}" \
  "$image" validate /etc/alloy

sudo -n docker run -d --name gwangsan-alloy \
  --network gwangsan-observability --network-alias alloy \
  --restart unless-stopped --memory 320m --cpus 0.5 \
  --user 65534:65534 --cap-drop ALL --security-opt no-new-privileges \
  --read-only --tmpfs /tmp:rw,noexec,nosuid,size=64m \
  --log-driver json-file --log-opt max-size=10m --log-opt max-file=3 \
  --env-file "$base/alloy.env" -e GOMEMLIMIT=180MiB \
  -p 127.0.0.1:4318:4318 -p 127.0.0.1:12345:12345 \
  -v "$base/alloy:/etc/alloy:ro" "${secret_mount[@]}" \
  "$image" run --server.http.listen-addr=0.0.0.0:12345 \
  --storage.path=/tmp/alloy /etc/alloy
