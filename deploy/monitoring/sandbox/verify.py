#!/usr/bin/env python3
"""sandbox 검증: Grafana에 프로비저닝된 9개 대시보드의 모든 패널 쿼리를 Grafana /api/ds/query로 실행하고,
로그→트레이스·트레이스→로그 링크, 로컬 경보 수신기의 발생/해제를 확인한다.
결과는 실제 응답만 기록한다. 데이터 없음(미수집)과 쿼리 오류를 구분한다.

python3 sandbox/verify.py <증거 디렉터리> [--mode accept|diagnostic] [--require-alert 이름 ...]
  accept(기본): 쿼리 오류, 미프로비저닝, 로그→트레이스·트레이스→로그 링크 미확인,
                필수 경보의 firing·resolved 기록 누락 중 하나라도 있으면 종료 코드 1.
  diagnostic:   같은 항목을 INCOMPLETE로 출력만 하고 종료 코드 0(진행 중 점검용, 합격 증거 아님).
"""
import argparse
import json
import pathlib
import re
import sys
import time
import urllib.parse
import urllib.request

GRAFANA = "http://127.0.0.1:13000"
ROOT = pathlib.Path(__file__).resolve().parents[1]
VARS = {"$environment": ".*", "${environment:queryparam}": "", "$uri": ".*", "$group": ".*", "$host": ".*",
        "$container": "gwangsan-container|gwangsan-chat", "$level": ".*", "$q": "", "$traceId": "",
        "$__rate_interval": "1m", "$__range": "1h", "$__interval": "1m"}


