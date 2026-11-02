#!/usr/bin/env bash
# 수동 승인된 CodeDeploy 번들에서 실행되는 API 컨테이너 배포/롤백 스크립트.
#   deploy-container.sh [deploy]  # release.env 의 IMAGE_REF(digest 고정)로 교체, 실패 시 자동 복구
#   deploy-container.sh rollback  # 직전 컨테이너(<name>-previous)로 되돌림
# 기존 env 파일·TZ·포트를 유지한다. 이미지·네트워크 prune 은 하지 않는다(다른 서비스 이미지 보호).
# 기존 앱을 멈춘 뒤의 어떤 실패(stop/rename/create/network connect/start/헬스)도 EXIT trap 이 기존 앱을 되살린다.
set -euo pipefail
export PATH=$PATH:/usr/local/bin:/usr/bin

here=$(cd "$(dirname "$0")" && pwd)
if [[ -f "$here/release.env" ]]; then
  # shellcheck source=/dev/null
  source "$here/release.env"
fi

# --- 서비스별 기본값 (미설정일 때만 적용; 테스트에서는 환경변수로 덮어쓴다) ---
: "${CONTAINER_NAME:=gwangsan-container}"
: "${HOST_PORT:=8080}"
: "${CONTAINER_PORT:=8080}"
: "${ENV_FILE:=/home/ubuntu/.env}"
: "${HEALTH_PATH:=/api/health}"
: "${HEALTH_TIMEOUT_SECONDS:=300}"  # 에이전트 계측+소형 EC2 기동 여유
: "${OBS_NETWORK=gwangsan-observability}"  # 명시적 빈 값(OBS_NETWORK=)이면 관측 네트워크 연결 생략
: "${OBS_ALIAS:=gwangsan-api}"
: "${IMAGE_REPOSITORY:=gwangsan-app}"
: "${OTEL_ENABLED:=true}"
: "${OTEL_TRACES_SAMPLER_ARG:=0.1}"
: "${DEPLOYMENT_ENVIRONMENT:=production}"
: "${AWS_REGION:=ap-northeast-2}"
: "${REGISTRY_LOGIN:=true}"

# 서비스별 docker -e 인자. -e 는 --env-file 보다 우선하므로 OTel 토글은 release.env 가 단일 기준이다.
service_args() {
  local enabled=false
  [[ "$OTEL_ENABLED" == "true" ]] && enabled=true
  printf '%s\n' \
    -e TZ=Asia/Seoul \
    -e "OTEL_JAVAAGENT_ENABLED=$enabled" \
    -e OTEL_INSTRUMENTATION_LETTUCE_ENABLED=false \
    -e "OTEL_TRACES_SAMPLER_ARG=$OTEL_TRACES_SAMPLER_ARG" \
    -e "OTEL_RESOURCE_ATTRIBUTES=deployment.environment.name=$DEPLOYMENT_ENVIRONMENT"
}
# --- 기본값 끝 ---

previous="${CONTAINER_NAME}-previous"
log() { echo "[release] $*"; }
die() { echo "[release] ERROR: $*" >&2; exit 1; }
exists() { docker container inspect "$1" >/dev/null 2>&1; }
id_of() { docker container inspect -f '{{.Id}}' "$1" 2>/dev/null || true; }

# 복구 상태
#   none     아무것도 안 건드렸거나 성공 → 복구 없음
#   stopping 기존 앱(old_id)이 원래 이름 그대로, 멈췄을 수 있음 → 그 자리에서 다시 start
#   swapped  기존 앱이 다른 이름으로 이동 → 새 컨테이너 정리 후 기존 앱을 원래 이름으로 되돌려 start
# mode=rollback 이면 new_id 는 보관 중이던 previous 컨테이너이므로 지우지 않고 previous 로 되돌린다.
state=none mode=deploy old_id="" new_id=""
# 이번 실행이 만든 컨테이너에만 붙는 표식. create 가 오류를 보고했을 때 이름 보유자를 넘겨받을지 판단한다.
run_label="gwangsan.release.run=$$-${RANDOM}-$(date +%s)"
label_of() { docker container inspect -f '{{index .Config.Labels "gwangsan.release.run"}}' "$1" 2>/dev/null || true; }

