#!/usr/bin/env node
// Socket.IO 부하 도구: 같은 스택 구성에서 baseline/instrumented 를 같은 워크로드로 잰다.
// 측정값: echo 지연(emit→본인 receiveMessage), DB 커밋 관측 지연(폴링 해상도 명시), 미커밋 수,
// drain 시간, 오류 수, 컨테이너 CPU/메모리. SLO 판정은 하지 않는다(숫자만 기록).
//
// node chat-load.mjs --label baseline --clients 10 --rate 1 --duration 60 --warmup 10 --repeats 3 --out <dir>
import { createRequire } from 'node:module';
import { execFile, execFileSync } from 'node:child_process';
import { promisify } from 'node:util';
import { writeFileSync, mkdirSync } from 'node:fs';
import { join } from 'node:path';

const exec = promisify(execFile);
const args = Object.fromEntries(
  process.argv.slice(2).reduce((acc, v, i, a) => (v.startsWith('--') ? [...acc, [v.slice(2), a[i + 1]]] : acc), []),
);
const opt = {
  label: args.label ?? 'run',
  clients: Number(args.clients ?? 10),
  rate: Number(args.rate ?? 1), // 클라이언트당 초당 메시지
  duration: Number(args.duration ?? 60),
  warmup: Number(args.warmup ?? 10),
  repeats: Number(args.repeats ?? 1),
  drainTimeout: Number(args['drain-timeout'] ?? 120),
  url: args.url ?? 'http://127.0.0.1:23000',
  project: args.project ?? 'gw-load',
  out: args.out ?? '.',
  chatDir: args['chat-dir'] ?? process.env.CHAT_DIR,
};
const require = createRequire(join(opt.chatDir, 'package.json'));
const { io } = require('socket.io-client');
const jwt = require('jsonwebtoken');
const SECRET = 'load-test-secret-at-least-32-bytes-long';

const dc = (svc, cmd, input) =>
  execFileSync('docker', ['compose', '-p', opt.project, 'exec', '-T', svc, ...cmd], { input, encoding: 'utf8', maxBuffer: 64 << 20 });
const sql = (q) => dc('mariadb', ['mariadb', '-uroot', '-ppassword', 'gwangsan_db', '-N', '-B', '-e', q]).trim();
const sqlAsync = async (q) =>
  (await exec('docker', ['compose', '-p', opt.project, 'exec', '-T', 'mariadb', 'mariadb', '-uroot', '-ppassword', 'gwangsan_db', '-N', '-B', '-e', q], { maxBuffer: 64 << 20 })).stdout.trim();

const pct = (xs, p) => {
  if (!xs.length) return null;
  const s = [...xs].sort((a, b) => a - b);
  return s[Math.min(s.length - 1, Math.ceil((p / 100) * s.length) - 1)];
};
const stats = (xs) => ({ n: xs.length, p50: pct(xs, 50), p95: pct(xs, 95), p99: pct(xs, 99), max: xs.length ? Math.max(...xs) : null });
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

function seed(n) {
  const seller = 1000; // 고정 판매자: 클라이언트 수를 바꿔도 INSERT IGNORE 로 누적 시드
  const members = [...Array.from({ length: n }, (_, i) => i + 1), seller].map((id) => `(${id},'L${id}','L${id}','unused','load-${id}','ROLE_USER','ACTIVE',NOW())`).join(',');
  const rooms = Array.from({ length: n }, (_, i) => `(${i + 1},NOW(),TRUE,${i + 1},${seller},1)`).join(',');
  sql(`INSERT IGNORE INTO tbl_member (member_id,name,nickname,password,phone_number,role,member_status,joined_at) VALUES ${members};
       INSERT IGNORE INTO tbl_product (product_id,title,description,gwangsan,member_id,type,mode,status,created_at,updated_at) VALUES (1,'F','F',0,${seller},'OBJECT','GIVER','ONGOING',NOW(),NOW());
       INSERT IGNORE INTO tbl_chat_room (room_id,created_at,is_active,buyer_id,seller_id,product_id) VALUES ${rooms};`);
  dc('redis', ['redis-cli'], Array.from({ length: n }, (_, i) => `SET auth:cache:load-${i + 1} '{"memberId":${i + 1},"nickname":"L${i + 1}"}' EX 7200`).join('\n') + '\n');
}

function connect(i) {
  const token = jwt.sign({ sub: `load-${i + 1}` }, SECRET, { expiresIn: '2h' });
  return new Promise((resolve, reject) => {
    const s = io(`${opt.url}/api/chat`, { auth: { token: `Bearer ${token}` }, transports: ['websocket'], reconnection: false });
    const t = setTimeout(() => reject(new Error('connect timeout')), 15000);
    s.on('connect', () => (clearTimeout(t), resolve(s)));
    s.on('connect_error', (e) => (clearTimeout(t), reject(e)));
  });
}

async function sampleDocker(stopAt, out) {
  while (Date.now() < stopAt) {
    try {
      const { stdout } = await exec('docker', ['stats', '--no-stream', '--format', '{{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}', `${opt.project}-api-1`, `${opt.project}-chat-1`, `${opt.project}-mariadb-1`, `${opt.project}-redis-1`]);
      for (const line of stdout.trim().split('\n')) {
        const [name, cpu, mem] = line.split('\t');
        const m = /([\d.]+)(\w+)/.exec(mem);
        const mib = m ? Number(m[1]) * ({ KiB: 1 / 1024, MiB: 1, GiB: 1024 }[m[2]] ?? 1) : NaN;
        (out[name] ??= []).push({ cpu: parseFloat(cpu), mib });
      }
    } catch {}
    await sleep(1000);
  }
}

