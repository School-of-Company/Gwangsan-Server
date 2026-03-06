#!/usr/bin/env bash
# ============================================================
# Gwangsan 성능 테스트 환경 셋업 스크립트
#
# Before (최적화 전, commit 6fd62c1): port 8080
# After  (최적화 후, develop HEAD):   port 8081
#
# 사용법:
#   chmod +x performance-test/setup.sh
#   cd /path/to/Gwangsan-Server
#   ./performance-test/setup.sh
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BEFORE_DIR="${PROJECT_ROOT}/../gwangsan-before"
BEFORE_COMMIT="6fd62c1"   # 프로젝션/인덱싱 적용 직전 커밋

BEFORE_PORT=8080
AFTER_PORT=8081

# --- DB 설정 (환경에 맞게 수정) ---
DB_HOST="${RDB_HOST:-localhost}"
DB_PORT="${RDB_PORT:-3306}"
DB_SCHEMA="${RDB_SCHEMA:-gwangsan_db}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-12345678}"

BEFORE_PID_FILE="${SCRIPT_DIR}/before.pid"
AFTER_PID_FILE="${SCRIPT_DIR}/after.pid"

LOG_DIR="${SCRIPT_DIR}/logs"
mkdir -p "$LOG_DIR"

# ============================================================
print_step() { echo; echo ">>> $1"; }
# ============================================================

print_step "[1/5] git worktree 설정 (Before: $BEFORE_COMMIT)"
cd "$PROJECT_ROOT"

if [ -d "$BEFORE_DIR" ]; then
  echo "  이미 존재: $BEFORE_DIR (건너뜀)"
else
  git worktree add "$BEFORE_DIR" "$BEFORE_COMMIT"
  echo "  worktree 생성 완료: $BEFORE_DIR"
fi

# ============================================================
print_step "[2/5] 빌드 - After (최적화 후)"
cd "$PROJECT_ROOT"
echo "  ./gradlew clean build -x test ..."
./gradlew clean build -x test -q
AFTER_JAR=$(ls build/libs/*.jar | grep -v plain | head -1)
echo "  After JAR: $AFTER_JAR"

# ============================================================
print_step "[3/5] 빌드 - Before (최적화 전)"
cd "$BEFORE_DIR"
echo "  ./gradlew clean build -x test ..."
./gradlew clean build -x test -q
BEFORE_JAR=$(ls build/libs/*.jar | grep -v plain | head -1)
echo "  Before JAR: $BEFORE_JAR"
cd "$PROJECT_ROOT"

# ============================================================
print_step "[4/5] 목업 데이터 삽입"
echo "  mock-data.sql 실행 중..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_SCHEMA" \
  < "${SCRIPT_DIR}/mock-data.sql" \
  && echo "  완료" \
  || echo "  [경고] mock-data.sql 실행 실패 - 수동으로 실행해주세요"

# ============================================================
print_step "[5/5] 서버 기동"

COMMON_OPTS=(
  "--spring.datasource.url=jdbc:mariadb://${DB_HOST}:${DB_PORT}/${DB_SCHEMA}"
  "--spring.datasource.username=${DB_USER}"
  "--spring.datasource.password=${DB_PASSWORD}"
  "--spring.jpa.show-sql=false"
  "--logging.level.root=WARN"
  "--logging.level.team.startup=WARN"
)

# Before 서버
echo "  Starting Before server on port $BEFORE_PORT ..."
nohup java -jar "$BEFORE_DIR/$BEFORE_JAR" \
  --server.port="$BEFORE_PORT" \
  "${COMMON_OPTS[@]}" \
  > "$LOG_DIR/before.log" 2>&1 &
echo $! > "$BEFORE_PID_FILE"
echo "  Before PID: $(cat $BEFORE_PID_FILE)"

# After 서버
echo "  Starting After server on port $AFTER_PORT ..."
nohup java -jar "$PROJECT_ROOT/$AFTER_JAR" \
  --server.port="$AFTER_PORT" \
  "${COMMON_OPTS[@]}" \
  > "$LOG_DIR/after.log" 2>&1 &
echo $! > "$AFTER_PID_FILE"
echo "  After PID: $(cat $AFTER_PID_FILE)"

# ============================================================
echo
echo "======================================================"
echo " 서버 기동 확인 중 (최대 60초 대기)..."
echo "======================================================"

wait_for_server() {
  local port=$1
  local label=$2
  local max=60
  local count=0
  while ! curl -sf "http://localhost:${port}/api/health" > /dev/null 2>&1; do
    sleep 2
    count=$((count + 2))
    if [ $count -ge $max ]; then
      echo "  [오류] $label (port $port) 기동 실패 - 로그 확인: $LOG_DIR/${label}.log"
      return 1
    fi
  done
  echo "  [OK] $label (port $port) 기동 완료"
}

wait_for_server $BEFORE_PORT "before"
wait_for_server $AFTER_PORT "after"

echo
echo "======================================================"
echo " 준비 완료!"
echo "======================================================"
echo " Before (최적화 전): http://localhost:$BEFORE_PORT"
echo " After  (최적화 후): http://localhost:$AFTER_PORT"
echo
echo " JMeter 테스트 실행:"
echo "   jmeter -n -t performance-test/jmeter/gwangsan-perf-test.jmx \\"
echo "          -l performance-test/results/result.jtl \\"
echo "          -e -o performance-test/results/report"
echo
echo " 서버 종료:"
echo "   ./performance-test/stop.sh"
echo "======================================================"
