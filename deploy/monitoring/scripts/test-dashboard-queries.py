#!/usr/bin/env python3
"""대시보드 PromQL 의미 테스트. 생성된 대시보드 JSON에서 패널 식을 그대로 꺼내 promtool promql_expr_test로 평가한다.
검증: 부분 집계 시 합계 미표시, 요청 0건 비율 미표시, histogram 인스턴스 합산 p95, 실패 카운터 미생성 시 0 표시, 환경별 up 분리, 디스크 fstype allowlist·마운트 필터.
python3 scripts/test-dashboard-queries.py   (docker로 prom/prometheus:v3.15.1의 promtool 실행)
"""
import json
import pathlib
import subprocess
import sys
import tempfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
VARS = {"$environment": ".*", "$group": ".*", "$uri": ".*", "$__rate_interval": "1m", "$__range": "1h"}


def expr(file, title, ref="A"):
    d = json.loads((ROOT / "grafana/dashboards" / file).read_text())
    for p in [q for top in d["panels"] for q in [top] + top.get("panels", [])]:
        if p.get("title") == title:
            e = next(t["expr"] for t in p["targets"] if t["refId"] == ref)
            for k in sorted(VARS, key=len, reverse=True):
                e = e.replace(k, VARS[k])
            return e
    raise SystemExit(f"panel not found: {file} {title}")


def case(series, tests):
    return {"interval": "15s", "input_series": [{"series": s, "values": v} for s, v in series],
            "promql_expr_test": tests}


unread = expr("00-overview.json", "읽기 전 대기")
ratio5xx = expr("00-overview.json", "API 5xx")
p95 = expr("00-overview.json", "API p95")
trace_fail = expr("08-telemetry.json", "span 실패")
age = expr("04-streams.json", "집계 경과 시간")
up_hist = expr("00-overview.json", "수집 상태 이력")
targets = expr("08-telemetry.json", "수집 대상")
disk = expr("06-hosts.json", "디스크 여유").replace("$host", ".*")
E = 'environment="production"'
API = f'service="gwangsan-api",{E}'

