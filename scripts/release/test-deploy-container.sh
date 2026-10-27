#!/usr/bin/env bash
# shellcheck disable=SC2317,SC2329  # check()/trap 으로 간접 호출되는 함수들
# 로컬 Docker 전용 회귀 테스트: 운영 이미지/레지스트리/AWS 를 쓰지 않고 공개 nginx digest 로
# deploy-container.sh 의 digest·계정/저장소 검증, 네트워크 선검사·빈 값 생략, 관측 네트워크 연결,
# env 우선순위, 인자 없는 CodeDeploy 훅 실행, 실패 주입(stop/rename/create/network connect/start/헬스)
# 시 자동 복구, 외부 이름 보유자 보존, 첫 배포 실패 정리, 수동 rollback 과 rollback 실패·헬스 실패 복구를 검증한다.
# docker/aws 는 PATH 앞의 shim 으로 감싸 실패를 주입한다(실제 docker 로 위임).
#   사용: scripts/release/test-deploy-container.sh [path/to/deploy-container.sh]
set -euo pipefail

script=${1:-"$(cd "$(dirname "$0")" && pwd)/deploy-container.sh"}
v1=nginx@sha256:a8b39bd9cf0f83869a2162827a0caf6137ddf759d50a171451b335cecc87d236  # 1.28-alpine
v2=nginx@sha256:5616878291a2eed594aee8db4dade5878cf7edcb475e59193904b198d9b830de  # 1.29-alpine
# 서비스별 기대 env (API 기본값)
otel_on=${EXPECT_OTEL_ON:-OTEL_JAVAAGENT_ENABLED=true}
otel_off=${EXPECT_OTEL_OFF:-OTEL_JAVAAGENT_ENABLED=false}
read -r -a prod_env <<< "${EXPECT_PROD_ENV:-TZ=Asia/Seoul OTEL_INSTRUMENTATION_LETTUCE_ENABLED=false OTEL_TRACES_SAMPLER_ARG=0.1 OTEL_RESOURCE_ATTRIBUTES=deployment.environment.name=production}"
name=release-test-$$
net=release-test-net-$$
port=18089
work=$(mktemp -d)
real_docker=$(command -v docker)
fails=0

cleanup() {
  "$real_docker" rm -f "$name" "$name-previous" "$name-rolledback" >/dev/null 2>&1 || true
  "$real_docker" network rm "$net" >/dev/null 2>&1 || true
  rm -rf "$work"
}
trap cleanup EXIT

mkdir -p "$work/bin"
# FAIL_DOCKER 로 시작하는 docker 명령의 FAIL_NTH(기본 1)번째 호출만 실패시킨다.
# FAIL_MODE=after 면 실제 명령을 실행한 뒤 실패를 보고한다(부분 성공 시나리오).
cat > "$work/bin/docker" <<EOF
#!/usr/bin/env bash
if [[ -n "\${FAIL_DOCKER:-}" && "\$*" == "\$FAIL_DOCKER"* ]]; then
  n=\$(( \$(cat "$work/count" 2>/dev/null || echo 0) + 1 )); echo "\$n" > "$work/count"
  if (( n == \${FAIL_NTH:-1} )); then
    [[ "\${FAIL_MODE:-before}" == after ]] && "$real_docker" "\$@" >/dev/null 2>&1
    # foreign: 실패 직전에 다른 주체가 같은 이름으로 컨테이너를 만든 상황(표식 없음)
    if [[ "\${FAIL_MODE:-}" == foreign ]]; then
      args=("\$@"); for i in "\${!args[@]}"; do [[ "\${args[\$i]}" == --name ]] && fname=\${args[\$((i+1))]}; done
      "$real_docker" create --name "\$fname" --label test.foreign=1 "$v1" >/dev/null
    fi
    echo "injected failure: docker \$*" >&2; exit 1
  fi
fi
case "\$1" in
  login) cat >/dev/null; exit 0 ;;
  pull) [[ "\$2" == *.dkr.ecr.* ]] && { echo "fake-pull-reached \$2" >&2; exit 1; } ;;
esac
exec "$real_docker" "\$@"
EOF
cat > "$work/bin/aws" <<'EOF'
#!/usr/bin/env bash
case "$1 $2" in
  "sts get-caller-identity") echo 111122223333 ;;
  "ecr get-login-password") echo fake ;;
  *) exit 1 ;;
esac
EOF
chmod +x "$work/bin/docker" "$work/bin/aws"
export PATH="$work/bin:$PATH"

printf 'FROM_ENV_FILE=yes\n%s\n' "$otel_on" > "$work/app.env"
docker network create "$net" >/dev/null

