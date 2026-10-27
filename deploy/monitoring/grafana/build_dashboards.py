#!/usr/bin/env python3
"""광산 운영 관제 대시보드 9종 생성기.

python3 grafana/build_dashboards.py  → grafana/dashboards/*.json
JSON은 생성 결과물이다. 패널을 고칠 때는 이 파일을 고친 뒤 다시 생성한다.
기본 Grafana 패널만 사용한다(플러그인·React·임의 CSS 없음).
"""
import json
import pathlib

OUT = pathlib.Path(__file__).resolve().parent / "dashboards"
PROM = {"type": "prometheus", "uid": "gwangsan-prometheus"}
LOKI = {"type": "loki", "uid": "gwangsan-loki"}
TEMPO = {"type": "tempo", "uid": "gwangsan-tempo"}

E = 'environment=~"$environment"'
API = f'service="gwangsan-api",{E}'
CHAT = f'service="gwangsan-chat",{E}'
AWS = f'collector="gwangsan-aws-alloy",{E}'

GREEN, ORANGE, RED, GREY = "green", "#FF9830", "red", "#8E8E8E"
BLUE, TEAL, PURPLE = "#3274D9", "#14B8A6", "#A352CC"

NO_DATA = "미수집"
NO_REQ = "요청 없음·미수집"
UNKNOWN = "계산 불가"

PER_SEC = "suffix:건/초"
COUNT = "suffix:건"
EA = "suffix:개"

UIDS = {
    "overview": "gwangsan-overview",
    "api": "gwangsan-api",
    "jvm": "gwangsan-jvm",
    "chat": "gwangsan-chat",
    "streams": "gwangsan-streams",
    "data": "gwangsan-data",
    "hosts": "gwangsan-hosts",
    "logs": "gwangsan-logs",
    "telemetry": "gwangsan-telemetry",
}


def desc(what, source, unit, window, missing, nxt):
    return (f"정의: {what}\n출처: {source}\n단위: {unit}\n집계 창: {window}\n"
            f"결측 처리: {missing}\n이상 시 확인: {nxt}")


def link(key, title):
    return {"title": title, "url": f"/d/{UIDS[key]}?${{__url_time_range}}&${{environment:queryparam}}"}


def thresholds(steps):
    out = [{"color": steps[0], "value": None}]
    for value, color in steps[1:]:
        out.append({"color": color, "value": value})
    return {"mode": "absolute", "steps": out}


def prom(expr, legend="", instant=False, ref="A"):
    t = {"datasource": PROM, "expr": expr, "refId": ref, "legendFormat": legend or "__auto"}
    if instant:
        t.update(instant=True, range=False, format="table")
    return t


class Board:
    def __init__(self, key, title, description, extra_vars=(), annotations=()):
        self.key, self.title, self.description = key, title, description
        self.panels, self.next_id, self.y = [], 1, 0
        self.row_h, self.x = 0, 0
        self.collapsed_row = None
        self.vars = [env_var()] + list(extra_vars)
        self.annotations = list(annotations)

    def place(self, w, h):
        if self.x + w > 24:
            self.y += self.row_h
            self.x, self.row_h = 0, 0
        pos = {"x": self.x, "y": self.y, "w": w, "h": h}
        self.x += w
        self.row_h = max(self.row_h, h)
        return pos

    def row(self, title, collapsed=False):
        if self.x:
            self.y += self.row_h
            self.x, self.row_h = 0, 0
        row = {"id": self._id(), "type": "row", "title": title, "collapsed": collapsed,
               "gridPos": {"x": 0, "y": self.y, "w": 24, "h": 1}, "panels": []}
        self.panels.append(row)
        self.collapsed_row = row if collapsed else None
        self.y += 1

    def _id(self):
        i = self.next_id
        self.next_id += 1
        return i

    def add(self, panel, w, h):
        panel["id"] = self._id()
        panel["gridPos"] = self.place(w, h)
        (self.collapsed_row["panels"] if self.collapsed_row else self.panels).append(panel)

    def stat(self, title, expr, unit, description, w=4, h=4, no_value=NO_DATA, steps=None,
             mappings=None, links=(), legend="", decimals=None, color_mode="value", text_mode="auto"):
        missing = {"type": "special", "options": {"match": "null+nan",
                                                  "result": {"text": no_value, "color": GREY, "index": 9}}}
        defaults = {"unit": unit, "noValue": no_value,
                    "thresholds": thresholds(steps or [GREEN]),
                    "color": {"mode": "thresholds"}, "mappings": (mappings or []) + [missing]}
        if decimals is not None:
            defaults["decimals"] = decimals
        # 추세는 옆 시계열 패널에 있으므로 sparkline을 끈다(좁은 폭에서 값·단위와 겹침).
        # 좁은 KPI 헤더에 링크 아이콘을 두면 제목이 잘리므로 값 클릭(data link)으로 이동한다.
        if links:
            defaults["links"] = [dict(l, oneClick=True) for l in links]
        self.add({
            "type": "stat", "title": title, "description": description, "datasource": PROM,
            "targets": [prom(expr, legend)],
            "fieldConfig": {"defaults": defaults, "overrides": []},
            "links": [],
            "options": {"reduceOptions": {"calcs": ["lastNotNull"], "fields": "", "values": False},
                        "colorMode": color_mode, "graphMode": "none", "justifyMode": "auto",
                        "textMode": text_mode, "orientation": "auto", "wideLayout": True,
                        "showPercentChange": False},
        }, w, h)

    def bars(self, title, targets, unit, description, w=24, h=5, no_value=NO_DATA, links=(), decimals=2):
        self.add({
            "type": "bargauge", "title": title, "description": description, "datasource": PROM,
            "targets": [prom(e, l, ref=chr(65 + i)) for i, (e, l) in enumerate(targets)],
            "fieldConfig": {"defaults": {"unit": unit, "noValue": no_value, "decimals": decimals, "min": 0,
                                         "color": {"mode": "fixed", "fixedColor": TEAL},
                                         "thresholds": thresholds([TEAL])}, "overrides": []},
            "links": list(links),
            "options": {"orientation": "vertical", "displayMode": "basic", "showUnfilled": True,
                        "valueMode": "text", "namePlacement": "top", "sizing": "auto",
                        "reduceOptions": {"calcs": ["lastNotNull"], "fields": "", "values": False}},
        }, w, h)

    def ts(self, title, targets, unit, description, w=12, h=8, no_value=NO_DATA, steps=None,
           stack=False, links=(), min_zero=True, overrides=None):
        custom = {"drawStyle": "line", "lineWidth": 2, "fillOpacity": 12, "showPoints": "never",
                  "spanNulls": False, "insertNulls": False,
                  "stacking": {"mode": "normal" if stack else "none", "group": "A"},
                  "thresholdsStyle": {"mode": "line+area" if steps else "off"},
                  "axisSoftMin": 0 if min_zero else None}
        self.add({
            "type": "timeseries", "title": title, "description": description, "datasource": PROM,
            "targets": [prom(e, l, ref=chr(65 + i)) for i, (e, l) in enumerate(targets)],
            "fieldConfig": {"defaults": {"unit": unit, "noValue": no_value, "custom": custom,
                                         "color": {"mode": "palette-classic"},
                                         "thresholds": thresholds(steps or [GREEN])},
                            "overrides": service_colors() + (overrides or [])},
            "links": list(links),
            "options": {"legend": {"displayMode": "table", "placement": "bottom", "showLegend": True,
                                   "calcs": ["lastNotNull", "max"]},
                        "tooltip": {"mode": "multi", "sort": "desc"}},
        }, w, h)

    def table(self, title, expr, description, columns, w=12, h=8, no_value="해당 없음", unit=None,
              links=None, sort=None, widths=None, order=None, value_map=None, fixed_widths=None, sort_asc=False):
        rename = {k: v for k, v in columns.items()}
        exclude = {"Time": True, "__name__": True}
        overrides = [{"matcher": {"id": "byName", "options": v}, "properties": [{"id": "noValue", "value": "-"}]}
                     for v in columns.values()]
        if unit:
            overrides.append({"matcher": {"id": "byName", "options": columns.get("Value", "Value")},
                              "properties": [{"id": "unit", "value": unit}]})
        overrides += [min_width({"id": "byName", "options": n}, px) for n, px in (widths or {}).items()]
        overrides += [{"matcher": {"id": "byName", "options": n}, "properties": [{"id": "custom.width", "value": px}]}
                      for n, px in (fixed_widths or {}).items()]
        if value_map:
            # 상태 열만 텍스트·색 매핑(나머지 열은 기본 표시)
            overrides.append({"matcher": {"id": "byName", "options": columns["Value"]},
                              "properties": [{"id": "mappings", "value": value_map},
                                             {"id": "custom.cellOptions", "value": {"type": "color-text"}}]})
        organize = {"excludeByName": exclude, "renameByName": rename, "includeByName": {k: True for k in columns}}
        if order:
            organize["indexByName"] = {k: i for i, k in enumerate(order)}
        if links:
            overrides.append({"matcher": {"id": "byName", "options": links[0]},
                              "properties": [{"id": "links", "value": [links[1]]}]})
        self.add({
            "type": "table", "title": title, "description": description, "datasource": PROM,
            "targets": [prom(expr, instant=True)],
            "fieldConfig": {"defaults": {"noValue": no_value, "mappings": ALERT_STATE_MAP,
                                         "custom": {"align": "auto",
                                                                         "cellOptions": {"type": "auto"}}},
                            "overrides": overrides},
            "transformations": [{"id": "organize", "options": organize}],
            "options": {"showHeader": True, "cellHeight": "sm",
                        "sortBy": [{"displayName": sort, "desc": not sort_asc}] if sort else []},
        }, w, h)

    def state(self, title, targets, description, mappings, w=24, h=6, steps=None):
        self.add({
            "type": "state-timeline", "title": title, "description": description, "datasource": PROM,
            "targets": [prom(e, l, ref=chr(65 + i)) for i, (e, l) in enumerate(targets)],
            "fieldConfig": {"defaults": {"noValue": NO_DATA, "mappings": mappings,
                                         "color": {"mode": "thresholds"},
                                         "thresholds": thresholds(steps or [RED, (1, GREEN)]),
                                         "custom": {"fillOpacity": 75, "lineWidth": 0}},
                            "overrides": []},
            "options": {"showValue": "never", "mergeValues": True, "alignValue": "left",
                        "rowHeight": 0.85, "legend": {"showLegend": False, "displayMode": "list",
                                                      "placement": "bottom"},
                        "tooltip": {"mode": "single"}},
        }, w, h)

    def logs(self, title, expr, description, w=24, h=10):
        self.add({
            "type": "logs", "title": title, "description": description, "datasource": LOKI,
            "targets": [{"datasource": LOKI, "expr": expr, "refId": "A", "queryType": "range",
                         "maxLines": 200}],
            "options": {"showTime": True, "wrapLogMessage": True, "enableLogDetails": True,
                        "prettifyLogMessage": False, "sortOrder": "Descending", "dedupStrategy": "none",
                        "showLabels": False, "showCommonLabels": False},
        }, w, h)

    def text(self, title, content, w=24, h=4, description=None):
        panel = {"type": "text", "title": title, "options": {"mode": "markdown", "content": content}}
        if description:
            panel["description"] = description
        self.add(panel, w, h)

    def build(self):
        return {
            "uid": UIDS[self.key], "title": self.title, "description": self.description,
            "tags": ["gwangsan"], "timezone": "Asia/Seoul", "weekStart": "monday",
            "refresh": "30s", "time": {"from": "now-1h", "to": "now"},
            "timepicker": {"refresh_intervals": ["30s", "1m", "5m", "15m"]},
            "graphTooltip": 1, "editable": False, "liveNow": False, "fiscalYearStartMonth": 0,
            "schemaVersion": 41, "version": 1,
            "links": [{"type": "dashboards", "tags": ["gwangsan"], "asDropdown": True,
                       "title": "광산 운영 관제", "keepTime": True, "includeVars": True,
                       "icon": "external link", "targetBlank": False}],
            "annotations": {"list": [builtin_annotation()] + self.annotations},
            "templating": {"list": self.vars},
            "panels": self.panels,
        }


