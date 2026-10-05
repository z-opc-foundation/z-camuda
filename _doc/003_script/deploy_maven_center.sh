#!/bin/bash
# ============================================================
# z-camuda Maven Central 发布脚本
# ============================================================
# 用法:
#   ./deploy_maven_center.sh publish   - 编译并发布到 Maven Central
#   ./deploy_maven_center.sh verify    - 仅验证 POM 元信息
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ACTION="${1:-publish}"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() { echo -e "${GREEN}[deploy]${NC} $1"; }
warn() { echo -e "${YELLOW}[deploy]${NC} $1"; }
err() { echo -e "${RED}[deploy]${NC} $1"; exit 1; }

# ============================================================
# 验证 POM 元信息
# ============================================================
verify_pom() {
    log "验证 POM 元信息..."
    cd "$REPO_ROOT"

    # 检查 Central 所需字段
    local required_fields=("name" "description" "url" "licenses" "developers" "scm")
    local pom_content
    pom_content=$(cat pom.xml)

    for field in "${required_fields[@]}"; do
        if ! echo "$pom_content" | grep -q "<$field>"; then
            err "pom.xml 缺少 <${field}> 标签"
        fi
    done

    log "✅ POM 元信息验证通过"
}

# ============================================================
# 发布
# ============================================================
publish() {
    log "开始发布 z-camuda 到 Maven Central..."
    cd "$REPO_ROOT"

    # 1. 清理
    log "1/4 清理..."
    mvn clean -q

    # 2. 编译
    log "2/4 编译..."
    mvn compile -DskipTests -q

    # 3. 安装到本地
    log "3/4 安装到本地仓库..."
    mvn install -DskipTests -q

    # 4. 发布 (central profile: sources + javadoc + gpg 签名 + Central Portal 上传)
    log "4/4 发布到 Maven Central..."
    mvn deploy -Pcentral -DskipTests 2>&1 | tee /tmp/z-camuda-deploy.log

    # 检查结果
    if grep -q "BUILD SUCCESS" /tmp/z-camuda-deploy.log; then
        log "✅ BUILD SUCCESS"
    else
        err "❌ BUILD FAILED"
    fi

    # 提取 Deployment ID
    local deployment_id
    deployment_id=$(grep -o 'deploymentId: [a-f0-9\-]*' /tmp/z-camuda-deploy.log | head -1 | awk '{print $2}')
    if [ -n "$deployment_id" ]; then
        log "✅ Bundle uploaded"
        log "   Deployment ID: $deployment_id"
        log "   Central Portal: https://central.sonatype.com/publishing/deployments"
    else
        log "⚠️  未找到 Deployment ID, 请检查 /tmp/z-camuda-deploy.log"
    fi
}

# ============================================================
# 主入口
# ============================================================
case "$ACTION" in
    verify)
        verify_pom
        ;;
    publish)
        verify_pom
        publish
        ;;
    *)
        echo "用法: $0 {verify|publish}"
        exit 1
        ;;
esac