def call(path, body=None):
    req = urllib.request.Request(GRAFANA + path, json.dumps(body).encode() if body else None,
                                 {"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, json.loads(r.read() or b"{}")
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read() or b"{}")


def subst(s):
    for k in sorted(VARS, key=len, reverse=True):
        s = s.replace(k, VARS[k])
    return s


def run_target(t):
    q = {k: (subst(v) if isinstance(v, str) else v) for k, v in t.items()}
    if q.get("query") == "":  # Trace ID 입력 대기 패널
        return {"status": "skipped(입력 필요)"}
    q.setdefault("intervalMs", 60000)
    q.setdefault("maxDataPoints", 300)
    code, res = call("/api/ds/query", {"queries": [q], "from": "now-1h", "to": "now"})
    r = res.get("results", {}).get(t["refId"], {}) if code == 200 else {}
    err = r.get("error") or (res.get("message") if code != 200 else None)
    frames = r.get("frames", [])
    values = sum(len(f.get("data", {}).get("values", [[]])[-1] or []) for f in frames if f.get("data", {}).get("values"))
    return {"status": "error" if err else ("data" if values else "empty"), "http": code, "error": err,
            "series": len(frames), "points": values}


def dashboards():
    out = []
    for path in sorted((ROOT / "grafana/dashboards").glob("*.json")):
        d = json.loads(path.read_text())
        code, live = call(f"/api/dashboards/uid/{d['uid']}")
        entry = {"file": path.name, "uid": d["uid"], "title": d["title"], "provisioned": code == 200,
                 "folder": live.get("meta", {}).get("folderTitle"), "panels": [],
                 "expected_panels": sum(1 for top in d["panels"] for q in [top] + top.get("panels", [])
                                        if q["type"] not in ("row", "text"))}
        for p in [q for top in d["panels"] for q in [top] + top.get("panels", [])]:
            if p["type"] in ("row", "text"):
                continue
            entry["panels"].append({"title": p["title"], "type": p["type"],
                                    "targets": [dict(expr=t.get("expr") or t.get("query"), **run_target(t))
                                                for t in p.get("targets", [])]})
        entry["checked_panels"] = len(entry["panels"])
        out.append(entry)
    return out


def links():
    res = {}
    code, r = call("/api/datasources/proxy/uid/gwangsan-loki/loki/api/v1/query_range?" + urllib.parse.urlencode(
        {"query": '{container="gwangsan-container"} |= "trace_id"', "limit": 20, "since": "10m"}))
    lines = [v[1] for s in r.get("data", {}).get("result", []) for v in s["values"]]
    regex = None
    for ds in call("/api/datasources")[1]:
        if ds["uid"] == "gwangsan-loki":
            regex = ds["jsonData"]["derivedFields"][0]["matcherRegex"]
    ids = [m.group(1) for m in (re.search(regex, l) for l in lines) if m]
    res["loki_lines_with_trace"] = len(lines)
    res["derived_field_regex_matches"] = len(ids)
    found = None
    for tid in ids:
        c, t = call(f"/api/datasources/proxy/uid/gwangsan-tempo/api/traces/{tid}")
        if c == 200 and (t.get("batches") or t.get("resourceSpans") or t.get("trace")):
            found = tid
            break
    res["log_to_trace"] = {"trace_id": found, "tempo_found": bool(found)}
    if found:
        tempo = next(ds for ds in call("/api/datasources")[1] if ds["uid"] == "gwangsan-tempo")
        q = tempo["jsonData"]["tracesToLogsV2"]["query"].replace("${__trace.traceId}", found)
        c, r = call("/api/datasources/proxy/uid/gwangsan-loki/loki/api/v1/query_range?" + urllib.parse.urlencode(
            {"query": q, "limit": 5, "since": "30m"}))
        res["trace_to_logs"] = {"query": q, "lines": sum(len(s["values"]) for s in r.get("data", {}).get("result", []))}
    return res


def alerts():
    path = ROOT / "sandbox/.run/alerts/received.jsonl"
    rows = [json.loads(l) for l in path.read_text().splitlines() if l.strip()] if path.exists() else []
    return {"received": rows, "by_status": {s: sorted({r["alertname"] for r in rows if r["status"] == s})
                                            for s in ("firing", "resolved")}}


def acceptance_failures(result, required_alerts):
    fails = []
    for d in result["dashboards"]:
        if not d["provisioned"]:
            fails.append(f"미프로비저닝: {d['file']}")
        if d["checked_panels"] != d["expected_panels"]:
            fails.append(f"패널 누락 검사: {d['file']} {d['checked_panels']}/{d['expected_panels']}")
        for p in d["panels"]:
            for t in p["targets"]:
                if t["status"] == "error":
                    fails.append(f"쿼리 오류: {d['file']} | {p['title']} | {t['error']}")
    links = result["links"]
    if not links.get("log_to_trace", {}).get("tempo_found"):
        fails.append("로그→트레이스 링크 미확인(trace_id 로그 또는 Tempo 조회 실패)")
    if links.get("trace_to_logs", {}).get("lines", 0) < 1:
        fails.append("트레이스→로그 링크 미확인")
    by = result["alerts"]["by_status"]
    for name in required_alerts:
        for status in ("firing", "resolved"):
            if name not in by[status]:
                fails.append(f"경보 증거 누락: {name} {status}")
    return fails


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("evidence", nargs="?", default=str(ROOT / "sandbox/.run/evidence"))
    ap.add_argument("--mode", choices=["accept", "diagnostic"], default="accept")
    ap.add_argument("--require-alert", action="append",
                    help="firing과 resolved가 모두 수신돼야 하는 경보 이름(반복 가능)")
    args = ap.parse_args()
    required = args.require_alert or ["Redis응답없음", "AWS수집경로중단"]
    evidence = pathlib.Path(args.evidence)
    evidence.mkdir(parents=True, exist_ok=True)
    result = {"checked_at": time.strftime("%Y-%m-%dT%H:%M:%S%z"), "mode": args.mode, "required_alerts": required,
              "grafana_health": call("/api/health")[1],
              "dashboards": dashboards(), "links": links(), "alerts": alerts()}
    fails = acceptance_failures(result, required)
    result["acceptance"] = {"passed": not fails, "failures": fails}
    (evidence / "sandbox-verify.json").write_text(json.dumps(result, ensure_ascii=False, indent=2))
    counts = {}
    for d in result["dashboards"]:
        for p in d["panels"]:
            for t in p["targets"]:
                counts[t["status"]] = counts.get(t["status"], 0) + 1
    print("provisioned:", sum(d["provisioned"] for d in result["dashboards"]), "/", len(result["dashboards"]))
    print("panels checked:", sum(d["checked_panels"] for d in result["dashboards"]), "(collapsed row 포함)")
    print("targets:", counts)
    print("links:", json.dumps(result["links"], ensure_ascii=False))
    print("alerts:", json.dumps(result["alerts"]["by_status"], ensure_ascii=False))
    label = "FAIL" if args.mode == "accept" else "INCOMPLETE"
    for f in fails:
        print(label, f, sep=": ")
    if fails:
        print(f"{label}: 수락 기준 미충족 {len(fails)}건" + (" (diagnostic: 합격 아님)" if args.mode == "diagnostic" else ""))
    else:
        print("ACCEPT: 모든 수락 기준 충족")
    sys.exit(1 if fails and args.mode == "accept" else 0)


if __name__ == "__main__":
    main()