def env_var():
    return {"name": "environment", "label": "환경", "type": "query", "datasource": PROM,
            "query": {"query": 'label_values(up{collector="gwangsan-aws-alloy"}, environment)',
                      "refId": "env"},
            "definition": 'label_values(up{collector="gwangsan-aws-alloy"}, environment)',
            "refresh": 2, "includeAll": True, "allValue": ".*", "multi": False,
            "current": {"text": "All", "value": "$__all"}, "sort": 1}


def query_var(name, label, query, multi=True):
    return {"name": name, "label": label, "type": "query", "datasource": PROM,
            "query": {"query": query, "refId": name}, "definition": query, "refresh": 2,
            "includeAll": True, "allValue": ".*", "multi": multi,
            "current": {"text": "All", "value": "$__all"}, "sort": 1}


def custom_var(name, label, options, all_value=".*"):
    return {"name": name, "label": label, "type": "custom", "query": ",".join(options),
            "includeAll": True, "allValue": all_value, "multi": True,
            "current": {"text": "All", "value": "$__all"},
            "options": []}


def text_var(name, label):
    return {"name": name, "label": label, "type": "textbox", "query": "", "current": {"text": "", "value": ""}}


def builtin_annotation():
    return {"builtIn": 1, "datasource": {"type": "grafana", "uid": "-- Grafana --"}, "enable": True,
            "hide": True, "iconColor": "rgba(0, 211, 255, 1)", "name": "Annotations & Alerts",
            "type": "dashboard"}


def restart_annotation():
    # 배포 이력 저장소가 없으므로 프로세스 시작 시각 변화로 '재시작(배포 추정)'을 표시한다.
    return {"name": "앱 재시작(배포 추정)", "datasource": PROM, "enable": True, "iconColor": ORANGE,
            "expr": f'changes(process_start_time_seconds{{service=~"gwangsan-api|gwangsan-chat",{E}}}[2m]) > 0',
            "step": "1m", "titleFormat": "{{service}} 재시작", "textFormat": "프로세스 시작 시각 변경"}


def level_color(level, color):
    return {"matcher": {"id": "byRegexp", "options": f".* {level}$"},
            "properties": [{"id": "color", "value": {"mode": "fixed", "fixedColor": color}}]}


def fixed_color(name, color):
    return {"matcher": {"id": "byName", "options": name},
            "properties": [{"id": "color", "value": {"mode": "fixed", "fixedColor": color}}]}


def min_width(matcher, px):
    # 표 셀이 글자 중간에서 잘리지 않도록 최소 폭만 지정한다(넘치면 native 가로 스크롤).
    return {"matcher": matcher, "properties": [{"id": "custom.minWidth", "value": px}]}


def service_colors():
    def ov(regex, color):
        return {"matcher": {"id": "byRegexp", "options": regex},
                "properties": [{"id": "color", "value": {"mode": "fixed", "fixedColor": color}}]}
    return [ov(".*(API|gwangsan-api).*", BLUE), ov(".*(채팅|gwangsan-chat).*", TEAL),
            ov(".*(Redis|redis).*", PURPLE)]


UP_MAP = [{"type": "value", "options": {"1": {"text": "정상", "color": GREEN, "index": 0},
                                        "0": {"text": "수집 실패", "color": RED, "index": 1}}}]
TARGET_UP_MAP = [{"type": "value", "options": {"1": {"text": "정상", "color": GREEN, "index": 0},
                                               "0": {"text": "응답 없음", "color": RED, "index": 1}}}]
OK_MAP = [{"type": "value", "options": {"1": {"text": "성공", "color": GREEN, "index": 0},
                                        "0": {"text": "실패", "color": RED, "index": 1}}}]
ALERT_STATE_MAP = [{"type": "value", "options": {"firing": {"text": "발생", "color": RED, "index": 0},
                                                "pending": {"text": "대기(지속 조건 확인 중)", "color": ORANGE,
                                                            "index": 1}}}]
COMPLETE_MAP = [{"type": "value", "options": {"1": {"text": "완전", "color": GREEN, "index": 0},
                                              "0": {"text": "부분 집계", "color": ORANGE, "index": 1}}}]


def hq(q, metric, sel, by="", window="5m"):
    by_clause = f"le{', ' + by if by else ''}"
    return f"histogram_quantile({q}, sum by ({by_clause}) (rate({metric}_bucket{{{sel}}}[{window}])))"


def rate(metric, sel, window="$__rate_interval", by=None):
    inner = f"rate({metric}{{{sel}}}[{window}])"
    return f"sum by ({by}) ({inner})" if by else f"sum({inner})"


