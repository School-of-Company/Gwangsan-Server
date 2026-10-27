# 로컬 경보 수신기: Alertmanager webhook 본문을 /alerts/received.jsonl에 한 줄씩 기록한다.
import http.server
import json
import time


class Sink(http.server.BaseHTTPRequestHandler):
    def do_POST(self):
        body = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
        with open("/alerts/received.jsonl", "a") as f:
            for a in body["alerts"]:
                f.write(json.dumps({"received_at": time.strftime("%Y-%m-%dT%H:%M:%S%z"),
                                    "status": a["status"], "alertname": a["labels"]["alertname"],
                                    "labels": a["labels"], "summary": a["annotations"].get("summary")},
                                   ensure_ascii=False) + "\n")
        self.send_response(200)
        self.end_headers()

    def log_message(self, *args):
        pass


http.server.HTTPServer(("0.0.0.0", 8080), Sink).serve_forever()
