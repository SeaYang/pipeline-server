# =============================================================================
# 多阶段构建：构建阶段 + 运行阶段
# 运行镜像使用 JDK（非 JRE），保留 jstack/jmap/jstat 等诊断工具
# =============================================================================

# ---------- 构建阶段 ----------
FROM maven:3.8.8-eclipse-temurin-8 AS builder

WORKDIR /build

# 先复制 pom 文件，利用 Docker 层缓存加速依赖下载
COPY pom.xml .
COPY pipeline-server-common/pom.xml   pipeline-server-common/
COPY pipeline-server-dao/pom.xml      pipeline-server-dao/
COPY pipeline-server-facade/pom.xml   pipeline-server-facade/
COPY pipeline-server-service/pom.xml  pipeline-server-service/

# 复制 Maven settings.xml（argo-client-java 需要 GitHub Packages 认证）
# 构建前请将 config/maven-settings.xml 中的 GitHub 用户名和 Token 替换为真实值
COPY config/maven-settings.xml /root/.m2/settings.xml

# 下载依赖（首次较慢，后续命中缓存）
RUN mvn dependency:go-offline -B -s /root/.m2/settings.xml

# 复制源码并打包（不绑定环境，制品环境无关）
COPY pipeline-server-common/src   pipeline-server-common/src
COPY pipeline-server-dao/src      pipeline-server-dao/src
COPY pipeline-server-facade/src   pipeline-server-facade/src
COPY pipeline-server-service/src  pipeline-server-service/src

RUN mvn clean package -DskipTests -B -s /root/.m2/settings.xml

# ---------- 运行阶段 ----------
# 使用 JDK 而非 JRE：保留 jstack/jmap/jstat/jcmd 等诊断工具
# 镜像仅大几十 MB，但生产排查问题时可直接 docker exec 使用
FROM eclipse-temurin:8-jdk

LABEL maintainer="SeaYang"

WORKDIR /app

# 从构建阶段复制可执行 jar
COPY --from=builder /build/pipeline-server-service/target/pipeline-server-service-*.jar app.jar

# 通过环境变量提供默认值，启动时可覆盖
# SPRING_PROFILES_ACTIVE 未设置时不加载额外 profile，仅用 application.yml
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=""

EXPOSE 9000

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-local}"]
