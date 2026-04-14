# ── 构建阶段 ──────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests -q

# ── 运行阶段 ──────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 创建非 root 用户
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=builder /build/target/*.jar app.jar

# 限制 JVM 内存（Koyeb 免费层 512 MB）
ENV JAVA_OPTS="-Xms128m -Xmx320m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