async function runPhase(sockets, seconds, tag, record) {
  const sent = new Map(); // content -> {t0, roomId}
  const done = [];
  let errors = 0;
  const handlers = sockets.map((s, i) => {
    const onMsg = (v) => {
      const k = v?.content;
      if (!v?.isMine || !sent.has(k)) return;
      const e = sent.get(k);
      sent.delete(k);
      done.push({ messageId: String(v.messageId), t0: e.t0, echoMs: Date.now() - e.t0 });
    };
    const onErr = () => errors++;
    s.on('receiveMessage', onMsg);
    s.on('error', onErr);
    return () => (s.off('receiveMessage', onMsg), s.off('error', onErr));
  });
  const interval = 1000 / opt.rate;
  const end = Date.now() + seconds * 1000;
  let n = 0;
  let lastSend = 0;
  await Promise.all(
    sockets.map(async (s, i) => {
      await sleep(Math.random() * interval); // 클라이언트 위상 분산
      let next = Date.now();
      while (Date.now() < end) {
        const content = `${tag}-c${i}-${n++}`;
        sent.set(content, { t0: Date.now() });
        s.emit('sendMessage', { roomId: i + 1, content, imageIds: [], messageType: 'TEXT' });
        lastSend = Date.now();
        next += interval;
        await sleep(Math.max(0, next - Date.now()));
      }
    }),
  );
  await sleep(5000); // echo 수신 유예
  handlers.forEach((off) => off());
  return { done, unanswered: sent.size, errors, emitted: done.length + sent.size, lastSend };
}

async function repeat(r) {
  const sockets = await Promise.all(Array.from({ length: opt.clients }, (_, i) => connect(i)));
  try {
    if (opt.warmup > 0) await runPhase(sockets, opt.warmup, `${opt.label}-w${r}`);
    const floor = sql('SELECT COALESCE(MAX(message_id),0) FROM tbl_chat_message');
    const firstSeen = new Map();
    let polling = true;
    let polls = 0;
    const poller = (async () => {
      while (polling) {
        const t = Date.now();
        const ids = (await sqlAsync(`SELECT message_id FROM tbl_chat_message WHERE message_id>${floor}`).catch(() => '')).split('\n');
        polls++;
        for (const id of ids) if (id && !firstSeen.has(id)) firstSeen.set(id, t);
        await sleep(100);
      }
    })();
    const docker = {};
    const tStart = Date.now();
    const sampler = sampleDocker(tStart + opt.duration * 1000, docker);
    const phase = await runPhase(sockets, opt.duration, `${opt.label}-m${r}`);
    const deadline = Date.now() + opt.drainTimeout * 1000;
    while (Date.now() < deadline && phase.done.some((d) => !firstSeen.has(d.messageId))) await sleep(200);
    // 마지막 emit 부터 마지막 커밋 관측까지(전부 커밋됐을 때만). 폴링 해상도만큼 늦게 잡힌다.
    const seen = phase.done.map((d) => firstSeen.get(d.messageId));
    const drainMs = seen.every((t) => t !== undefined) && seen.length ? Math.max(...seen) - phase.lastSend : null;
    polling = false;
    await poller;
    await sampler;
    const commit = phase.done.filter((d) => firstSeen.has(d.messageId)).map((d) => firstSeen.get(d.messageId) - d.t0);
    const summarize = (xs, k) => (xs?.length ? { avg: xs.reduce((a, b) => a + b[k], 0) / xs.length, max: Math.max(...xs.map((x) => x[k])) } : null);
    return {
      repeat: r,
      emitted: phase.emitted,
      echoed: phase.done.length,
      unansweredEchoes: phase.unanswered,
      socketErrors: phase.errors,
      committed: commit.length,
      uncommitted: phase.done.length - commit.length,
      throughputEchoPerSec: phase.done.length / opt.duration,
      echoMs: stats(phase.done.map((d) => d.echoMs)),
      commitObservedMs: stats(commit),
      commitPollResolutionMs: polls ? Math.round((Date.now() - tStart) / polls) : null,
      drainMsAfterLastSend: drainMs,
      containers: Object.fromEntries(Object.entries(docker).map(([k, v]) => [k, { cpuPct: summarize(v, 'cpu'), memMiB: summarize(v, 'mib') }])),
    };
  } finally {
    sockets.forEach((s) => s.disconnect());
  }
}

seed(opt.clients);
const result = { ...opt, chatDir: undefined, startedAt: new Date().toISOString(), host: { node: process.version }, repeats: [] };
for (let r = 1; r <= opt.repeats; r++) {
  const res = await repeat(r);
  result.repeats.push(res);
  console.log(JSON.stringify({ label: opt.label, clients: opt.clients, repeat: r, echoP95: res.echoMs.p95, commitP95: res.commitObservedMs.p95, uncommitted: res.uncommitted, errors: res.socketErrors + res.unansweredEchoes }));
  await sleep(3000);
}
mkdirSync(opt.out, { recursive: true });
const file = join(opt.out, `load-${opt.label}-c${opt.clients}-r${opt.rate}-d${opt.duration}x${opt.repeats}.json`);
writeFileSync(file, JSON.stringify(result, null, 2));
console.log(`wrote ${file}`);
