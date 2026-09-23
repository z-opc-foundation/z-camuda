#!/bin/bash
# ============================================================
# wf-demo.sh — z-wf 端到端演示 (REST API + 健康检查)
# 演示: 健康检查 → 流程定义 → 启动请假流程
# ============================================================
# 用法:
#   ./wf-demo.sh health                          # 健康检查
#   ./wf-demo.sh definitions                     # 列出流程定义
#   ./wf-demo.sh start alice bob 3 sick-leave    # 启动请假流程
# ============================================================
set -e

Z_WF_HOST="${Z_WF_HOST:-127.0.0.1}"
Z_WF_PORT="${Z_WF_PORT:-18080}"
Z_WF_RPC_HOST="${Z_WF_RPC_HOST:-127.0.0.1}"
Z_WF_RPC_PORT="${Z_WF_RPC_PORT:-20880}"

ACTION="${1:-help}"
shift || true

# 通用参数
APPLICANT="${1:-alice}"
APPROVER="${2:-bob}"
DAYS="${3:-3}"
REASON="${4:-test-from-wf-demo}"

log() { echo -e "\033[32m[wf-demo]\033[0m $1"; }
warn() { echo -e "\033[33m[wf-demo]\033[0m $1"; }

case "$ACTION" in
  health)
    log "GET ${Z_WF_HOST}:${Z_WF_PORT}/api/wf/health"
    curl -s -w "\n  HTTP %{http_code}\n" "http://${Z_WF_HOST}:${Z_WF_PORT}/api/wf/health"
    ;;
  rpc-port)
    log "检查 z-wf Netty RPC 端口: ${Z_WF_RPC_HOST}:${Z_WF_RPC_PORT}"
    nc -z "${Z_WF_RPC_HOST}" "${Z_WF_RPC_PORT}" && echo "  ✓ Port OPEN" || echo "  ✗ Port CLOSED"
    ;;
  definitions)
    log "GET /api/approval-center/processes/definitions"
    curl -s "http://${Z_WF_HOST}:${Z_WF_PORT}/api/approval-center/processes/definitions" | python3 -m json.tool
    ;;
  start)
    log "POST /api/leave/start (REST API)"
    RESP=$(curl -s -X POST "http://${Z_WF_HOST}:${Z_WF_PORT}/api/leave/start" \
      -H "Content-Type: application/json" \
      -d "{\"applicant\":\"${APPLICANT}\",\"approver\":\"${APPROVER}\",\"days\":${DAYS},\"reason\":\"${REASON}\"}")
    echo "$RESP" | python3 -m json.tool
    log "✓ 流程已启动, 返回 processInstanceId"
    ;;
  help|*)
    echo "用法: $0 {health|rpc-port|definitions|start} [args]"
    echo ""
    echo "Commands:"
    echo "  health                       REST API 健康检查"
    echo "  rpc-port                     检查 z-rpc Netty 端口是否监听"
    echo "  definitions                  列出已部署的流程定义"
    echo "  start [applicant] [approver] [days] [reason]"
    echo "                               REST API 启动请假流程"
    ;;
esac
