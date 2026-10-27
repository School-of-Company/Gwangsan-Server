#!/usr/bin/env bash
set -euo pipefail

# 호스트 지표 수집기. 사용: node-exporter-run.sh <바인딩 IP>
#   앱 EC2: 172.22.0.1 (gwangsan-observability gateway, Alloy만 접근)
#   DB/Redis EC2: 각 사설 IP + 보안 그룹에서 앱 EC2(10.0.2.44)→9100만 허용
# 0.0.0.0 바인딩은 거부한다. 기존 컨테이너는 교체하지 않는다.
bind=${1:?bind IP required}
if [[ "$bind" == 0.0.0.0 || "$bind" == "::" ]]; then
  echo 'refusing wildcard bind' >&2
  exit 1
fi
if sudo -n docker container inspect gwangsan-node-exporter >/dev/null 2>&1; then
  echo 'gwangsan-node-exporter already exists' >&2
  exit 1
fi

image=prom/node-exporter:v1.10.2@sha256:3ac34ce007accad95afed72149e0d2b927b7e42fd1c866149b945b84737c62c3
sudo -n docker run -d --name gwangsan-node-exporter \
  --net host --pid host --restart unless-stopped \
  --memory 64m --cpus 0.2 --user 65534:65534 --cap-drop ALL \
  --security-opt no-new-privileges --read-only \
  --log-driver json-file --log-opt max-size=10m --log-opt max-file=3 \
  -v /:/host:ro,rslave \
  "$image" --path.rootfs=/host --web.listen-address="$bind:9100" \
  --collector.disable-defaults \
  --collector.cpu --collector.loadavg --collector.meminfo --collector.filesystem \
  --collector.diskstats --collector.netdev --collector.stat --collector.uname \
  --collector.time --collector.pressure --collector.vmstat \
  '--collector.filesystem.fs-types-exclude=^(autofs|binfmt_misc|cgroup2?|configfs|debugfs|devpts|devtmpfs|fusectl|hugetlbfs|mqueue|nsfs|overlay|proc|pstore|rpc_pipefs|securityfs|squashfs|sysfs|tmpfs|tracefs)$' \
  '--collector.netdev.device-exclude=^(lo|veth.*|br-.*|docker.*)$'