# ---------------------------------------------------------------- 00 서비스 현황
def overview():
    b = Board("overview", "00 · 서비스 현황",
              "광산 운영 관제 홈. 장애 서비스를 찾고 상세 화면으로 이동한다.",
              annotations=[restart_annotation()])
    complete = f"on() (min(gwangsan_stream_snapshot_complete{{{E}}}) == 1)"
    b.stat("API 요청/초", rate("http_server_requests_seconds_count", API), PER_SEC,
           desc("API가 처리 완료한 HTTP 요청 수의 초당 비율", "Spring Micrometer http_server_requests",
                "건/초", "$__rate_interval", "API 지표가 없으면 '미수집'. 0은 실제 요청 없음",
                "01 · API 요청·외부 연동"), links=[link("api", "API 상세")], decimals=2)
    b.stat("API 5xx",
           f'{rate("http_server_requests_seconds_count", API + ",status=~\"5..\"", "5m")} / '
           f'({rate("http_server_requests_seconds_count", API, "5m")} > 0)', "percentunit",
           desc("5분 동안 5xx 응답 / 전체 응답", "http_server_requests_seconds_count{status}",
                "비율(%)", "5분", "요청이 0건이면 계산하지 않음(요청 없음 또는 미수집)",
                "01 · API, 07 · 로그"), no_value=NO_REQ,
           steps=[GREEN, (0.01, ORANGE), (0.05, RED)], links=[link("api", "API 상세")], decimals=2)
    b.stat("API p95", hq(0.95, "http_server_requests_seconds", API), "s",
           desc("전체 경로 합산 히스토그램의 95번째 백분위 응답 시간",
                "http_server_requests_seconds_bucket (Micrometer histogram 활성화 필요)",
                "초", "5분", "요청·버킷이 없으면 '요청 없음 또는 미수집'. 인스턴스별 p95를 평균내지 않음",
                "01 · API"), no_value=NO_REQ, steps=[GREEN, (0.5, ORANGE), (1, RED)],
           links=[link("api", "API 상세")])
    b.stat("채팅 연결 수", f"sum(gwangsan_chat_connections{{{CHAT}}})", EA,
           desc("현재 Socket.IO 연결 수", "채팅 서버 gwangsan_chat_connections", "개", "현재값",
                "채팅 지표가 없으면 '미수집'. 재시작 시 0부터 다시 증가", "03 · 채팅·NestJS"),
           links=[link("chat", "채팅 상세")])
    b.stat("읽기 전 대기", f"sum(gwangsan_stream_unread{{{E}}}) and {complete}", COUNT,
           desc("소비 그룹에 아직 전달되지 않은 메시지 수(XINFO GROUPS lag 합)",
                "API Streams 집계 gwangsan_stream_unread", "건", "30초 캐시 집계",
                "lag 미지원·부분 집계면 합계를 표시하지 않음(계산 불가). XLEN 보관량과 다름",
                "04 · Redis Streams 처리"), no_value=UNKNOWN,
           steps=[GREEN, (100, ORANGE), (1000, RED)], links=[link("streams", "Streams 상세")])
    b.stat("ACK 대기", f"sum(gwangsan_stream_pending{{{E}}}) and {complete}", COUNT,
           desc("전달됐지만 아직 ACK되지 않은 메시지 수(PEL pending 합)",
                "API Streams 집계 gwangsan_stream_pending", "건", "30초 캐시 집계",
                "부분 집계면 합계를 표시하지 않음. 읽기 전 대기와 합산하지 않음",
                "04 · Redis Streams 처리"), no_value=UNKNOWN,
           steps=[GREEN, (50, ORANGE), (500, RED)], links=[link("streams", "Streams 상세")])

    b.bars("채팅 처리 흐름", [
        (rate("gwangsan_chat_events_total", CHAT + ',event="sendMessage",outcome="success"', "5m"), "① 수신"),
        (rate("gwangsan_chat_publish_duration_seconds_count", CHAT + ',outcome="success"', "5m"), "② 발행"),
        (rate("gwangsan_chat_persist_attempts_total", E + ',outcome="success"', "5m"), "③ 저장"),
        (rate("gwangsan_chat_ack_total", E + ',outcome="success"', "5m"), "④ ACK")], PER_SEC,
        desc("파이프라인 단계별 성공 처리율(① 수신=채팅 sendMessage 성공, ② 발행=Redis 발행 성공, "
             "③ 저장=Spring DB 저장 커밋, ④ ACK=Streams ACK 성공). 단계마다 세는 대상이 달라(재시도·재전송·채팅 외 발행 포함) 값이 같을 필요가 없다. "
             "같은 값이어도 수신자 전달·누락 없음을 뜻하지 않는다. 소켓 송출은 수신 확인이 아니다",
             "채팅 events/publish, API persist/ack 카운터", "건/초", "5분",
             "막대가 없는 단계 = 해당 지표 미수집(0으로 채우지 않음)", "03 · 채팅, 04 · Streams"),
        h=6, links=[link("streams", "Streams 상세")])
    b.ts("API 요청·5xx", [(rate("http_server_requests_seconds_count", API), "API 요청/초"),
                         (rate("http_server_requests_seconds_count", API + ',status=~"5.."'), "API 5xx/초")],
         PER_SEC, desc("요청/초와 5xx/초", "http_server_requests_seconds_count", "건/초",
                       "$__rate_interval", "선이 끊기면 미수집 구간", "01 · API"),
         links=[link("api", "API 상세")])
    b.ts("채팅 저장 지연", [
        (hq(0.95, "gwangsan_chat_publish_to_commit_seconds", E), "발행→DB 커밋 p95"),
        (hq(0.95, "gwangsan_chat_persist_duration_seconds", E + ',outcome="success"'), "소비→커밋 p95")],
         "s", desc("성공 커밋 기준 지연. 아직 완료되지 않은 메시지는 포함되지 않으므로 대기 건수와 함께 본다",
                   "API histogram", "초", "5분", "성공 커밋이 없으면 선이 없음", "04 · Streams"),
         steps=[GREEN, (5, ORANGE), (30, RED)], links=[link("streams", "Streams 상세")])

    b.table("진행 중 경보", f'ALERTS{{alertstate=~"firing|pending"}}',
            desc("Prometheus 규칙 평가 결과 중 발생·대기 경보", "학교 Prometheus ALERTS",
                 "-", "현재", "없으면 '진행 중 경보 없음'. 학교 VM 전체 중단은 여기서 감지 불가",
                 "경보 runbook: docs/observability/operations/alerts.md"),
            {"alertname": "경보", "severity": "심각도", "alertstate": "상태", "service": "서비스",
             "environment": "환경"}, no_value="진행 중 경보 없음", widths={"서비스": 130})
    b.state("수집 상태 이력", [(f"max by (service, environment) (up{{{AWS}}})", "{{service}} · {{environment}}")],
            desc("AWS 대상(서비스·환경)별 scrape 성공 여부", "Alloy scrape up → remote_write", "정상/수집 실패",
                 "scrape 15초", "회색 빈 구간 = 학교에 샘플이 도착하지 않음(미수집)", "08 · 수집·저장 상태"),
            UP_MAP, w=12, h=8)
    b.panels[-1]["fieldConfig"]["defaults"]["custom"]["axisWidth"] = 190
    return b


# ---------------------------------------------------------------- 01 API
def api():
    b = Board("api", "01 · API 요청·외부 연동", "Spring API 처리량·오류·지연, 외부 호출, 관련 로그·트레이스.",
              extra_vars=[query_var("uri", "경로 템플릿",
                                    f'label_values(http_server_requests_seconds_count{{{API}}}, uri)')],
              annotations=[restart_annotation()])
    S = API + ',uri=~"$uri"'
    b.stat("처리량", rate("http_server_requests_seconds_count", S), PER_SEC,
           desc("완료된 HTTP 요청/초", "http_server_requests_seconds_count", "건/초", "$__rate_interval",
                "미수집과 0(요청 없음)을 구분", "아래 상태 코드 분포"), w=6, decimals=2)
    for title, pat, steps in (("4xx 비율", "4..", [GREEN, (0.1, ORANGE), (0.3, RED)]),
                              ("5xx 비율", "5..", [GREEN, (0.01, ORANGE), (0.05, RED)])):
        b.stat(title, f'{rate("http_server_requests_seconds_count", S + f",status=~\"{pat}\"", "5m")} / '
                      f'({rate("http_server_requests_seconds_count", S, "5m")} > 0)', "percentunit",
               desc(f"5분 {pat} 응답 비율", "http_server_requests_seconds_count{status}", "%", "5분",
                    "요청 0건이면 계산하지 않음", "오류 경로 순위, API 오류 로그"),
               w=6, no_value=NO_REQ, steps=steps, decimals=2)
    b.stat("처리 중 요청", f"sum(http_server_requests_active_seconds_active_count{{{API}}})", EA,
           desc("현재 처리 중인 요청 수", "Micrometer http_server_requests_active (LongTaskTimer)", "개",
                "현재값", "Spring 버전·설정상 미노출이면 '미수집'", "02 · JVM·Spring의 Tomcat 스레드"), w=6)
    b.ts("응답 시간 백분위", [(hq(q, "http_server_requests_seconds", S), f"p{int(q * 100)}")
                         for q in (0.5, 0.95, 0.99)], "s",
         desc("경로 필터 적용 후 합산 히스토그램 백분위", "http_server_requests_seconds_bucket", "초", "5분",
              "표본이 적으면 p99는 해석 제한", "느린 경로 순위, 07 · 트레이스"),
         steps=[GREEN, (0.5, ORANGE), (1, RED)])
    b.ts("상태 코드별 요청", [(rate("http_server_requests_seconds_count", S, by="status"), "{{status}}")],
         PER_SEC, desc("HTTP 상태 코드별 요청/초", "http_server_requests_seconds_count", "건/초",
                       "$__rate_interval", "미수집 구간은 선 없음", "오류 경로 순위"), stack=True)
    b.table("느린 경로 Top 10 (p95)",
            f"topk(10, {hq(0.95, 'http_server_requests_seconds', S, by='uri', window='$__range')})",
            desc("선택 기간 경로 템플릿별 p95", "http_server_requests_seconds_bucket", "초", "선택한 시간 범위",
                 "요청이 없는 경로는 제외", "07 · 트레이스에서 느린 trace 검색"),
            {"uri": "경로 템플릿", "Value": "p95"}, unit="s", sort="p95")
    b.table("오류 경로 Top 10 (5xx)",
            f'topk(10, sum by (uri, status) (increase(http_server_requests_seconds_count{{{S},status=~"5.."}}[$__range])) > 0)',
            desc("선택 기간 5xx 발생 경로", "http_server_requests_seconds_count", "건", "선택한 시간 범위",
                 "5xx가 없으면 '해당 없음'", "아래 API 오류 로그"),
            {"uri": "경로 템플릿", "status": "상태", "Value": "건수"}, unit="short", sort="건수")
    b.row("외부·내부 연동 (HTTP 클라이언트)")
    b.ts("연동 호출 p95", [(hq(0.95, "http_client_requests_seconds", API, by="client_name"), "{{client_name}}")],
         "s", desc("RestClient/RestTemplate 등 관측된 외부 호출(채팅 내부 HTTP 포함)",
                   "http_client_requests_seconds_bucket", "초", "5분",
                   "클라이언트 관측이 없으면 '미수집'", "07 · 트레이스"))
    b.ts("연동 실패/초", [(f'sum by (client_name, status) (rate(http_client_requests_seconds_count{{{API},outcome!="SUCCESS"}}[$__rate_interval]))',
                       "{{client_name}} {{status}}")], PER_SEC,
         desc("SUCCESS 외 결과의 외부 호출/초", "http_client_requests_seconds_count{outcome}", "건/초",
              "$__rate_interval", "실패가 없으면 선 없음", "API 오류 로그"))
    b.logs("API 오류 로그", '{container="gwangsan-container", level="ERROR"}',
           desc("API 컨테이너 ERROR 로그. JSON 로그의 trace_id는 상세 보기에서 Tempo로 연결",
                "기존 Promtail → Loki (container 라벨 유지)", "-", "선택한 시간 범위(최대 200줄)",
                "구형 평문 로그에는 trace 링크 없음", "07 · 로그·분산 추적"))
    return b


