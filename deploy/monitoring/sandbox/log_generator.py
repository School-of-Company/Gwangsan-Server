# 합성 로그·트레이스 생성기(sandbox 전용, 실제 사용자 데이터 없음).
# 구형 평문 줄과 신형 JSON 줄을 섞어 stdout으로 내고, 같은 trace_id의 합성 span을 Alloy OTLP로 보낸다.
import datetime
import json
import random
import sys
import time
import urllib.request

service = sys.argv[1]
endpoint = "http://alloy:4318/v1/traces"


def send_span(trace_id, span_id, start_ns, duration_ns, error):
    body = {"resourceSpans": [{
        "resource": {"attributes": [
            {"key": "service.name", "value": {"stringValue": service}},
            {"key": "deployment.environment.name", "value": {"stringValue": "sandbox"}}]},
        "scopeSpans": [{"scope": {"name": "sandbox"}, "spans": [{
            "traceId": trace_id, "spanId": span_id, "name": "GET /api/sandbox", "kind": 2,
            "startTimeUnixNano": str(start_ns), "endTimeUnixNano": str(start_ns + duration_ns),
            "status": {"code": 2 if error else 1}}]}]}]}
    req = urllib.request.Request(endpoint, json.dumps(body).encode(), {"Content-Type": "application/json"})
    try:
        urllib.request.urlopen(req, timeout=2).read()
    except OSError as e:  # 수집기 장애가 로그 생성을 막지 않는다
        print(json.dumps({"level": "WARN", "service": service, "message": f"SANDBOX_OTLP_FAILED {type(e).__name__}"}))


i = 0
while True:
    i += 1
    now = datetime.datetime.now(datetime.timezone.utc)
    trace_id = "%032x" % random.getrandbits(128)
    span_id = "%016x" % random.getrandbits(64)
    if i % 10 == 0:
        print(now.strftime("%Y-%m-%d %H:%M:%S.000") + " ERROR 1 --- [main] c.g.Legacy : SANDBOX_LEGACY_ERROR")
        print("    at sandbox.Legacy.frame(Legacy.java:1)")
    error = i % 7 == 0
    slow = i % 5 == 0
    send_span(trace_id, span_id, time.time_ns() - (1_500_000_000 if slow else 20_000_000),
              1_500_000_000 if slow else 20_000_000, error)
    print(json.dumps({"timestamp": now.isoformat(), "level": "ERROR" if error else "INFO", "service": service,
                      "message": f"SANDBOX_EVENT seq={i}", "event": "sandbox.tick",
                      "trace_id": trace_id, "span_id": span_id}), flush=True)
    time.sleep(2)