recover() {
  set +e
  if [[ "$state" == swapped ]]; then
    # create 가 컨테이너를 만든 뒤 오류를 보고하면 new_id 가 비어 있다. 이름 보유자는 이번 실행의
    # 표식이 있을 때만 넘겨받는다. 다른 주체가 이름을 가져간 경우 그 컨테이너는 절대 건드리지 않는다.
    local holder
    holder=$(id_of "$CONTAINER_NAME")
    if [[ -z "$new_id" && -n "$holder" && "$holder" != "$(id_of "$old_id")" \
          && "$(label_of "$holder")" == "${run_label#*=}" ]]; then new_id=$holder; fi
    if [[ -n "$new_id" ]] && exists "$new_id"; then
      if [[ "$mode" == rollback ]]; then
        docker stop "$new_id" >/dev/null
        [[ "$(id_of "$CONTAINER_NAME")" == "$new_id" ]] && docker rename "$new_id" "$previous"
      else
        docker logs --tail 50 "$new_id" >&2
        docker rm -f "$new_id" >/dev/null
      fi
    fi
  fi
  [[ -n "$old_id" ]] || return 0  # 첫 배포: 되살릴 기존 앱이 없다
  # rename 이 실제로는 적용된 뒤 실패를 보고한 경우까지 포함해, 기존 앱을 원래 이름으로 되돌린다.
  if [[ "$(id_of "$CONTAINER_NAME")" != "$(id_of "$old_id")" ]]; then
    if exists "$CONTAINER_NAME"; then
      # 이름은 되찾을 수 없지만 서비스는 살린다(기존 앱은 지금 이름 그대로 start; 호스트 포트는 기존 앱 소유).
      echo "[release] ERROR: $CONTAINER_NAME is held by a container this run did not create; leaving it untouched" >&2
      docker start "$old_id" >/dev/null && log "started original container ${old_id:0:12} under its current name"
      echo "[release] ERROR: manual action required: rename ${old_id:0:12} back to $CONTAINER_NAME after inspecting the holder" >&2
      return 1
    fi
    docker rename "$old_id" "$CONTAINER_NAME" || return 1
  fi
  log "recovering: starting original container ${old_id:0:12}"
  if docker start "$old_id" >/dev/null; then
    log "restored $(docker container inspect -f '{{.Config.Image}}' "$old_id")"
    return 0
  fi
  echo "[release] ERROR: automatic restore FAILED; manual action required (container ${old_id:0:12})" >&2
  return 1
}

on_exit() {
  local rc=$?
  trap - EXIT
  if [[ $rc -ne 0 && "$state" != none ]]; then
    recover || true
  fi
  exit "$rc"
}
trap on_exit EXIT

healthy() {
  local deadline=$((SECONDS + HEALTH_TIMEOUT_SECONDS))
  while (( SECONDS < deadline )); do
    if [[ "$(docker container inspect -f '{{.State.Running}}' "$CONTAINER_NAME" 2>/dev/null)" != "true" ]]; then
      return 1
    fi
    curl -fsS -o /dev/null --max-time 5 "http://127.0.0.1:${HOST_PORT}${HEALTH_PATH}" && return 0
    sleep 3
  done
  return 1
}

