#!/usr/bin/env python3
"""실제 config.alloy 경로로 들어온 메트릭을 자체 Prometheus 수신기에서 검증한다.

python3 verify.py <prom_url> <out_dir> <phase>
  phase=steady  계약 메트릭 존재·고정 라벨·라벨 허용목록·증가량·대시보드 PromQL 평가
  phase=rawcount|gap <start> <end>  원격 경로 중단 구간(epoch 초)의 원본 샘플: 재개 직전 없음, 재개 뒤 WAL 재전송으로 채워짐
숫자를 지어내지 않는다. 비어 있는 패널은 '비어 있음'으로 그대로 기록한다.
"""
import json
import pathlib
import sys
import time
import urllib.parse
import urllib.request

PROM, OUT, PHASE = sys.argv[1], pathlib.Path(sys.argv[2]), sys.argv[3]
OUT.mkdir(parents=True, exist_ok=True)
DASH = pathlib.Path(__file__).resolve().parents[2] / "grafana/dashboards"

CONTRACT = {
    "gwangsan_chat_connections": "gwangsan-chat",
    "gwangsan_chat_events_total": "gwangsan-chat",
    "gwangsan_chat_publish_duration_seconds_bucket": "gwangsan-chat",
    "gwangsan_chat_persist_attempts_total": "gwangsan-api",
    "gwangsan_chat_persist_duration_seconds_bucket": "gwangsan-api",
    "gwangsan_chat_publish_to_commit_seconds_bucket": "gwangsan-api",
    "gwangsan_chat_ack_total": "gwangsan-api",
    "gwangsan_chat_retry_total": "gwangsan-api",
    "gwangsan_chat_dlq_total": "gwangsan-api",
    "gwangsan_stream_unread": "gwangsan-api",
    "gwangsan_stream_pending": "gwangsan-api",
    "gwangsan_stream_snapshot_timestamp_seconds": "gwangsan-api",
    "gwangsan_stream_snapshot_success": "gwangsan-api",
    "gwangsan_stream_snapshot_complete": "gwangsan-api",
}
# 계약 메트릭에 허용되는 라벨. 동적 ID 라벨이 생기면 실패한다.
ALLOWED = {"__name__", "job", "service", "environment", "instance", "collector",
           "outcome", "event", "group", "le"}
results, failures = {}, []


def q(expr, t=None):
    params = {"query": expr}
    if t is not None:
        params["time"] = str(t)
    with urllib.request.urlopen(f"{PROM}/api/v1/query?{urllib.parse.urlencode(params)}", timeout=20) as r:
        body = json.load(r)
    if body["status"] != "success":
        raise RuntimeError(body)
    return body["data"]["result"]


def check(name, ok, detail):
    results[name] = {"ok": bool(ok), "detail": detail}
    if not ok:
        failures.append(name)