run() {
  rm -f "$work/count"
  env -i PATH="$PATH" HOME="$HOME" \
    CONTAINER_NAME="$name" HOST_PORT="$port" CONTAINER_PORT=80 ENV_FILE="$work/app.env" \
    HEALTH_PATH=/ HEALTH_TIMEOUT_SECONDS=20 OBS_NETWORK="$net" OBS_ALIAS=gwangsan-test \
    REGISTRY_LOGIN=false "$@" bash "$script" "${DEPLOY_CMD:-deploy}" >"$work/out" 2>&1
}
check() {  # check <설명> <명령...>
  local desc=$1; shift
  if "$@"; then echo "ok   - $desc"; else echo "FAIL - $desc"; fails=$((fails + 1)); sed 's/^/     | /' "$work/out"; fi
}
state_of() { docker container inspect -f '{{.Config.Image}} {{.State.Running}}' "$1" 2>/dev/null; }
is() { [[ "$(state_of "$1")" == "$2" ]]; }  # is <컨테이너> "<image> <running>"
absent() { ! docker container inspect "$1" >/dev/null 2>&1; }
running() { [[ "$(docker container inspect -f '{{.State.Running}}' "$1" 2>/dev/null)" == true ]]; }
on_net() { docker network inspect "$net" -f '{{range .Containers}}{{.Name}} {{end}}' | grep -qw -- "$1"; }
env_of() { docker container inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$name"; }
has_env() { local v; for v in "$@"; do env_of | grep -qx "$v" || return 1; done; }
serves() { local _; for _ in 1 2 3 4 5; do curl -fsS -o /dev/null --max-time 5 "http://127.0.0.1:$port/" && return 0; sleep 1; done; return 1; }  # 기동 직후 여유
up_as() { is "$name" "$1 true" && serves; }  # 원래 이름으로 실행 중이고 실제 응답
only_known() {  # 테스트 이름의 컨테이너가 name/previous 외에 남지 않음
  [[ -z "$(docker ps -a --format '{{.Names}}' | grep -- "^$name" | grep -vx -e "$name" -e "$name-previous" || true)" ]]
}
out_has() { grep -q -- "$1" "$work/out"; }
not() { ! "$@"; }

# --- 입력 검증 ---
check "태그 참조는 거부" not run IMAGE_REF=nginx:1.28-alpine
check "거부 시 컨테이너 미생성" absent "$name"
check "관측 네트워크 없으면 건드리기 전에 실패" not run IMAGE_REF="$v1" OBS_NETWORK=missing-net-$$
check "다른 AWS 계정의 ECR 참조 거부" not run REGISTRY_LOGIN=true IMAGE_REPOSITORY=testrepo \
  IMAGE_REF="999988887777.dkr.ecr.ap-northeast-2.amazonaws.com/testrepo@${v1#*@}"
check "  └ 계정 불일치 메시지" out_has "account 111122223333"
check "같은 계정 다른 저장소 거부" not run REGISTRY_LOGIN=true IMAGE_REPOSITORY=testrepo \
  IMAGE_REF="111122223333.dkr.ecr.ap-northeast-2.amazonaws.com/other@${v1#*@}"
check "  └ 저장소 불일치 메시지" out_has "repository testrepo"
check "인증 계정+설정 저장소는 검증 통과 후 pull 단계 도달" not run REGISTRY_LOGIN=true IMAGE_REPOSITORY=testrepo \
  IMAGE_REF="111122223333.dkr.ecr.ap-northeast-2.amazonaws.com/testrepo@${v1#*@}"
check "  └ pull 도달" out_has fake-pull-reached
check "검증 실패 시 컨테이너 미생성" absent "$name"

# --- 인자 없는 CodeDeploy 훅 + release.env 의 명시적 빈 OBS_NETWORK ---
mkdir -p "$work/bundle/scripts/release"
cp "$script" "$work/bundle/scripts/release/deploy-container.sh"
cat > "$work/bundle/scripts/release/release.env" <<EOF
IMAGE_REF=$v1
OTEL_ENABLED=true
OTEL_TRACES_SAMPLER_ARG=0.1
CONTAINER_NAME=$name
HOST_PORT=$port
CONTAINER_PORT=80
ENV_FILE=$work/app.env
HEALTH_PATH=/
HEALTH_TIMEOUT_SECONDS=20
REGISTRY_LOGIN=false
OBS_NETWORK=
EOF
check "인자 없는 훅 실행(번들 release.env)으로 배포" \
  bash -c "env -i PATH='$PATH' HOME='$HOME' bash '$work/bundle/scripts/release/deploy-container.sh' >'$work/out' 2>&1"
check "  └ v1 실행·응답" up_as "$v1"
check "  └ release.env 의 OBS_NETWORK= 는 관측 네트워크 생략" not on_net "$name"
docker rm -f "$name" >/dev/null
check "환경변수 OBS_NETWORK= 도 생략으로 존중" run IMAGE_REF="$v1" OBS_NETWORK=
check "  └ 관측 네트워크 미연결" not on_net "$name"
docker rm -f "$name" >/dev/null
if ! docker network inspect gwangsan-observability >/dev/null 2>&1; then
  check "OBS_NETWORK 미설정이면 기본 네트워크 요구(없으면 실패)" not bash -c \
    "env -i PATH='$PATH' HOME='$HOME' CONTAINER_NAME='$name' ENV_FILE='$work/app.env' IMAGE_REF='$v1' REGISTRY_LOGIN=false bash '$script' >'$work/out' 2>&1"
  check "  └ 기본 네트워크 이름 메시지" out_has "network gwangsan-observability missing"
fi

# --- 정상 배포 ---
check "v1 배포 성공" run IMAGE_REF="$v1"
check "v1 실행 중" running "$name"
check "관측 네트워크 별칭 연결" on_net "$name"
check "기본 bridge 유지" bash -c "docker container inspect $name -f '{{json .NetworkSettings.Networks}}' | grep -q '\"bridge\"'"
check "env 파일 유지" has_env FROM_ENV_FILE=yes
check "이번 실행 소유 표식(label) 부착" bash -c "docker container inspect -f '{{index .Config.Labels \"gwangsan.release.run\"}}' $name | grep -q ."
check "운영 샘플링/환경/TZ 주입" has_env "$otel_on" "${prod_env[@]}"
check "호스트 포트는 앱 포트 하나만" bash -c "[ \"\$(docker port $name | wc -l | tr -d ' ')\" -le 2 ] && docker port $name | grep -q '^80/tcp'"

check "v2 배포 성공 (OTel 비활성 롤백 플래그)" run IMAGE_REF="$v2" OTEL_ENABLED=false
check "v2 실행 중" is "$name" "$v2 true"
check "-e 가 env 파일보다 우선 ($otel_off)" has_env "$otel_off"
check "v1 은 정지된 previous 로 보관" is "$name-previous" "$v1 false"
check "스크립트에 prune 없음(무관한 이미지 보호)" not grep -Eq 'docker[^#]*prune' "$script"

# --- 기존 앱을 멈춘 뒤의 실패 → 자동 복구 (현재 v2 실행 중) ---
check "헬스 실패 배포는 실패로 종료" not run IMAGE_REF="$v1" HEALTH_PATH=/missing HEALTH_TIMEOUT_SECONDS=6
check "  └ v2 자동 복구·응답" up_as "$v2"
check "  └ 복구된 컨테이너 관측 네트워크 유지" on_net "$name"

for spec in "stop:before" "stop:after" "rename:before" "rename:after" "create:before" "create:after" \
            "network connect:before" "start:before" "start:after"; do
  cmd=${spec%%:*} mode=${spec##*:}
  check "docker $cmd 실패($mode) 시 배포 실패로 종료" not run IMAGE_REF="$v1" FAIL_DOCKER="$cmd" FAIL_MODE="$mode"
  check "  └ 기존 v2 가 원래 이름으로 실행·응답" up_as "$v2"
  check "  └ 실패한 새 컨테이너 잔존 없음" only_known
  check "  └ 관측 네트워크 유지" on_net "$name"
done

# --- create 오류 직전 다른 주체가 이름을 가져간 경우: 그 컨테이너는 건드리지 않는다 ---
check "create 실패+외부 이름 보유자 시 실패로 종료" not run IMAGE_REF="$v1" FAIL_DOCKER=create FAIL_MODE=foreign
check "  └ 외부 보유자 보존(삭제·시작 안 함)" bash -c "docker container inspect -f '{{index .Config.Labels \"test.foreign\"}} {{.State.Running}}' $name | grep -qx '1 false'"
prev_up() { is "$name-previous" "$v2 true" && serves; }
check "  └ 기존 v2 는 previous 이름으로라도 기동·응답" prev_up
check "  └ 수동 조치 안내" out_has "manual action required"
docker rm -f "$name" >/dev/null; docker stop "$name-previous" >/dev/null; docker rename "$name-previous" "$name"; docker start "$name" >/dev/null

# --- 첫 배포(기존 앱 없음) 실패: 새 컨테이너 정리 ---
docker rm -f "$name" "$name-previous" >/dev/null
check "첫 배포 start 실패 시 실패로 종료" not run IMAGE_REF="$v1" FAIL_DOCKER=start
check "  └ 실패한 컨테이너 정리됨" absent "$name"

# --- 수동 rollback ---
check "v2 배포" run IMAGE_REF="$v2"
check "v1 배포(previous=v2)" run IMAGE_REF="$v1"
DEPLOY_CMD=rollback check "rollback 명령 성공" run
check "  └ v2 실행·응답" up_as "$v2"
check "  └ 되돌려진 v1 은 previous 로 보관" is "$name-previous" "$v1 false"
for spec in "stop:1" "rename:1" "rename:2" "start:1" "health:0"; do
  cmd=${spec%%:*} nth=${spec##*:}
  if [[ $cmd == health ]]; then
    DEPLOY_CMD=rollback check "rollback 대상 헬스 실패 시 실패로 종료" not run HEALTH_PATH=/missing HEALTH_TIMEOUT_SECONDS=6
  else
    DEPLOY_CMD=rollback check "rollback 중 $cmd(#$nth) 실패 시 실패로 종료" not run FAIL_DOCKER="$cmd" FAIL_NTH="$nth"
  fi
  check "  └ 현재 v2 복구·응답" up_as "$v2"
  check "  └ 대상 v1 은 previous 로 원위치(삭제 안 됨)" is "$name-previous" "$v1 false"
  check "  └ 잔존 컨테이너 없음" only_known
done

echo "failures: $fails"
exit "$fails"