# ---------------------------------------------------------------- 02 JVM
def jvm():
    b = Board("jvm", "02 · JVM·Spring", "API JVM 메모리·GC·스레드, Tomcat, Hikari, 비동기 실행기.",
              annotations=[restart_annotation()])
    b.stat("힙 사용률", f'sum(jvm_memory_used_bytes{{{API},area="heap"}}) / sum(jvm_memory_max_bytes{{{API},area="heap"}} > 0)',
           "percentunit", desc("힙 사용량 / 최대 힙", "jvm_memory_used_bytes·max_bytes{area=heap}", "%",
                               "현재값", "최대값 미정(-1) 풀은 제외", "힙·비힙 추세"),
           steps=[GREEN, (0.8, ORANGE), (0.9, RED)], decimals=1)
    b.stat("CPU 사용률", f"max(process_cpu_usage{{{API}}})", "percentunit",
           desc("JVM 프로세스 CPU 사용률(호스트 전체 코어 대비)", "process_cpu_usage", "%", "현재값",
                "미수집 시 표시 안 함", "06 · 서버"), steps=[GREEN, (0.7, ORANGE), (0.9, RED)], decimals=1)
    b.stat("GC 정지율", f"sum(rate(jvm_gc_pause_seconds_sum{{{API}}}[5m]))", "percentunit",
           desc("초당 GC 정지 시간(=정지 비율)", "jvm_gc_pause_seconds_sum", "%", "5분", "GC가 없으면 0",
                "GC 추세"), steps=[GREEN, (0.05, ORANGE), (0.1, RED)], decimals=2)
    b.stat("스레드 수", f"sum(jvm_threads_live_threads{{{API}}})", EA,
           desc("JVM 라이브 스레드 수", "jvm_threads_live_threads", "개", "현재값", "-", "스레드 상태"))
    b.stat("DB 풀 사용", f"sum(hikaricp_connections_active{{{API}}}) / sum(hikaricp_connections_max{{{API}}})",
           "percentunit", desc("Hikari active / max", "hikaricp_connections_*", "%", "현재값",
                               "풀 미초기화면 미수집", "Hikari 패널, 05 · MariaDB"),
           steps=[GREEN, (0.8, ORANGE), (0.95, RED)], decimals=0)
    b.stat("커넥션 대기", f"sum(hikaricp_connections_pending{{{API}}})", EA,
           desc("커넥션을 기다리는 스레드 수", "hikaricp_connections_pending", "개", "현재값",
                "0 = 대기 없음", "DB연결대기지속 경보 runbook"), steps=[GREEN, (1, ORANGE), (5, RED)])
    b.ts("힙·비힙 메모리", [
        (f'sum by (area) (jvm_memory_used_bytes{{{API}}})', "사용 {{area}}"),
        (f'sum by (area) (jvm_memory_committed_bytes{{{API}}})', "커밋 {{area}}"),
        (f'sum(jvm_memory_max_bytes{{{API},area="heap"}} > 0)', "최대 heap")], "bytes",
         desc("영역별 사용·커밋·최대 메모리", "jvm_memory_*_bytes", "바이트", "scrape 15초", "-", "GC 추세"))
    b.ts("GC 정지", [(f'sum by (action, cause) (rate(jvm_gc_pause_seconds_sum{{{API}}}[$__rate_interval]))',
                    "{{action}} · {{cause}}")], "percentunit",
         desc("원인별 초당 GC 정지 시간", "jvm_gc_pause_seconds_sum", "%", "$__rate_interval", "-", "힙 추세"))
    b.ts("스레드 상태", [(f'sum by (state) (jvm_threads_states_threads{{{API}}})', "{{state}}")], EA,
         desc("상태별 스레드 수", "jvm_threads_states_threads", "개", "scrape 15초", "-", "Tomcat 스레드"),
         stack=True)
    b.ts("Tomcat 요청 스레드", [(f"sum(tomcat_threads_busy_threads{{{API}}})", "사용 중"),
                             (f"sum(tomcat_threads_config_max_threads{{{API}}})", "최대")], EA,
         desc("Tomcat 작업 스레드 사용/최대", "tomcat_threads_* (server.tomcat.mbeanregistry.enabled 필요)",
              "개", "scrape 15초", "MBean 등록이 꺼져 있으면 '미수집'", "01 · API 처리 중 요청"))
    b.ts("Hikari 커넥션", [(f"sum(hikaricp_connections_active{{{API}}})", "active"),
                         (f"sum(hikaricp_connections_idle{{{API}}})", "idle"),
                         (f"sum(hikaricp_connections_pending{{{API}}})", "pending"),
                         (f"sum(hikaricp_connections_max{{{API}}})", "max")], EA,
         desc("커넥션 풀 상태", "hikaricp_connections_*", "개", "scrape 15초", "-", "05 · MariaDB 연결"))
    b.ts("Hikari 타임아웃·획득 시간", [
        (f"sum(increase(hikaricp_connections_timeout_total{{{API}}}[$__rate_interval]))", "타임아웃 건수"),
        (f"max(hikaricp_connections_acquire_seconds_max{{{API}}})", "획득 최대(초)")], "short",
         desc("커넥션 획득 타임아웃 발생 건수와 최대 획득 시간", "hikaricp_connections_timeout_total, acquire_seconds_max",
              "건 / 초", "$__rate_interval", "-", "DB 연결 대기"))
    b.ts("비동기 실행기", [(f"sum by (name) (executor_active_threads{{{API}}})", "active {{name}}"),
                       (f"sum by (name) (executor_queued_tasks{{{API}}})", "대기 {{name}}"),
                       (f'sum by (name) (rate(executor_rejected_tasks_total{{{API}}}[$__rate_interval]))', "거부/초 {{name}}")],
         "short", desc("@Async·스케줄러 실행기 상태", "executor_* (Spring이 등록한 실행기만)", "개 / 건/초",
                       "scrape 15초", "등록되지 않은 실행기는 표시하지 않음", "04 · Streams"))
    b.stat("API 가동 시간", f"time() - max(process_start_time_seconds{{{API}}})", "s",
           desc("마지막 프로세스 시작 이후 경과", "process_start_time_seconds", "초", "현재값",
                "미수집이면 표시 안 함", "재시작 주석"), w=12, h=8)
    return b


# ---------------------------------------------------------------- 03 채팅
def chat():
    b = Board("chat", "03 · 채팅·NestJS", "Socket.IO 이벤트, Redis 발행, Node 런타임.",
              annotations=[restart_annotation()])
    b.stat("현재 연결", f"sum(gwangsan_chat_connections{{{CHAT}}})", EA,
           desc("Socket.IO 연결 수", "gwangsan_chat_connections", "개", "현재값", "재시작 시 0부터", "연결 추세"))
    b.stat("이벤트 수신", rate("gwangsan_chat_events_total", CHAT, "5m"), PER_SEC,
           desc("모든 Socket.IO 이벤트 처리 건/초", "gwangsan_chat_events_total", "건/초", "5분",
                "미수집과 0 구분", "이벤트별 추세"), decimals=2)
    b.stat("발행 성공률",
           f'{rate("gwangsan_chat_publish_duration_seconds_count", CHAT + ",outcome=\"success\"", "5m")} / '
           f'({rate("gwangsan_chat_publish_duration_seconds_count", CHAT, "5m")} > 0)', "percentunit",
           desc("발행 성공 / 전체 발행 시도", "gwangsan_chat_publish_duration_seconds_count{outcome}", "%",
                "5분", "발행 0건이면 계산하지 않음", "발행 실패 추세, 채팅 오류 로그"),
           no_value=NO_REQ, steps=[RED, (0.95, ORANGE), (0.99, GREEN)], decimals=2)
    b.stat("발행 p95", hq(0.95, "gwangsan_chat_publish_duration_seconds", CHAT), "s",
           desc("Redis XADD 완료까지 p95", "gwangsan_chat_publish_duration_seconds_bucket", "초", "5분",
                "발행 없으면 계산 불가", "05 · Redis"), no_value=NO_REQ,
           steps=[GREEN, (0.05, ORANGE), (0.25, RED)])
    b.stat("루프 p99", f"max(nodejs_eventloop_lag_p99_seconds{{{CHAT}}})", "s",
           desc("Node 이벤트 루프 지연 p99", "prom-client nodejs_eventloop_lag_p99_seconds", "초", "현재값",
                "-", "Node 이벤트 루프 경보"), steps=[GREEN, (0.05, ORANGE), (0.1, RED)])
    b.stat("RSS 메모리", f"max(process_resident_memory_bytes{{{CHAT}}})", "bytes",
           desc("Node 프로세스 상주 메모리", "process_resident_memory_bytes", "바이트", "현재값", "-", "힙 추세"))
    b.ts("이벤트별 처리/초", [(rate("gwangsan_chat_events_total", CHAT, by="event, outcome"),
                         "{{event}} · {{outcome}}")], PER_SEC,
         desc("허용 목록 이벤트·결과별 처리량(미등록 이벤트는 other)", "gwangsan_chat_events_total", "건/초",
              "$__rate_interval", "-", "실패 이벤트"))
    b.ts("실패 이벤트/초", [(f'sum by (event, outcome) (rate(gwangsan_chat_events_total{{{CHAT},outcome!="success"}}[$__rate_interval]))',
                         "{{event}} · {{outcome}}")], PER_SEC,
         desc("success 외 결과(인증 실패·검증 실패·발행 실패 등 소유 측 정의)", "gwangsan_chat_events_total",
              "건/초", "$__rate_interval", "실패가 없으면 선 없음", "채팅 오류 로그"))
    b.ts("Redis 발행 지연", [(hq(q, "gwangsan_chat_publish_duration_seconds", CHAT), f"p{int(q * 100)}")
                         for q in (0.5, 0.95, 0.99)], "s",
         desc("발행 지연 백분위", "gwangsan_chat_publish_duration_seconds_bucket", "초", "5분", "-", "05 · Redis"))
    b.ts("이벤트 처리 p95 (이벤트별)", [(hq(0.95, "gwangsan_chat_event_duration_seconds", CHAT, by="event"), "{{event}}")],
         "s", desc("Socket.IO 이벤트 핸들러 처리 시간 p95(소켓 송출 포함, 수신자 도착 아님)",
                   "gwangsan_chat_event_duration_seconds_bucket (채팅 서버 계측)", "초", "5분",
                   "이벤트가 없으면 선 없음", "Redis 발행 지연, 07 · 트레이스"),
         steps=[GREEN, (0.25, ORANGE), (1, RED)])
    b.ts("연결 수", [(f"sum(gwangsan_chat_connections{{{CHAT}}})", "채팅 연결")], EA,
         desc("연결 수 추세. 급감은 재시작·네트워크 문제 가능", "gwangsan_chat_connections", "개",
              "scrape 15초", "-", "재시작 주석"))
    b.ts("Node 힙", [(f"sum(nodejs_heap_size_used_bytes{{{CHAT}}})", "사용"),
                    (f"sum(nodejs_heap_size_total_bytes{{{CHAT}}})", "할당")], "bytes",
         desc("V8 힙 사용/할당", "nodejs_heap_size_*_bytes", "바이트", "scrape 15초", "-", "RSS"))
    b.ts("GC·CPU", [(f'sum by (kind) (rate(nodejs_gc_duration_seconds_sum{{{CHAT}}}[$__rate_interval]))', "GC {{kind}}"),
                   (f"sum(rate(process_cpu_seconds_total{{{CHAT}}}[$__rate_interval]))", "CPU 코어")], "short",
         desc("GC 시간 비율과 CPU 코어 사용", "nodejs_gc_duration_seconds, process_cpu_seconds_total",
              "초/초", "$__rate_interval", "-", "06 · 서버"), w=24)
    b.ts("활성 핸들", [(f"sum(nodejs_active_handles_total{{{CHAT}}})", "핸들"),
                    (f"sum(nodejs_active_requests_total{{{CHAT}}})", "요청")], EA,
         desc("libuv 활성 핸들·요청. 지속 증가는 누수 가능", "nodejs_active_*_total", "개", "scrape 15초",
              "-", "연결 수"), w=24, h=7)
    b.logs("채팅 오류·경고 로그", '{container="gwangsan-chat", level=~"ERROR|WARN"}',
           desc("채팅 컨테이너 ERROR/WARN 로그. 채팅 본문은 수집 대상이 아님",
                "기존 Promtail → Loki", "-", "선택 기간(최대 200줄)", "구형 Nest 로그는 level 라벨이 없어 제외될 수 있음",
                "07 · 로그·분산 추적"))
    return b


