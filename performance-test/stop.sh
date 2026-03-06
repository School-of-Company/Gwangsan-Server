#!/usr/bin/env bash
# 성능 테스트 서버 종료

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BEFORE_DIR="${PROJECT_ROOT}/../gwangsan-before"

BEFORE_PID_FILE="${SCRIPT_DIR}/before.pid"
AFTER_PID_FILE="${SCRIPT_DIR}/after.pid"

stop_server() {
  local pid_file=$1
  local label=$2
  if [ -f "$pid_file" ]; then
    local pid
    pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      kill "$pid"
      echo "  [OK] $label (PID $pid) 종료"
    else
      echo "  [--] $label 이미 종료됨"
    fi
    rm -f "$pid_file"
  else
    echo "  [--] $label PID 파일 없음"
  fi
}

echo ">>> 서버 종료"
stop_server "$BEFORE_PID_FILE" "Before"
stop_server "$AFTER_PID_FILE"  "After"

echo
read -p ">>> git worktree ($BEFORE_DIR) 도 삭제할까요? [y/N] " answer
if [[ "$answer" =~ ^[Yy]$ ]]; then
  cd "$PROJECT_ROOT"
  git worktree remove "$BEFORE_DIR" --force 2>/dev/null && echo "  worktree 삭제 완료" || echo "  worktree 삭제 실패 (수동으로: git worktree remove $BEFORE_DIR)"
fi

echo "완료."