check_image_ref() {
  [[ "${IMAGE_REF:-}" =~ ^[^@[:space:]]+@sha256:[0-9a-f]{64}$ ]] || die "IMAGE_REF must be an immutable repo@sha256:<digest> reference"
  [[ "$REGISTRY_LOGIN" == "true" ]] || return 0
  local account
  account=$(aws sts get-caller-identity --query Account --output text)
  [[ "${IMAGE_REF%%@*}" == "${account}.dkr.ecr.${AWS_REGION}.amazonaws.com/${IMAGE_REPOSITORY}" ]] \
    || die "IMAGE_REF must be in this account's ${AWS_REGION} ECR repository ${IMAGE_REPOSITORY} (account ${account})"
}

deploy() {
  check_image_ref
  [[ -f "$ENV_FILE" ]] || die "env file $ENV_FILE not found"
  if [[ -n "$OBS_NETWORK" ]]; then
    docker network inspect "$OBS_NETWORK" >/dev/null 2>&1 || die "network $OBS_NETWORK missing; create it (platform runbook) or set OBS_NETWORK= to skip"
  fi

  if [[ "$REGISTRY_LOGIN" == "true" ]]; then
    aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "${IMAGE_REF%%/*}"
  fi
  docker pull "$IMAGE_REF"

  local args=() a
  while IFS= read -r a; do args+=("$a"); done < <(service_args)

  # 이전 컨테이너는 지우지 않고 멈춘 채 <name>-previous 로 보관한다(즉시 롤백용).
  if exists "$CONTAINER_NAME"; then
    old_id=$(id_of "$CONTAINER_NAME")
    if exists "$previous"; then docker rm -f "$previous" >/dev/null; fi
    state=stopping
    docker stop "$old_id" >/dev/null
    docker rename "$old_id" "$previous"
    log "kept previous $(docker container inspect -f '{{.Config.Image}}' "$old_id") as $previous"
  fi
  state=swapped

  new_id=$(docker create --name "$CONTAINER_NAME" --label "$run_label" \
    -p "${HOST_PORT}:${CONTAINER_PORT}" \
    --env-file "$ENV_FILE" \
    "${args[@]}" \
    "$IMAGE_REF")
  # 기본 bridge 는 유지하고 관측 네트워크만 추가한다(Alloy: alloy:4318, 스크레이프: 별칭).
  if [[ -n "$OBS_NETWORK" ]]; then
    docker network connect --alias "$OBS_ALIAS" "$OBS_NETWORK" "$new_id"
  fi
  docker start "$new_id" >/dev/null

  healthy || die "health check failed for $IMAGE_REF"
  state=none
  log "healthy: $IMAGE_REF"
}

rollback() {
  mode=rollback
  exists "$previous" || die "no $previous container to restore"
  new_id=$(id_of "$previous")
  if exists "$CONTAINER_NAME"; then
    old_id=$(id_of "$CONTAINER_NAME")
    if exists "${CONTAINER_NAME}-rolledback"; then docker rm -f "${CONTAINER_NAME}-rolledback" >/dev/null; fi
    state=stopping
    docker stop "$old_id" >/dev/null
    docker rename "$old_id" "${CONTAINER_NAME}-rolledback"
  fi
  state=swapped
  docker rename "$new_id" "$CONTAINER_NAME"
  docker start "$new_id" >/dev/null
  # 대상이 실제로 준비될 때까지 성공으로 보지 않는다. 실패하면 EXIT trap 이 현재 앱을 되살린다.
  healthy || die "health check failed for rollback target"
  state=none
  log "rolled back to $(docker container inspect -f '{{.Config.Image}}' "$new_id")"
  # 되돌려진 쪽을 다음 rollback 대상으로 보관한다(실패해도 롤백 자체는 유지).
  if [[ -n "$old_id" ]]; then
    docker rename "$old_id" "$previous" || echo "[release] WARN: could not rename ${CONTAINER_NAME}-rolledback to $previous" >&2
  fi
}

case "${1:-deploy}" in  # CodeDeploy 훅은 인자 없이 실행한다
  deploy) deploy ;;
  rollback) rollback ;;
  *) die "usage: $0 deploy|rollback" ;;
esac