# ---------------------------------------------------------------- 04 Streams
def streams():
    b = Board("streams", "04 · Redis Streams 처리", "채팅 저장 파이프라인: 발행 → 소비 → DB 커밋 → ACK, 재시도·DLQ, 집계 상태.",
              extra_vars=[query_var("group", "스트림 범위", f"label_values(gwangsan_stream_pending{{{E}}}, group)")],
              annotations=[restart_annotation()])
    G = E + ',group=~"$group"'
    complete = f'on() (min(gwangsan_stream_snapshot_complete{{{G}}}) == 1)'
    b.stat("읽기 전 대기", f"sum(gwangsan_stream_unread{{{G}}}) and {complete}", COUNT,
           desc("아직 소비자에게 전달되지 않은 메시지(lag)", "gwangsan_stream_unread", "건", "30초 캐시",
                "lag null·부분 집계 → 계산 불가(0 아님)", "그룹별 추세"), no_value=UNKNOWN,
           steps=[GREEN, (100, ORANGE), (1000, RED)])
    b.stat("ACK 대기", f"sum(gwangsan_stream_pending{{{G}}}) and {complete}", COUNT,
           desc("전달 후 ACK 전(PEL)", "gwangsan_stream_pending", "건", "30초 캐시", "부분 집계 → 계산 불가",
                "그룹별 추세"), no_value=UNKNOWN, steps=[GREEN, (50, ORANGE), (500, RED)])
    b.stat("저장 성공/초", rate("gwangsan_chat_persist_attempts_total", E + ',outcome="success"', "5m"), PER_SEC,
           desc("DB 커밋 성공 처리 시도/초", "gwangsan_chat_persist_attempts_total{outcome=success}", "건/초",
                "5분", "-", "처리 시도 결과"), decimals=2)
    b.stat("재시도(1h)", f'sum(increase(gwangsan_chat_retry_total{{{E},outcome="success"}}[1h]))', COUNT,
           desc("최근 1시간 재시도 스트림 이동 성공 건수(보관량 XLEN 아님, XADD 실패는 제외 — 아래 추세의 failure)",
                "gwangsan_chat_retry_total{outcome=success}", "건", "1시간",
                "카운터 없음 = 미수집", "재시도·DLQ 추세"), steps=[GREEN, (1, ORANGE), (50, RED)], decimals=0)
    b.stat("DLQ(1h)", f'sum(increase(gwangsan_chat_dlq_total{{{E},outcome="success"}}[1h]))', COUNT,
           desc("최근 1시간 DLQ 이동 성공 건수. DLQ XADD 실패(outcome=failure)는 이동이 아니므로 제외하고 아래 추세에 따로 표시",
                "gwangsan_chat_dlq_total{outcome=success}", "건", "1시간", "-", "채팅최종실패발생 runbook"),
           steps=[GREEN, (1, RED)], decimals=0)
    b.stat("커밋 p95", hq(0.95, "gwangsan_chat_publish_to_commit_seconds", E), "s",
           desc("최초 발행부터 DB 커밋까지(성공 건만). 미완료 메시지는 제외되므로 대기 건수와 함께 해석",
                "gwangsan_chat_publish_to_commit_seconds", "초", "5분", "성공 커밋 없음 → 계산 불가",
                "지연 추세"), no_value=NO_REQ, steps=[GREEN, (5, ORANGE), (30, RED)])

    b.row("집계 상태 (정확성 판단 근거)")
    b.stat("집계 경과 시간", f"time() - max(gwangsan_stream_snapshot_timestamp_seconds{{{G}}})", "s",
           # 한 번도 성공하지 못하면 NaN → '미수집'

           desc("마지막 정상 Streams 집계 이후 경과", "gwangsan_stream_snapshot_timestamp_seconds", "초", "현재값",
                "120초 초과 시 위 대기 값은 오래된 값", "API 로그"), w=8,
           steps=[GREEN, (60, ORANGE), (120, RED)])
    b.stat("집계 성공", f"min(gwangsan_stream_snapshot_success{{{G}}})", "none",
           desc("가장 최근 집계 성공 여부(그룹 최솟값)", "gwangsan_stream_snapshot_success", "성공/실패",
                "현재값", "-", "Streams집계실패 경보"), w=8, mappings=OK_MAP, steps=[RED, (1, GREEN)])
    b.stat("집계 범위", f"min(gwangsan_stream_snapshot_complete{{{G}}})", "none",
           desc("모든 대상 스트림을 예산 안에 집계했는지", "gwangsan_stream_snapshot_complete", "완전/부분",
                "현재값", "부분이면 합계 패널은 계산 불가로 표시", "API 집계 예산"), w=8,
           mappings=COMPLETE_MAP, steps=[ORANGE, (1, GREEN)])
    b.state("그룹별 집계 상태", [(f"min by (group) (gwangsan_stream_snapshot_success{{{G}}})", "성공 {{group}}"),
                            (f"min by (group) (gwangsan_stream_snapshot_complete{{{G}}})", "완전 {{group}}")],
            desc("시간에 따른 집계 성공·완전성", "gwangsan_stream_snapshot_*", "-", "scrape 15초",
                 "회색 = 미수집", "API 로그"), OK_MAP, h=5)

    b.row("대기·처리")
    b.ts("읽기 전 대기 (그룹별)", [(f"sum by (group) (gwangsan_stream_unread{{{G}}})", "{{group}}")], COUNT,
         desc("범위(rooms=채팅방 스트림 합, retry=재시도 스트림)별 lag. 소비 그룹 이름이 아님. 합은 고유 메시지 수와 다를 수 있음", "gwangsan_stream_unread", "건", "30초 캐시",
              "lag 미지원 그룹은 선 없음(0 아님)", "ACK 대기"))
    b.ts("ACK 대기 (그룹별)", [(f"sum by (group) (gwangsan_stream_pending{{{G}}})", "{{group}}")], COUNT,
         desc("그룹별 PEL 건수. XPENDING idle은 원래 대기 시간이 아님", "gwangsan_stream_pending", "건",
              "30초 캐시", "-", "저장 시도 결과"))
    b.ts("저장 시도 결과", [(rate("gwangsan_chat_persist_attempts_total", E, by="outcome"), "{{outcome}}")],
         PER_SEC, desc("처리 시도별 결과(재시도 포함, 고유 메시지 수 아님)", "gwangsan_chat_persist_attempts_total",
                       "건/초", "$__rate_interval", "-", "API 오류 로그"))
    b.ts("ACK 결과", [(rate("gwangsan_chat_ack_total", E, by="outcome"), "{{outcome}}")], PER_SEC,
         desc("XACK 결과. not_pending(이미 ACK됨·PEL에 없음)은 운영 실패가 아니며 failure만 실패", "gwangsan_chat_ack_total", "건/초", "$__rate_interval", "-", "05 · Redis"))
    b.ts("재시도·DLQ 신규 유입", [(rate("gwangsan_chat_retry_total", E, by="outcome"), "재시도 {{outcome}}"),
                             (rate("gwangsan_chat_dlq_total", E, by="outcome"), "DLQ {{outcome}}")], PER_SEC,
         desc("재시도/DLQ 인계 시도율. success=이동 성공, failure=인계 XADD 실패. 스트림 보관량(XLEN)과 다름", "gwangsan_chat_retry_total, dlq_total",
              "건/초", "$__rate_interval", "-", "07 · 실패 trace"))
    b.ts("처리 지연", [(hq(0.95, "gwangsan_chat_persist_duration_seconds", E, by="outcome"), "소비→커밋 p95 {{outcome}}"),
                    (hq(0.5, "gwangsan_chat_publish_to_commit_seconds", E), "발행→커밋 p50"),
                    (hq(0.95, "gwangsan_chat_publish_to_commit_seconds", E), "발행→커밋 p95"),
                    (hq(0.99, "gwangsan_chat_publish_to_commit_seconds", E), "발행→커밋 p99")], "s",
         desc("성공 커밋 기준 지연 백분위", "API histogram", "초", "5분",
              "서버 간 시계 오차는 계측 오류로 별도 집계(소유 측)", "02 · Hikari, 05 · MariaDB"))
    b.text("지표 해석 규칙", (
        "- 읽기 전 대기와 ACK 대기는 별개.\n"
        "- 두 대기 값을 합산하지 않습니다.\n"
        "- XLEN은 보관량입니다.\n"
        "- ACK 후에도 항목이 남습니다.\n"
        "- 재시도·DLQ는 신규 유입 수.\n"
        "- 지연은 성공 커밋만 집계합니다.\n"
        "- 미완료는 대기 건수로 봅니다.\n"
        "- 부분·오래된 집계는 계산 불가.\n"
        "- 메시지 원문은 표시하지 않습니다."),
           h=9, description="읽기 전 대기(lag)와 ACK 대기(pending)는 합산하지 않는다. XLEN은 ACK된 항목도 포함하는 보관량이다. 재시도·DLQ는 신규 유입 카운터이며 지연은 성공 완료건 기준이다. 부분·오래된 집계는 계산 불가로 표시한다.")
    return b


