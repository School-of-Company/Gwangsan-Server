#!/usr/bin/env bash
set -euo pipefail

# 로컬 격리 sandbox. 운영 Loki/AWS/학교 VM에 연결하지 않는다.
#   sandbox/run.sh up       설정 생성(.run/) 후 기동
#   sandbox/run.sh verify [증거 디렉터리] [--mode accept|diagnostic]  쿼리·링크·경보 수락 검증
#   sandbox/run.sh down     중지 및 볼륨 삭제(sandbox 프로젝트 전용)
here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/.." && pwd)
run="$here/.run"
dc=(docker compose -p gwangsan-sandbox --project-directory "$root" -f "$root/compose.yaml" -f "$here/compose.sandbox.yaml")

prepare() {
  mkdir -p "$run/secrets" "$run/init" "$run/alerts"
  : > "$run/alerts/received.jsonl"
  chmod 777 "$run/alerts"
  [[ -f "$root/secrets/grafana_admin_password" ]] || { mkdir -p "$root/secrets"; openssl rand -hex 16 > "$root/secrets/grafana_admin_password"; }
  local redis_pw db_pw
  redis_pw=$(openssl rand -hex 16); db_pw=$(openssl rand -hex 16)
  openssl rand -hex 16 > "$run/secrets/mariadb_root"
  printf '%s' "$redis_pw" > "$run/secrets/redis_exporter_password"
  printf 'gwangsan_monitor:%s@(mariadb:3306)/' "$db_pw" > "$run/secrets/mysql_exporter_dsn"
  chmod 644 "$run/secrets"/*
  # 운영 템플릿을 그대로 채워 쓴다(템플릿이 실제로 동작하는지 검증).
  sed -e "s/<APP_HOST_IP>/%/g" -e "s/<PASSWORD>/$db_pw/g" "$root/exporters/mariadb-monitor-grant.sql" > "$run/init/10-monitor.sql"
  grep '^user ' "$root/exporters/redis-monitor-acl.conf" | sed "s/<PASSWORD>/$redis_pw/" > "$run/users.acl"
  echo 'user default on nopass ~* &* +@all' >> "$run/users.acl"
  # 학교 설정 사본: 운영 Loki 대상만 sandbox Loki로 교체.
  sed 's/ssh.gsmsv.site:26129/loki:3100/' "$root/prometheus.yml" > "$run/prometheus.yml"
  sed 's#http://ssh.gsmsv.site:26129#http://loki:3100#' "$root/grafana/provisioning/datasources/datasources.yml" > "$run/datasources.yml"
  python3 - "$root/promtail-candidate.yml" "$run/promtail.yml" <<'PY'
import sys
s = open(sys.argv[1]).read()
s = s.replace("http://ssh.gsmsv.site:26129/loki/api/v1/push", "http://loki:3100/loki/api/v1/push")
s = s.replace("        refresh_interval: 5s\n",
              "        refresh_interval: 5s\n        filters:\n          - name: label\n            values: [gwangsan.sandbox]\n")
open(sys.argv[2], "w").write(s)
PY
}

case ${1:-} in
  up) prepare; "${dc[@]}" up -d ;;
  verify) shift; python3 "$here/verify.py" "${1:-$root/../../.omo/evidence/observability/platform}" "${@:2}" ;;
  down) "${dc[@]}" down -v ;;
  *) echo "usage: $0 up|verify|down" >&2; exit 2 ;;
esac
