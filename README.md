# pipeline-server

> 基于 Argo Workflows 的 CI/CD 流水线平台后端服务，提供流水线编排、任务调度、制品管理、事件触发等核心能力。

## 关联仓库

流水线平台由多个仓库组成，协同工作：

| 仓库 | 说明 |
|------|------|
| <a href="https://github.com/SeaYang/pipeline-frontend" target="_blank">pipeline-frontend</a> | 流水线平台前端 |
| <a href="https://github.com/SeaYang/pipeline-manifests" target="_blank">pipeline-manifests</a> | 流水线平台清单文件（原子任务模板、流水线编排、环境搭建） |
| <a href="https://github.com/SeaYang/cix-cli" target="_blank">cix-cli</a> | 基础命令行工具 |

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 1.8 |
| 框架 | Spring Boot | 2.7.18 |
| 微服务 | Spring Cloud | 2021.0.9 |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.0 |
| 连接池 | Druid | 1.2.20 |
| 流水线引擎 | Argo Workflows Client | v3.4.8 |
| 容器编排 | Kubernetes Java Client | 14.0.1 |
| 代码托管 | GitLab4J API | 5.8.0 |
| 构建工具 | Maven | 3.6+ |

## 模块架构

项目采用多模块 Maven 结构：

```
pipeline-server/
├── pipeline-server-common      # 公共模块：工具类、常量、异常、统一返回封装
├── pipeline-server-dao         # 数据访问层：Entity、Mapper、XML 映射
├── pipeline-server-facade      # 对外接口层：API 定义、DTO、Feign 客户端
└── pipeline-server-service     # 业务层：Controller、Service、启动类、配置文件
```

模块依赖关系：

```
service → dao → common
service → facade → common
```

## 环境准备

后端服务依赖以下外部组件，请先完成环境搭建：

| 组件 | 说明 |
|------|------|
| MySQL 8.0 | 数据存储 |
| Argo Workflows | 流水线执行引擎 |
| Kubernetes 集群 | 容器编排（Argo Workflows 运行环境） |

完整的从零搭建教程（虚拟机 → Docker → K8s → Argo Workflows → Nexus3），请参考 pipeline-manifests 的 <a href="https://github.com/SeaYang/pipeline-manifests#1-%E7%8E%AF%E5%A2%83%E5%87%86%E5%A4%87" target="_blank">环境准备</a> 章节。

## 数据库初始化

1. 创建数据库：

```sql
CREATE DATABASE pipeline DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

2. 执行初始化脚本（聚合了所有建表语句）：

```bash
mysql -u root -p pipeline < sql/init.sql
```

> 单表 DDL 可在 `sql/` 目录下查看各独立 `.sql` 文件。
>
> **基础数据**（字典、默认配置等）的初始化待后续补充。

## 本地开发

### 环境要求

- JDK 1.8
- Maven 3.6+
- MySQL 8.0

### 步骤

1. **克隆代码**

```bash
git clone https://github.com/SeaYang/pipeline-server.git
cd pipeline-server
```

2. **初始化数据库**

按照 [数据库初始化](#数据库初始化) 章节执行。

3. **配置 Maven settings.xml（Argo SDK 依赖）**

项目依赖 `argo-client-java`，该包发布在 GitHub Packages 仓库，需要配置 Maven 认证才能拉取。

参考项目内的 `config/maven-settings.xml`，关键配置：

```xml
<servers>
  <!-- GitHub Packages 认证（拉取 argo-client-java） -->
  <server>
    <id>github</id>
    <username>你的GitHub用户名</username>
    <password>你的GitHub Personal Access Token（需 read:packages 权限）</password>
  </server>
</servers>

<mirrors>
  <!-- 阿里云镜像加速公共依赖 -->
  <mirror>
    <id>alimaven</id>
    <name>aliyun maven</name>
    <url>http://maven.aliyun.com/nexus/content/groups/public/</url>
    <mirrorOf>central</mirrorOf>
  </mirror>
</mirrors>

<profiles>
  <profile>
    <id>github</id>
    <repositories>
      <repository>
        <id>central</id>
        <url>https://repo.maven.apache.org/maven2</url>
      </repository>
      <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/argoproj/argo-workflows</url>
      </repository>
    </repositories>
  </profile>
</profiles>

<activeProfiles>
  <activeProfile>github</activeProfile>
</activeProfiles>
```

将配置好的 `settings.xml` 放到 `~/.m2/settings.xml`，或在构建时通过 `-s` 参数指定：

```bash
mvn install -DskipTests -s config/maven-settings.xml
```

4. **修改本地配置**

编辑 `pipeline-server-service/src/main/resources/application-local.yml`，修改以下配置项：

```yaml
# 数据库连接（改为你的本地地址和账号）
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/pipeline?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: 你的密码

# Argo Workflows Server
Argo:
  server:
    url: https://<your-argo-host>:2746
    token: "Bearer <your-argo-token>"
    namespace: argo

# Kubernetes API Server
kubernetes:
  client:
    master-url: https://<your-k8s-host>:6443
    token: "<your-k8s-token>"
    verifying-ssl: false