# ---------------------------------------------------------------- 05 MariaDB·Redis
def data():
    b = Board("data", "05 · MariaDB·Redis", "데이터 저장소 상태. exporter는 읽기 전용 계정 생성 후 활성화된다.")
    db = f'service="mariadb",{E}'
    rd = f'service="redis",{E}'
    b.text("활성화 상태", (
        "- 지표는 최소 권한 모니터링 계정과 `db-redis.alloy`를 배치한 뒤 수집됩니다.\n"
        "- 그 전에는 모든 패널이 `미수집`입니다. 장애도 정상도 아닙니다.\n"
        "- Redis Streams 처리 상태는 04 화면(API 집계)에서 봅니다."), h=6)
    b.row("MariaDB")
    b.stat("DB 상태", f"max(mysql_up{{{db}}})", "none",
           desc("exporter의 DB 접속 성공", "mysqld_exporter mysql_up", "정상/실패", "30초", "미활성 → 미수집",
                "MariaDB응답없음 runbook"), mappings=UP_MAP, steps=[RED, (1, GREEN)], w=8)
    b.stat("연결 사용률", f"max(mysql_global_status_threads_connected{{{db}}}) / max(mysql_global_variables_max_connections{{{db}}})",
           "percentunit", desc("연결 수 / max_connections", "global_status·variables", "%", "현재값", "-", "02 · Hikari"),
           steps=[GREEN, (0.7, ORANGE), (0.9, RED)], decimals=1, w=8)
    b.stat("쿼리/초", f"sum(rate(mysql_global_status_queries{{{db}}}[5m]))", PER_SEC,
           desc("서버 전체 쿼리 수/초", "mysql_global_status_queries", "건/초", "5분", "-", "슬로 쿼리"), decimals=1, w=8)
    b.stat("슬로 쿼리 (1시간)", f"sum(increase(mysql_global_status_slow_queries{{{db}}}[1h]))", COUNT,
           desc("long_query_time 초과 쿼리 증가분(본문 미수집)", "mysql_global_status_slow_queries", "건", "1시간",
                "-", "DB 서버 slow log(운영자 확인)"), steps=[GREEN, (1, ORANGE), (50, RED)], decimals=0, w=8)
    b.stat("버퍼풀 적중률",
           f"1 - sum(rate(mysql_global_status_innodb_buffer_pool_reads{{{db}}}[5m])) / (sum(rate(mysql_global_status_innodb_buffer_pool_read_requests{{{db}}}[5m])) > 0)",
           "percentunit", desc("InnoDB 버퍼풀 논리 읽기 중 디스크 읽기 제외 비율", "innodb_buffer_pool_reads·read_requests",
                               "%", "5분", "읽기 0건이면 계산 불가", "06 · 디스크 I/O"),
           no_value=NO_REQ, steps=[RED, (0.9, ORANGE), (0.99, GREEN)], decimals=2, w=8)
    b.stat("가동 시간", f"max(mysql_global_status_uptime{{{db}}})", "s",
           desc("MariaDB 가동 시간", "mysql_global_status_uptime", "초", "현재값", "-", "재시작 여부"), w=8)
    b.ts("연결·스레드", [(f"max(mysql_global_status_threads_connected{{{db}}})", "연결"),
                      (f"max(mysql_global_status_threads_running{{{db}}})", "실행 중"),
                      (f"max(mysql_global_variables_max_connections{{{db}}})", "max_connections")], EA,
         desc("연결·실행 스레드", "mysql_global_status_threads_*", "개", "30초", "-", "02 · Hikari"))
    b.ts("잠금 대기·데드락", [(f"sum(rate(mysql_global_status_innodb_row_lock_waits{{{db}}}[$__rate_interval]))", "행 잠금 대기/초"),
                         (f"sum(rate(mysql_global_status_innodb_deadlocks{{{db}}}[$__rate_interval]))", "데드락/초")],
         PER_SEC, desc("InnoDB 행 잠금 대기와 데드락 발생률", "innodb_row_lock_waits, innodb_deadlocks(MariaDB)",
                       "건/초", "$__rate_interval", "-", "04 · 저장 시도 결과"))
    b.row("Redis")
    b.stat("Redis 상태", f"max(redis_up{{{rd}}})", "none",
           desc("exporter의 Redis 접속 성공", "redis_exporter redis_up", "정상/실패", "30초", "미활성 → 미수집",
                "Redis응답없음 runbook"), mappings=UP_MAP, steps=[RED, (1, GREEN)])
    b.stat("메모리 사용", f"max(redis_memory_used_bytes{{{rd}}})", "bytes",
           desc("Redis used_memory", "redis_memory_used_bytes", "바이트", "현재값", "-", "메모리 추세"))
    b.stat("명령/초", f"sum(rate(redis_commands_processed_total{{{rd}}}[5m]))", PER_SEC,
           desc("처리 명령 수/초", "redis_commands_processed_total", "건/초", "5분", "-", "명령 추세"), decimals=1)
    b.stat("연결 수", f"max(redis_connected_clients{{{rd}}})", EA,
           desc("연결된 클라이언트 수", "redis_connected_clients", "개", "현재값", "-", "Blocked 클라이언트"))
    b.stat("키 축출(1h)", f"sum(increase(redis_evicted_keys_total{{{rd}}}[1h]))", COUNT,
           desc("메모리 정책에 의한 키 축출", "redis_evicted_keys_total", "건", "1시간", "-",
                "maxmemory·Streams MAXLEN 정책"), steps=[GREEN, (1, RED)], decimals=0)
    b.stat("RDB 저장", f"max(redis_rdb_last_bgsave_status{{{rd}}})", "none",
           desc("마지막 BGSAVE 성공 여부(영속화 사용 시)", "redis_rdb_last_bgsave_status", "성공/실패", "현재값",
                "RDB를 쓰지 않으면 의미 없음", "Redis 설정"), mappings=OK_MAP, steps=[RED, (1, GREEN)])
    b.ts("Redis 메모리", [(f"max(redis_memory_used_bytes{{{rd}}})", "Redis 사용"),
                        (f"max(redis_memory_max_bytes{{{rd}}} > 0)", "Redis maxmemory")], "bytes",
         desc("사용 메모리와 maxmemory(설정 시)", "redis_memory_*_bytes", "바이트", "30초",
              "maxmemory=0(무제한)이면 상한 선 없음", "Eviction"))
    b.ts("Redis 클라이언트·명령", [(f"max(redis_connected_clients{{{rd}}})", "Redis 연결"),
                               (f"max(redis_blocked_clients{{{rd}}})", "Redis blocked(XREADGROUP BLOCK 포함)"),
                               (f"sum(rate(redis_commands_processed_total{{{rd}}}[$__rate_interval]))", "Redis 명령/초")],
         "short", desc("blocked는 Streams 블로킹 읽기로 정상적으로 존재할 수 있음", "redis_exporter", "개 / 건/초",
                       "30초", "-", "04 · Streams"),
         overrides=[fixed_color("Redis 연결", PURPLE), fixed_color("Redis blocked(XREADGROUP BLOCK 포함)", ORANGE),
                    fixed_color("Redis 명령/초", TEAL)])
    return b


