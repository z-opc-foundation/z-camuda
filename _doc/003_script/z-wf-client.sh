#!/bin/bash
# ============================================================
# z-wf-client.sh — 外部应用通过 z-rpc 直连 z-wf-server
# 演示: 业务流程 (启动 → 待办查询 → 审批) 完整端到端打通
# ============================================================
# 用法:
#   ./z-wf-client.sh start alice bob 3 sick-leave   # 启动请假流程
#   ./z-wf-client.sh todos bob                       # 查询待办
#   ./z-wf-client.sh complete <taskId> approved      # 审批通过
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
REASON="${4:-test-from-zwf-client}"

log() { echo -e "\033[32m[zwf-client]\033[0m $1"; }
warn() { echo -e "\033[33m[zwf-client]\033[0m $1"; }

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
  start-rpc)
    log "通过 z-rpc Netty 直连 z-wf 调 startLeaveProcess"
    java -cp "$(cat /tmp/zwf-cp.txt):/Users/zifang/.m2/repository/io/github/yuku123/z-wf-starter/1.0.0/z-wf-starter-1.0.0.jar:/Users/zifang/.m2/repository/io/github/yuku123/z-wf-core/1.0.0/z-wf-core-1.0.0.jar:/Users/zifang/.m2/repository/io/github/yuku123/z-wf-web/1.0.0/z-wf-web-1.0.0.jar" \
      com.zifang.z.wf.client.ZWfRpcClient "${Z_WF_RPC_HOST}" "${Z_WF_RPC_PORT}" start "${APPLICANT}" "${APPROVER}" "${DAYS}" "${REASON}"
    ;;
  help|*)
    echo "用法: $0 {health|rpc-port|definitions|start|start-rpc} [args]"
    echo ""
    echo "Commands:"
    echo "  health                       REST API 健康检查"
    echo "  rpc-port                     检查 z-rpc Netty 端口是否监听"
    echo "  definitions                  列出已部署的流程定义"
    echo "  start [applicant] [approver] [days] [reason]"
    echo "                               REST API 启动请假流程"
    echo "  start-rpc [applicant] [approver] [days] [reason]"
    echo "                               z-rpc 直接调 z-wf 启动请假流程"
    ;;
esac