tests = [
    # 부분 집계(complete=0)이면 합계 없음 → 패널은 '계산 불가 · 부분 집계'
    case([(f'gwangsan_stream_unread{{{E},group="a"}}', "5x40"), (f'gwangsan_stream_unread{{{E},group="b"}}', "7x40"),
          (f'gwangsan_stream_snapshot_complete{{{E},group="a"}}', "1x40"),
          (f'gwangsan_stream_snapshot_complete{{{E},group="b"}}', "0x20 1x20")],
         [{"expr": unread, "eval_time": "4m", "exp_samples": []},
          {"expr": unread, "eval_time": "9m", "exp_samples": [{"labels": "{}", "value": 12}]}]),
    # 요청 0건(카운터 정지) → 비율 없음, 트래픽 있으면 25%
    case([(f'http_server_requests_seconds_count{{{API},status="200"}}', "0x20 0+3x20"),
          (f'http_server_requests_seconds_count{{{API},status="500"}}', "0x20 0+1x20")],
         [{"expr": ratio5xx, "eval_time": "4m", "exp_samples": []},
          {"expr": ratio5xx, "eval_time": "10m", "exp_samples": [{"labels": "{}", "value": 0.25}]}]),
    # 두 인스턴스 버킷을 합산한 p95(인스턴스별 p95 평균 아님)
    case([(f'http_server_requests_seconds_bucket{{{API},instance="i1",le="0.05"}}', "0x40"),
          (f'http_server_requests_seconds_bucket{{{API},instance="i1",le="0.1"}}', "0+10x40"),
          (f'http_server_requests_seconds_bucket{{{API},instance="i1",le="+Inf"}}', "0+10x40"),
          (f'http_server_requests_seconds_bucket{{{API},instance="i2",le="0.05"}}', "0x40"),
          (f'http_server_requests_seconds_bucket{{{API},instance="i2",le="0.1"}}', "0+30x40"),
          (f'http_server_requests_seconds_bucket{{{API},instance="i2",le="+Inf"}}', "0+30x40")],
         [{"expr": p95, "eval_time": "8m", "exp_samples": [{"labels": "{}", "value": 0.0975}]}]),
    # 실패 카운터 미생성 + 전송 카운터 존재 → 0, 둘 다 없으면 미수집(빈 결과)
    case([('otelcol_exporter_sent_spans_total{service="aws-alloy"}', "0+5x40")],
         [{"expr": trace_fail, "eval_time": "8m", "exp_samples": [{"labels": "{}", "value": 0}]}]),
    case([('up{job="other"}', "1x40")], [{"expr": trace_fail, "eval_time": "8m", "exp_samples": []}]),
    # 집계 경과 시간 = 현재 - 마지막 정상 집계 시각
    case([(f'gwangsan_stream_snapshot_timestamp_seconds{{{E},group="a"}}', "0+15x8 120x32")],
         [{"expr": age, "eval_time": "8m", "exp_samples": [{"labels": "{}", "value": 360}]}]),
    # 환경 All: sandbox 실패(up=0)가 test 정상(up=1)에 가려지지 않고 환경별로 따로 보인다
    case([('up{collector="gwangsan-aws-alloy",environment="sandbox",service="gwangsan-api",job="api",instance="a"}', "0x40"),
          ('up{collector="gwangsan-aws-alloy",environment="test",service="gwangsan-api",job="api",instance="a"}', "1x40")],
         [{"expr": up_hist, "eval_time": "8m", "exp_samples": [
             {"labels": '{environment="sandbox",service="gwangsan-api"}', "value": 0},
             {"labels": '{environment="test",service="gwangsan-api"}', "value": 1}]},
          {"expr": targets, "eval_time": "8m", "exp_samples": [
             {"labels": '{environment="sandbox",instance="a",job="api",service="gwangsan-api"}', "value": 0},
             {"labels": '{environment="test",instance="a",job="api",service="gwangsan-api"}', "value": 1}]}]),
    # 디스크 여유: ext4·xfs 영구 디스크만 남기고 tmpfs·overlay·erofs·fakeowner·/etc bind mount는 제외
    case([('node_filesystem_avail_bytes{instance="h",mountpoint="/",fstype="ext4"}', "50x40"),
          ('node_filesystem_avail_bytes{instance="h",mountpoint="/data",fstype="xfs"}', "30x40"),
          ('node_filesystem_size_bytes{instance="h",mountpoint="/data",fstype="xfs"}', "100x40"),
          ('node_filesystem_avail_bytes{instance="h",mountpoint="/oldroot",fstype="erofs"}', "0x40"),
          ('node_filesystem_size_bytes{instance="h",mountpoint="/oldroot",fstype="erofs"}', "100x40"),
          ('node_filesystem_avail_bytes{instance="h",mountpoint="/run/host_mark/Users",fstype="fakeowner"}', "52x40"),
          ('node_filesystem_size_bytes{instance="h",mountpoint="/run/host_mark/Users",fstype="fakeowner"}', "100x40"),
          ('node_filesystem_size_bytes{instance="h",mountpoint="/",fstype="ext4"}', "100x40"),
          ('node_filesystem_avail_bytes{instance="h",mountpoint="/run",fstype="tmpfs"}', "1x40"),
          ('node_filesystem_size_bytes{instance="h",mountpoint="/run",fstype="tmpfs"}', "100x40"),
          ('node_filesystem_avail_bytes{instance="h",mountpoint="/etc/hosts",fstype="ext4"}', "1x40"),
          ('node_filesystem_size_bytes{instance="h",mountpoint="/etc/hosts",fstype="ext4"}', "100x40")],
         [{"expr": disk, "eval_time": "8m", "exp_samples": [{"labels": '{instance="h",mountpoint="/"}', "value": 0.5},
                                                             {"labels": '{instance="h",mountpoint="/data"}', "value": 0.3}]}]),
]

with tempfile.TemporaryDirectory() as tmp:
    (pathlib.Path(tmp) / "empty.yml").write_text("groups: []\n")
    (pathlib.Path(tmp) / "t.yml").write_text(json.dumps({"rule_files": ["empty.yml"], "evaluation_interval": "15s",
                                                         "tests": tests}, ensure_ascii=False))
    r = subprocess.run(["docker", "run", "--rm", "--network", "none", "-v", f"{tmp}:/t:ro", "-w", "/t",
                        "--entrypoint", "promtool", "prom/prometheus:v3.15.1", "test", "rules", "t.yml"],
                       capture_output=True, text=True)
    print(r.stdout + r.stderr)
    sys.exit(r.returncode)