# ---------------------------------------------------------------- 06 서버
def hosts():
    b = Board("hosts", "06 · 서버·컨테이너", "AWS 앱·DB·Redis 호스트와 학교 VM의 CPU·메모리·디스크·네트워크, 앱 프로세스.",
              extra_vars=[query_var("host", "호스트", f"label_values(node_uname_info, instance)")])
    H = 'instance=~"$host"'
    b.stat("CPU 사용률", f'1 - avg by (instance) (rate(node_cpu_seconds_total{{{H},mode="idle"}}[5m]))',
           "percentunit", desc("호스트별 idle 제외 CPU", "node_exporter", "%", "5분", "exporter 미설치 호스트는 표시 없음",
                               "CPU 추세"), w=8, h=5, steps=[GREEN, (0.7, ORANGE), (0.9, RED)], legend="{{instance}}",
           decimals=1)
    b.stat("메모리 사용률", f"1 - max by (instance) (node_memory_MemAvailable_bytes{{{H}}}) / max by (instance) (node_memory_MemTotal_bytes{{{H}}})",
           "percentunit", desc("MemAvailable 기준 사용률", "node_exporter meminfo", "%", "현재값", "-", "메모리 추세"),
           w=8, h=5, steps=[GREEN, (0.8, ORANGE), (0.9, RED)], legend="{{instance}}", decimals=1)
    b.stat("루트 디스크 여유", f'min by (instance) (node_filesystem_avail_bytes{{{H},mountpoint="/"}} / node_filesystem_size_bytes{{{H},mountpoint="/"}})',
           "percentunit", desc("/ 파일시스템 사용 가능 비율", "node_filesystem_*", "%", "현재값", "-",
                               "저장공간부족 runbook"), w=8, h=5, steps=[RED, (0.2, ORANGE), (0.3, GREEN)],
           legend="{{instance}}", decimals=1)
    b.ts("CPU", [(f'1 - avg by (instance) (rate(node_cpu_seconds_total{{{H},mode="idle"}}[$__rate_interval]))', "{{instance}}")],
         "percentunit", desc("호스트별 CPU 사용률", "node_cpu_seconds_total", "%", "$__rate_interval", "-", "부하"),
         steps=[GREEN, (0.9, RED)])
    b.ts("부하(load1) / 코어", [(f'max by (instance) (node_load1{{{H}}}) / count by (instance) (node_cpu_seconds_total{{{H},mode="idle"}})',
                             "{{instance}}")], "short",
         desc("1분 평균 부하를 코어 수로 나눈 값. 1 이상이면 대기 발생", "node_load1", "배", "scrape 15초", "-", "CPU"))
    b.ts("메모리·스왑", [(f"max by (instance) (node_memory_MemTotal_bytes{{{H}}} - node_memory_MemAvailable_bytes{{{H}}})", "사용 {{instance}}"),
                      (f"max by (instance) (node_memory_SwapTotal_bytes{{{H}}} - node_memory_SwapFree_bytes{{{H}}})", "스왑 {{instance}}")],
         "bytes", desc("사용 메모리·스왑", "node_memory_*", "바이트", "scrape 15초", "-", "앱 프로세스"))
    FS = H + ',fstype=~"ext[234]|xfs|btrfs|zfs",mountpoint!~"/etc/(hostname|hosts|resolv.conf)"'
    b.ts("디스크 여유", [(f'min by (instance, mountpoint) (node_filesystem_avail_bytes{{{FS}}} / node_filesystem_size_bytes{{{FS}}})',
                       "{{instance}} {{mountpoint}}")], "percentunit",
         desc("마운트별 사용 가능 비율. 영구 디스크 fstype(ext2/3/4·xfs·btrfs·zfs)만 표시하고 그 외 fstype(tmpfs·overlay·erofs·fuse·virtiofs 등)과 컨테이너 /etc bind mount는 생략", "node_filesystem_*", "%", "scrape 15초", "-",
              "08 · 저장 상태"), steps=[RED, (0.2, ORANGE), (0.3, GREEN)])
    b.ts("디스크 I/O", [(f"sum by (instance) (rate(node_disk_read_bytes_total{{{H}}}[$__rate_interval]))", "읽기 {{instance}}"),
                     (f"sum by (instance) (rate(node_disk_written_bytes_total{{{H}}}[$__rate_interval]))", "쓰기 {{instance}}")],
         "Bps", desc("디스크 읽기/쓰기 처리량", "node_disk_*_bytes_total", "바이트/초", "$__rate_interval", "-", "05 · DB"))
    b.ts("네트워크", [(f"sum by (instance) (rate(node_network_receive_bytes_total{{{H}}}[$__rate_interval]))", "수신 {{instance}}"),
                   (f"sum by (instance) (rate(node_network_transmit_bytes_total{{{H}}}[$__rate_interval]))", "송신 {{instance}}")],
         "Bps", desc("물리 NIC 송수신(lo·veth·docker 제외). 학교 VM은 브리지 네트워크 제약으로 미수집",
                     "node_network_*_bytes_total", "바이트/초", "$__rate_interval", "학교 VM은 설계상 미수집",
                     "08 · 수집 경로"))
    b.row("앱 프로세스")
    b.ts("앱 CPU", [(f"max(process_cpu_usage{{{API}}})", "API (코어 대비 비율)"),
                  (f"sum(rate(process_cpu_seconds_total{{{CHAT}}}[$__rate_interval]))", "채팅 (사용 코어)")], "short",
         desc("API는 0~1 비율, 채팅은 사용 코어 수. 컨테이너 단위 지표는 미수집이라 프로세스 지표로 대체", "process_cpu_usage, process_cpu_seconds_total", "비율 / 코어",
              "$__rate_interval", "-", "02 · JVM, 03 · 채팅"))
    b.table("앱 가동 시간", f'time() - max by (service) (process_start_time_seconds{{service=~"gwangsan-api|gwangsan-chat",{E}}})',
            desc("서비스별 마지막 시작 이후 경과(재시작 감지). 컨테이너 단위 지표 대신 프로세스 지표 사용", "process_start_time_seconds", "초", "현재값",
                 "미수집 서비스는 행 없음", "재시작 주석"), {"service": "서비스", "Value": "가동 시간"}, unit="s",
            widths={"가동 시간": 100})
    return b


# ---------------------------------------------------------------- 07 로그·분산 추적
def logs():
    b = Board("logs", "07 · 로그·분산 추적", "JSON 로그 검색, trace_id로 Tempo 이동, 느린·실패 트레이스 검색.",
              extra_vars=[custom_var("container", "컨테이너", ["gwangsan-container", "gwangsan-chat"],
                                     all_value="gwangsan-container|gwangsan-chat"),
                          custom_var("level", "로그 수준", ["ERROR", "WARN", "INFO", "DEBUG"]),
                          text_var("q", "검색어"), text_var("traceId", "Trace ID")])
    b.text("사용 안내", (
        "- Tempo 링크·표 ID로 추적합니다.\n"
        "- 운영 샘플링은 10%입니다.\n"
        "- 전체 요청·오류 수와 다릅니다.\n"
        "- ID는 검색어로 찾습니다.\n"
        "- 검색 기간을 좁혀주세요.\n"
        "- 개인정보 보호를 위한 설정.\n"
        "- Java Redis 자동 span 제외.\n"
        "- Streams는 04에서 확인합니다.\n"
        "- 채팅 Redis는 Node span."), h=8,
        description=(
        "- 로그 상세에서 `Tempo에서 트레이스 보기`를 누르면 같은 trace로 이동합니다. 구형 평문 로그에는 trace_id가 없습니다. 아래 trace 표의 ID를 누르면 span 구조와 관련 로그 링크가 열립니다.\n"
        "- 트레이스는 **샘플링**됩니다(운영 초기 10%). 트레이스 건수를 전체 요청·오류 수로 해석하지 마세요. 오류 trace도 모두 보존되지 않습니다.\n"
        "- trace_id·회원·방·메시지 ID는 Loki 라벨이 아니므로 검색어(줄 필터)로 찾습니다. 넓은 시간 범위의 전체 검색은 느립니다.\n"
        "- API의 일반 Redis 호출(Java Lettuce) span은 개인정보 보호를 위해 **의도적으로 수집하지 않습니다**(JWT가 Redis 키로 쓰임). "
        "Redis Streams 처리는 커스텀 span·04 화면, 채팅의 Redis 명령은 Node span으로 확인합니다."))
    sel = '{container=~"$container", level=~"$level"}'
    b.add({"type": "timeseries", "title": "로그 건수 (컨테이너·수준)", "datasource": LOKI,
           "description": desc("분당 로그 줄 수", "Loki count_over_time", "건", "$__interval",
                               "로그가 없으면 선 없음. 장기 무로그를 장애로 단정하지 않음", "아래 로그"),
           "targets": [{"datasource": LOKI, "refId": "A", "queryType": "range",
                        "expr": f'sum by (container, level) (count_over_time({sel} |= "$q" [$__interval]))',
                        "legendFormat": "{{container}} {{level}}"}],
           "fieldConfig": {"defaults": {"unit": COUNT, "noValue": "로그 없음",
                                        "custom": {"drawStyle": "bars", "fillOpacity": 60, "stacking": {"mode": "normal"}}},
                           "overrides": [level_color("ERROR", RED), level_color("WARN", ORANGE),
                                         level_color("INFO", BLUE), level_color("DEBUG", GREY)]},
           "options": {"legend": {"displayMode": "list", "placement": "bottom", "showLegend": True},
                       "tooltip": {"mode": "multi"}}}, 24, 6)
    b.logs("로그 검색", f'{sel} |= "$q" |= "$traceId"',
           desc("선택 컨테이너·수준·검색어의 로그", "기존 Promtail → Loki (container/stream/level 라벨)", "-",
                "선택 기간(최대 200줄)", "결과 없음은 '로그 없음'이지 '정상' 판정이 아님", "trace 링크"), h=12)
    for title, ql, d in (
            ("실패 트레이스", '{resource.service.name=~"gwangsan-api|gwangsan-chat" && status=error}',
             "status=error span을 포함한 trace"),
            ("느린 트레이스 (1초 이상)", '{resource.service.name=~"gwangsan-api|gwangsan-chat" && duration > 1s}',
             "1초 이상 span을 포함한 trace")):
        b.add({"type": "table", "title": title, "datasource": TEMPO,
               "description": desc(d + ". 샘플링된 trace만 존재", "Tempo TraceQL 검색", "-", "선택 기간(최대 20건)",
                                   "결과 없음은 '샘플 없음'일 수 있음", "trace 클릭 → 관련 로그"),
               "targets": [{"datasource": TEMPO, "refId": "A", "queryType": "traceql", "query": ql, "limit": 20,
                            "tableType": "traces"}],
               "fieldConfig": {"defaults": {"noValue": "해당 trace 없음"},
                               "overrides": [min_width({"id": "byRegexp", "options": "^(traceID|Trace ID)$"}, 280)] + [
                                   {"matcher": {"id": "byRegexp", "options": f"^({raw}|{shown})$"},
                                    "properties": [{"id": "displayName", "value": ko}]}
                                   for raw, shown, ko in (("startTime", "Start time", "시작 시각"),
                                                          ("traceService", "Service", "서비스"),
                                                          ("traceName", "Name", "작업"))]},
               "options": {"showHeader": True, "cellHeight": "sm"}}, 12, 9)
    return b