```

5. **启动服务**

**方式一：IDE 启动（推荐）**

直接运行启动类 `com.ci.pipeline.service.bootstrap.Application`，默认加载 `local` 配置。

**方式二：命令行启动**

```bash
# 1. 在根目录编译安装所有模块到本地仓库（首次或依赖变更后执行）
mvn install -DskipTests

# 2. 启动 service 模块
mvn spring-boot:run -pl pipeline-server-service -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

6. **验证**

服务启动后访问健康检查接口：

```bash
curl http://localhost:9000/actuator/health
```

## 配置管理（多环境）

### 设计理念：环境无关制品

与前端项目（Vue/React 打包时与环境绑定）不同，**Java 可执行 JAR 天然支持环境无关**——打一个 jar 包，到处运行，运行时通过参数指定环境。这是 Spring Boot 的最佳实践（[12-Factor App](https://12factor.net/config) 的 Config 原则）。

### Profile 机制

项目通过 Spring Boot Profile 实现多环境配置：

```
application.yml            # 公共配置（所有环境共享）
application-local.yml      # 本地开发环境
application-test.yml       # 测试环境（待补充）
application-prd.yml        # 生产环境（待补充）
```

- `application.yml` 是公共配置，所有环境都会加载
- `application-{profile}.yml` 是环境特定配置，会**覆盖**公共配置中的同名项
- 启动时通过 `--spring.profiles.active=<profile>` 指定加载哪个环境配置

### 如何指定环境

以下三种方式，**优先级从高到低**：

```bash
# 方式一：命令行参数（最高优先级，推荐）
java -jar app.jar --spring.profiles.active=prd

# 方式二：环境变量
export SPRING_PROFILES_ACTIVE=prd
java -jar app.jar

# 方式三：Docker 部署时通过 -e 传入
docker run -e SPRING_PROFILES_ACTIVE=prd pipeline-server:latest
```

> 如果不指定 profile，默认加载 `local` 配置（见 Dockerfile 中 ENTRYPOINT 的默认值）。

### 敏感信息处理

生产环境的数据库密码、Token 等敏感信息，可以通过命令行参数或环境变量覆盖配置文件中的值：

```bash
# 方式一：命令行参数覆盖单个配置项
java -jar app.jar --spring.profiles.active=prd --spring.datasource.password=你的密码

# 方式二：环境变量（Spring Boot 自动映射，驼峰转下划线）
export SPRING_DATASOURCE_PASSWORD=你的密码
java -jar app.jar
```

## 打包部署

### 方式一：可执行 JAR

```bash
# 打包（制品环境无关，不绑定任何环境）
mvn clean package -DskipTests

# 运行（启动时指定环境）
java -jar pipeline-server-service/target/pipeline-server-service-1.0.0.jar --spring.profiles.active=local
```

### 方式二：Docker 部署

项目提供 `Dockerfile`，采用**多阶段构建**：

- **构建阶段**：`maven:3.8.8-eclipse-temurin-8` 编译打包
- **运行阶段**：`eclipse-temurin:8-jdk` 运行（使用 JDK 而非 JRE，保留 `jstack`/`jmap`/`jstat` 等诊断工具，方便生产排查问题）

> ⚠️ **构建前**：Dockerfile 会将 `config/maven-settings.xml` 复制到构建容器中用于拉取 Argo SDK。请先将该文件中的 GitHub 用户名和 Token 替换为真实值（参考 [本地开发 - 配置 Maven settings.xml](#3-配置-maven-settingsxmlargo-sdk-依赖)）。

```bash
# 构建镜像
docker build -t pipeline-server:latest .

或者

docker build \
  --build-arg HTTP_PROXY=socks5://host.docker.internal:7897 \
  --build-arg HTTPS_PROXY=socks5://host.docker.internal:7897 \
  --build-arg ALL_PROXY=socks5://host.docker.internal:7897 \
  -t pipeline-server:latest .

# 运行容器（指定环境）
docker run -d \
  --name pipeline-server \
  -p 9000:9000 \
  -e SPRING_PROFILES_ACTIVE=local \
  pipeline-server:latest

或者

docker run -d \
  --name pipeline-server \
  -p 9000:9000 \
  -e SPRING_PROFILES_ACTIVE=local \
  -e SPRING_DATASOURCE_URL='jdbc:mysql://host.docker.internal:3306/pipeline?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true' \
  pipeline-server:latest
```

**自定义 JVM 参数**：

```bash
docker run -d \
  --name pipeline-server \
  -p 9000:9000 \
  -e JAVA_OPTS="-Xms512m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError" \
  -e SPRING_PROFILES_ACTIVE=prd \
  pipeline-server:latest
```

> 💡 **为什么用 JDK 镜像而非 JRE？**
>
> JRE 镜像缺少 `jstack`（线程栈）、`jmap`（堆 dump）、`jstat`（GC 监控）、`jcmd`（综合诊断）等工具。生产环境遇到死锁、OOM 等问题时，JDK 镜像可以直接 `docker exec` 进容器排查，代价仅多几十 MB 镜像大小，非常值得。

## 健康检查

| 接口 | 说明 |
|------|------|
| `GET /admin/health` | 健康检查 |
| `GET /admin/info` | 应用信息 |
| `GET /actuator/health` | Actuator 健康检查 |