if PHASE == "steady":
    for job in ("gwangsan-api", "gwangsan-chat", "alloy"):
        r = q(f'up{{job="{job}",environment="test"}}')
        check(f"up:{job}", r and all(x["value"][1] == "1" for x in r), [x["metric"] for x in r])
    for name, service in CONTRACT.items():
        r = q(f'{name}{{environment="test"}}')  # 공유 수신기(C sandbox)의 다른 환경 데이터는 제외
        labels = sorted({k for x in r for k in x["metric"]})
        fixed = {(x["metric"].get("service"), x["metric"].get("instance"), x["metric"].get("environment"),
                  x["metric"].get("collector")) for x in r}
        check(f"present:{name}", r, f"{len(r)} series")
        check(f"fixed-labels:{name}", r and fixed == {(service, service, "test", "gwangsan-aws-alloy")}, sorted(map(str, fixed)))
        check(f"label-allowlist:{name}", set(labels) <= ALLOWED, labels)
    sent = q('sum(increase(gwangsan_chat_events_total{environment="test",event="sendMessage",outcome="success"}[10m]))') or \
        q('sum(increase(gwangsan_chat_events_total{environment="test"}[10m]))')
    committed = q('sum(increase(gwangsan_chat_persist_attempts_total{environment="test",outcome="success"}[10m]))')
    p95 = q('histogram_quantile(0.95, sum by (le) (rate(gwangsan_chat_publish_to_commit_seconds_bucket{environment="test"}[10m])))')
    check("traffic:events-increase", sent and float(sent[0]["value"][1]) > 0, sent)
    check("traffic:commits-increase", committed and float(committed[0]["value"][1]) > 0, committed)
    check("histogram:p95-defined", p95 and p95[0]["value"][1] not in ("NaN", "+Inf"), p95)
    # 9개 대시보드의 실제 PromQL 을 그대로 평가한다(변수만 치환). 오류는 실패, 빈 결과는 기록만.
    subs = {"$environment": "test", "$group": ".*", "$uri": ".*", "$__rate_interval": "1m",
            "$__range": "10m", "$__interval": "15s", "${environment}": "test"}
    panels = []
    for f in sorted(DASH.glob("*.json")):
        d = json.loads(f.read_text())
        for p in [s for top in d.get("panels", []) for s in [top] + top.get("panels", [])]:
            for t in p.get("targets", []):
                if t.get("datasource", {}).get("uid", p.get("datasource", {}).get("uid")) != "gwangsan-prometheus" or "expr" not in t:
                    continue
                e = t["expr"]
                for k in sorted(subs, key=len, reverse=True):
                    e = e.replace(k, subs[k])
                try:
                    n = len(q(e))
                    panels.append({"dashboard": f.name, "panel": p.get("title"), "ref": t.get("refId"), "series": n})
                except Exception as ex:  # noqa: BLE001 - 잘못된 PromQL 은 그대로 기록하고 실패 처리
                    panels.append({"dashboard": f.name, "panel": p.get("title"), "ref": t.get("refId"), "error": str(ex)[:200]})
    errors = [p for p in panels if "error" in p]
    check("dashboards:promql-valid", not errors, errors[:10])
    results["dashboards:panels"] = {"ok": True, "detail": {
        "evaluated": len(panels), "nonEmpty": sum(1 for p in panels if p.get("series")),
        "empty": [f'{p["dashboard"]}:{p["panel"]}:{p["ref"]}' for p in panels if p.get("series") == 0]}}
elif PHASE in ("rawcount", "gap"):
    # query_range 는 평가 시각과 5분 lookback 때문에 빈 구간도 연속처럼 보인다.
    # 그래서 range vector 를 instant 로 조회해 원본 샘플 시각(수신기에 실제 저장된 것)만 센다.
    start, end = float(sys.argv[4]), float(sys.argv[5])
    sel = 'gwangsan_chat_events_total{environment="test",service="gwangsan-chat",event="sendMessage",outcome="success"}'
    now = time.time()
    window = int(now - start + 60)
    r = q(f"{sel}[{window}s]", now)
    ts = sorted({float(v[0]) for s in r for v in s["values"]})
    inside = [t for t in ts if start + 5 < t < end - 5]
    expected = max(0, int((end - start - 10) // 15))
    record = {"gapSeconds": round(end - start), "rawSamplesInsideGap": len(inside), "expectedAtScrape15s": expected,
              "series": len(r), "queriedAt": now}
    if PHASE == "rawcount":
        # relay 재개 직전 스냅샷: 아직 원격 전송이 막혀 있으므로 구간 샘플이 없어야 한다.
        (OUT / "gap-before-resume.json").write_text(json.dumps(record))
        check("gap:no-samples-before-resume", len(inside) <= 1, record)
    else:
        before = json.loads((OUT / "gap-before-resume.json").read_text())
        record["beforeResume"] = before["rawSamplesInsideGap"]
        check("gap:raw-samples-backfilled-after-resume",
              expected and len(inside) >= expected - 1 and len(inside) > before["rawSamplesInsideGap"], record)
        for m in ("prometheus_remote_storage_samples_failed_total", "prometheus_remote_storage_samples_retried_total",
                  "prometheus_remote_storage_samples_dropped_total"):
            r = q(f'sum({m}{{job="alloy",environment="test"}})')
            results[f"alloy:{m}"] = {"ok": True, "detail": r[0]["value"][1] if r else "absent"}

summary = {"phase": PHASE, "at": time.time(), "failures": failures, "results": results}
(OUT / f"metrics-verify-{PHASE}.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2))
print(json.dumps({"phase": PHASE, "failures": failures, "checks": len(results)}, ensure_ascii=False))
sys.exit(1 if failures else 0)