# ---------------------------------------------------------------- 08 수집·저장 상태
def telemetry():
    b = Board("telemetry", "08 · 수집·저장 상태", "관측 시스템 자체 상태: 수집 경로, 전송 실패, 저장 용량·보관 기간.")
    b.stat("지표 최신성", f"time() - max(timestamp(up{{{AWS}}}))", "s",
           desc("학교 Prometheus에 도착한 마지막 AWS 샘플 이후 경과", "remote_write up 타임스탬프", "초", "현재값",
                "한 번도 도착하지 않았으면 미수집", "AWS수집경로중단 runbook"),
           steps=[GREEN, (60, ORANGE), (180, RED)])
    b.stat("수집 정상률", f"count(up{{{AWS}}} == 1) / count(up{{{AWS}}})", "percentunit",
           desc("AWS scrape 대상 중 up=1 비율", "up", "%", "현재값", "-", "대상 표"),
           steps=[RED, (0.99, GREEN)], decimals=0)
    b.stat("메트릭 실패", 'sum(rate(prometheus_remote_storage_samples_failed_total{service="aws-alloy"}[5m]))', PER_SEC,
           desc("Alloy remote_write 영구 실패 샘플/초", "prometheus_remote_storage_samples_failed_total", "건/초", "5분",
                "단절 중에는 이 값도 도착하지 않음(최신성 패널로 판단)", "메트릭전송실패 runbook"),
           steps=[GREEN, (0.001, RED)], decimals=2)
    # 실패 카운터는 첫 실패 전까지 없다. 전송 카운터가 있으면(수집기 생존) 0으로 표시한다.
    b.stat("span 실패", 'sum(rate(otelcol_exporter_send_failed_spans_total{service="aws-alloy"}[5m])) '
                              'or sum(rate(otelcol_exporter_sent_spans_total{service="aws-alloy"}[5m])) * 0', PER_SEC,
           desc("Alloy→Tempo 전송 실패 span/초", "otelcol_exporter_send_failed_spans_total", "건/초", "5분", "-",
                "트레이스전송실패 runbook"), steps=[GREEN, (0.001, RED)], decimals=2)
    b.stat("Loki", 'max(up{job="loki",collector=""})', "none",
           desc("기존 Loki /metrics 응답", "학교 Prometheus scrape", "정상/실패", "30초", "-", "로그전송드롭"),
           mappings=UP_MAP, steps=[RED, (1, GREEN)])
    b.stat("Tempo", 'max(up{job="tempo",collector=""})', "none",
           desc("학교 Tempo 응답", "학교 Prometheus scrape", "정상/실패", "15초", "-", "학교관측구성요소중단"),
           mappings=UP_MAP, steps=[RED, (1, GREEN)])
    b.table("수집 대상", f'max by (job, service, environment, instance) (up{{{AWS}}}) or max by (job, service, environment, instance) (up{{collector=""}})',
            desc("모든 scrape 대상의 up. AWS 대상은 Alloy, 학교 대상은 학교 Prometheus가 수집", "up", "1=정상",
                 "현재값", "환경변수로 비활성화한 대상은 행 없음(미수집)", "대상별 runbook"),
            {"job": "job", "service": "서비스", "environment": "환경", "instance": "인스턴스", "Value": "상태"}, w=12, h=9,
            order=["Value", "environment", "service", "job", "instance"], value_map=TARGET_UP_MAP,
            fixed_widths={"상태": 90, "환경": 100, "서비스": 150}, sort="상태", sort_asc=True)
    b.ts("remote_write 처리", [
        ('sum(rate(prometheus_remote_storage_samples_total{service="aws-alloy"}[$__rate_interval]))', "전송"),
        ('sum(rate(prometheus_remote_storage_samples_retried_total{service="aws-alloy"}[$__rate_interval]))', "재시도"),
        ('sum(rate(prometheus_remote_storage_samples_failed_total{service="aws-alloy"}[$__rate_interval]))', "실패"),
        ('sum(rate(prometheus_http_requests_total{handler="/api/v1/write",code!~"2.."}[$__rate_interval]))', "학교 수신 거부")],
         PER_SEC, desc("Alloy 전송/재시도/실패와 학교 Prometheus 수신 오류", "Alloy·Prometheus 자체 지표", "건/초",
                       "$__rate_interval", "-", "터널 상태"), w=12, h=9)
    b.ts("트레이스 전송", [
        ('sum(rate(otelcol_receiver_accepted_spans_total{service="aws-alloy"}[$__rate_interval]))', "수신"),
        ('sum(rate(otelcol_exporter_sent_spans_total{service="aws-alloy"}[$__rate_interval]))', "전송"),
        ('sum(rate(otelcol_exporter_send_failed_spans_total{service="aws-alloy"}[$__rate_interval]))', "실패"),
        ('sum(rate(otelcol_receiver_refused_spans_total{service="aws-alloy"}[$__rate_interval]))', "수신 거부(메모리)")],
         PER_SEC, desc("앱→Alloy→Tempo span 흐름", "otelcol_* (Alloy)", "건/초", "$__rate_interval", "-", "Tempo"))
    b.ts("큐 적재", [('max(otelcol_exporter_queue_size{service="aws-alloy"})', "트레이스 큐"),
                   ('max(prometheus_remote_storage_highest_timestamp_in_seconds{service="aws-alloy"}) - max(prometheus_remote_storage_queue_highest_sent_timestamp_seconds{service="aws-alloy"})',
                    "remote_write 지연(초)")], "short",
         desc("트레이스 큐 길이(최대 200)와 remote_write 미전송 지연", "Alloy 자체 지표", "개 / 초", "scrape 15초",
              "-", "터널·학교 수신"))
    b.ts("Promtail 로그 전송", [
        ('sum(rate(promtail_sent_entries_total[$__rate_interval]))', "전송"),
        ('sum by (reason) (rate(promtail_dropped_entries_total[$__rate_interval]))', "드롭 {{reason}}"),
        ('sum by (status_code) (rate(promtail_request_duration_seconds_count{status_code!~"2.."}[$__rate_interval]))', "응답 {{status_code}}")],
         PER_SEC, desc("Promtail→Loki 전송·드롭·오류 응답", "promtail_* (Promtail을 관측 네트워크에 연결해야 수집)",
                       "건/초", "$__rate_interval", "연결 전에는 미수집", "로그전송드롭 runbook"), w=24)
    b.row("저장 용량·보관 기간", collapsed=True)
    b.stat("Prometheus TSDB", 'max(prometheus_tsdb_storage_blocks_bytes{job="prometheus"}) + max(prometheus_tsdb_wal_storage_size_bytes{job="prometheus"})',
           "bytes", desc("블록+WAL 크기. 보관 7일 또는 8GB 중 먼저 도달 시 삭제", "prometheus_tsdb_*", "바이트",
                         "현재값", "-", "디스크 여유"), w=6)
    b.stat("TSDB 상한 대비", 'max(prometheus_tsdb_storage_blocks_bytes{job="prometheus"}) / max(prometheus_tsdb_retention_limit_bytes{job="prometheus"} > 0)',
           "percentunit", desc("블록 크기 / retention.size(8GB)", "prometheus_tsdb_retention_limit_bytes", "%", "현재값",
                               "-", "보관 정책"), w=6, steps=[GREEN, (0.8, ORANGE), (0.95, RED)], decimals=1)
    b.stat("가장 오래된 메트릭", 'time() - min(prometheus_tsdb_lowest_timestamp_seconds{job="prometheus"})', "s",
           desc("실제 보관 중인 가장 오래된 샘플 경과(목표 7일)", "prometheus_tsdb_lowest_timestamp_seconds", "초",
                "현재값", "-", "용량 상한으로 7일보다 짧을 수 있음"), w=6)
    b.stat("학교 VM 디스크 여유", 'min(node_filesystem_avail_bytes{job="node",collector="",mountpoint="/"} / node_filesystem_size_bytes{job="node",collector="",mountpoint="/"})',
           "percentunit", desc("학교 VM / 여유 비율. 트레이스는 3일 보관, 용량 상한은 보장되지 않음", "node_exporter",
                               "%", "현재값", "-", "저장공간부족 runbook"), w=6,
           steps=[RED, (0.2, ORANGE), (0.3, GREEN)], decimals=1)
    b.ts("저장 증가", [('max(prometheus_tsdb_storage_blocks_bytes{job="prometheus"})', "Prometheus 블록"),
                    ('max(prometheus_tsdb_head_series{job="prometheus"})', "활성 시계열 수")], "short",
         desc("블록 크기·활성 시계열 추세(카디널리티 폭증 감지)", "prometheus_tsdb_*", "바이트 / 개", "scrape 15초",
              "-", "라벨 허용 목록"))
    b.ts("Tempo 수집", [('sum(rate(tempo_distributor_spans_received_total[$__rate_interval]))', "수신 span/초"),
                      ('sum(rate(tempo_discarded_spans_total[$__rate_interval]))', "폐기 span/초")], PER_SEC,
         desc("Tempo 수신·폐기 span", "tempo_* 자체 지표", "건/초", "$__rate_interval", "-", "트레이스 전송"))
    b.text("보관·복구 한계", (
        "- 메트릭: 7일 또는 8GB.\n"
        "- 트레이스: 3일.\n"
        "- 로그 보관: 기존 Loki 기준.\n"
        "- VM 소실 시 데이터 복구 불가.\n"
        "- HA·외부 백업이 아닙니다.\n"
        "- VM 전체 중단은 자가 알림 불가.\n"
        "- 복구 절차: backup.md"), h=8,
        description="복구 절차 전체: docs/observability/operations/backup.md")
    return b


def main():
    OUT.mkdir(exist_ok=True)
    names = {"overview": "00-overview", "api": "01-api", "jvm": "02-jvm", "chat": "03-chat",
             "streams": "04-streams", "data": "05-data", "hosts": "06-hosts", "logs": "07-logs",
             "telemetry": "08-telemetry"}
    for fn in (overview, api, jvm, chat, streams, data, hosts, logs, telemetry):
        board = fn()
        path = OUT / f"{names[board.key]}.json"
        path.write_text(json.dumps(board.build(), ensure_ascii=False, indent=2) + "\n")
        print(path.name, len([p for p in board.panels if p["type"] != "row"]), "panels")


if __name__ == "__main__":
    main()
