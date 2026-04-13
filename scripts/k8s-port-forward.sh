#!/bin/bash
# =============================================================================
# K8s 포트포워드 스크립트 — 로컬에서 K8s 서비스 접근
# 사용법: bash scripts/k8s-port-forward.sh [start|stop|status]
# =============================================================================

NAMESPACE="rental-commerce-dev"
PID_DIR="/tmp/k8s-pf-pids"
mkdir -p "$PID_DIR"

# 포트포워드 설정: "서비스명 로컬포트 원격포트 설명"
SERVICES=(
  "rental-api 8080 8080 BE-API"
  "rental-web 3000 3000 FE-Web"
  "grafana 3001 3000 Grafana"
  "prometheus 9090 9090 Prometheus"
  "pinpoint-web 28080 8080 Pinpoint"
  "argocd-server:argocd 8443 443 ArgoCD"
  "mysql 3306 3306 MySQL"
  "redis 6379 6379 Redis"
  "minio 9000 9000 MinIO-API"
  "minio 9001 9001 MinIO-Console"
  "mongodb 27017 27017 MongoDB"
  "kafka 9092 9092 Kafka"
)

start_forward() {
  local svc_info=$1
  local svc=$(echo "$svc_info" | awk '{print $1}')
  local local_port=$(echo "$svc_info" | awk '{print $2}')
  local remote_port=$(echo "$svc_info" | awk '{print $3}')
  local desc=$(echo "$svc_info" | awk '{print $4}')
  local pid_file="$PID_DIR/pf-$local_port.pid"

  # 네임스페이스 분리 (argocd 등)
  local ns="$NAMESPACE"
  if [[ "$svc" == *":"* ]]; then
    ns="${svc##*:}"
    svc="${svc%%:*}"
  fi

  # 이미 실행 중인지 확인
  if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "  ✅ $desc — localhost:$local_port (이미 실행 중)"
    return
  fi

  # 포트포워드 시작
  kubectl port-forward "svc/$svc" "$local_port:$remote_port" -n "$ns" > /dev/null 2>&1 &
  local pid=$!
  echo "$pid" > "$pid_file"

  # 연결 확인
  sleep 1
  if kill -0 "$pid" 2>/dev/null; then
    echo "  ✅ $desc — localhost:$local_port"
  else
    echo "  ❌ $desc — 실패 (서비스 미실행?)"
    rm -f "$pid_file"
  fi
}

stop_all() {
  echo "포트포워드 종료 중..."
  for pid_file in "$PID_DIR"/pf-*.pid; do
    if [ -f "$pid_file" ]; then
      local pid=$(cat "$pid_file")
      local port=$(basename "$pid_file" | sed 's/pf-//;s/.pid//')
      kill "$pid" 2>/dev/null && echo "  종료: localhost:$port (PID $pid)"
      rm -f "$pid_file"
    fi
  done
  echo "완료."
}

show_status() {
  echo "=========================================="
  echo " K8s 포트포워드 상태"
  echo "=========================================="
  for entry in "${SERVICES[@]}"; do
    local svc=$(echo "$entry" | awk '{print $1}')
    local local_port=$(echo "$entry" | awk '{print $2}')
    local desc=$(echo "$entry" | awk '{print $4}')
    local pid_file="$PID_DIR/pf-$local_port.pid"

    if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
      echo "  ✅ $desc — http://localhost:$local_port"
    else
      echo "  ⬚  $desc — 미실행"
      rm -f "$pid_file" 2>/dev/null
    fi
  done
}

case "${1:-start}" in
  start)
    echo "=========================================="
    echo " K8s 포트포워드 시작"
    echo "=========================================="
    for entry in "${SERVICES[@]}"; do
      start_forward "$entry"
    done
    echo ""
    echo "=========================================="
    echo " 접근 URL"
    echo "=========================================="
    echo "  BE API:       http://localhost:8080"
    echo "  FE Web:       http://localhost:3000"
    echo "  Grafana:      http://localhost:3001 (admin/admin)"
    echo "  Prometheus:   http://localhost:9090"
    echo "  Pinpoint:     http://localhost:28080"
    echo "  ArgoCD:       https://localhost:8443 (admin/OBdK7FyhEbO5RDNz)"
    echo "  MinIO Console:http://localhost:9001 (minioadmin/minioadmin)"
    echo "  MySQL:        localhost:3306 (rental/rental)"
    echo "  Redis:        localhost:6379"
    echo "  MongoDB:      localhost:27017"
    echo "=========================================="
    ;;
  stop)
    stop_all
    ;;
  status)
    show_status
    ;;
  *)
    echo "사용법: $0 [start|stop|status]"
    exit 1
    ;;
esac
