#!/usr/bin/env python3
"""chat-load.mjs 결과 JSON 들을 같은 워크로드끼리 묶어 markdown 표로 낸다. 해석(인과·SLO)은 하지 않는다.
거부된 전송은 소켓 error 와 echo 없음 양쪽에 함께 잡힐 수 있어 두 열을 더하지 않는다.
uncommitted 는 echo 된(수락된) 메시지 중 미커밋 수다. 거부된 전송은 포함하지 않는다.
python3 summarize.py <load-*.json ...>
"""
import json
import statistics
import sys

rows = []
for f in sorted(sys.argv[1:]):
    d = json.load(open(f))
    for r in d["repeats"]:
        rows.append((d["clients"], d["label"], r))


def med(xs):
    xs = [x for x in xs if x is not None]
    return round(statistics.median(xs)) if xs else "-"


print("| clients | mode | repeats | emitted | echoed | committed | rejected(socket error) | no echo | uncommitted of echoed | echo p50/p95/p99 ms (median of repeats) "
      "| commit-observed p50/p95/p99 ms | drain ms | API CPU% avg | API mem MiB max | chat CPU% avg |")
print("|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|")
for clients in sorted({c for c, _, _ in rows}):
    for mode in ("baseline", "instrumented", "instrumented-off"):
        rs = [r for c, m, r in rows if c == clients and m == mode]
        if not rs:
            continue
        api = [r["containers"].get("gw-load-api-1", {}) for r in rs]
        chat = [r["containers"].get("gw-load-chat-1", {}) for r in rs]
        print(f"| {clients} | {mode} | {len(rs)} | {sum(r['emitted'] for r in rs)} | {sum(r['echoed'] for r in rs)} | {sum(r['committed'] for r in rs)} "
              f"| {sum(r['socketErrors'] for r in rs)} | {sum(r['unansweredEchoes'] for r in rs)} | {sum(r['uncommitted'] for r in rs)} "
              f"| {med([r['echoMs']['p50'] for r in rs])}/{med([r['echoMs']['p95'] for r in rs])}/{med([r['echoMs']['p99'] for r in rs])} "
              f"| {med([r['commitObservedMs']['p50'] for r in rs])}/{med([r['commitObservedMs']['p95'] for r in rs])}/{med([r['commitObservedMs']['p99'] for r in rs])} "
              f"| {med([r['drainMsAfterLastSend'] for r in rs])} "
              f"| {med([(a.get('cpuPct') or {}).get('avg') for a in api])} | {med([(a.get('memMiB') or {}).get('max') for a in api])} "
              f"| {med([(c.get('cpuPct') or {}).get('avg') for c in chat])} |")
print()
print("poll resolution ms (commit-observed): " + ", ".join(
    f"{m}/c{c}:{med([r['commitPollResolutionMs'] for cc, mm, r in rows if cc == c and mm == m])}"
    for c in sorted({c for c, _, _ in rows}) for m in ("baseline", "instrumented") if any(cc == c and mm == m for cc, mm, _ in rows)))
