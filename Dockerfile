# z-wf Dockerfile — 多阶段构建 (Eclipse Temurin 8 JDK + JRE)
# 用于把 z-wf-admin 打成可对外提供服务的镜像

# ============================================================
# Stage 1: build (maven:3.9.9 + Eclipse Temurin 8 JDK)
# ============================================================
FROM maven:3.9.9-eclipse-temurin-8 AS builder

ARG M2_REPO=/root/.m2
WORKDIR /build

# 1) 先复制 parent + 子模块 pom + 子模块源码
COPY ./pom.xml ./pom.xml
COPY ./z-wf-core ./z-wf-core
COPY ./z-wf-web ./z-wf-web
COPY ./z-wf-starter ./z-wf-starter
COPY ./z-wf-admin ./z-wf-admin

# 2) install parent (NEXUS 跳过测试和 javadoc, 加速)
RUN mvn -N -f pom.xml install -DskipTests -Dmaven.javadoc.skip=true -q

# 3) 编译 admin 及其依赖 (-am)
RUN mvn -f pom.xml -pl z-wf-admin -am clean package \
        -DskipTests \
        -Dmaven.javadoc.skip=true \
        -q

# ============================================================
# Stage 2: runtime (Eclipse Temurin 8 JRE + 非 root 用户)
# ============================================================
FROM eclipse-temurin:8-jre

LABEL maintainer="zifang"
LABEL description="z-wf - Camunda-based Workflow Engine (REST + z-config + z-rpc)"

# 时区
RUN apt-get update && apt-get install -y --no-install-recommends tzdata wget && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apt-get remove -y tzdata && apt-get autoremove -y && apt-get clean

# 创建非 root 用户
RUN useradd --system --uid 10001 zwf

WORKDIR /app

# 复制可执行 jar (Spring Boot fat jar, 由 z-wf-admin 的 spring-boot-maven-plugin repackage 生成, classifier=exec)
COPY --from=builder /build/z-wf-admin/target/z-wf-admin-*-exec.jar /app/z-wf-admin.jar

RUN mkdir -p /app/logs && chown -R zwf:zwf /app
USER 10001

# 端口: 8080 (审批中心 REST API + 自带管理前端)
EXPOSE 8080

# 健康检查 (WfBaseHealthController = /api/wf/health, 兼容 SERVER_CONTEXT_PATH 前缀)
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget -q -O- "http://127.0.0.1:8080${SERVER_CONTEXT_PATH:-}/api/wf/health" || exit 1

# JVM 默认参数 (可在 docker run -e JVM_OPTS=... 覆盖)
ENV JVM_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 启动入口类: com.zifang.z.wf.admin.ZWfAdminApplication
ENTRYPOINT ["sh", "-c", "exec java $JVM_OPTS -jar /app/z-wf-admin.jar --spring.main.banner-mode=off"]
