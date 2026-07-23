# AGENTS.md

This file provides guidance to AI coding agents (such as Claude Code, Cursor, GitHub Copilot, Codex, Gemini, etc.) when working with code in this repository.

## Project Overview

- **Project Name**: ci-pipeline-server
- **Type**: Spring Cloud Microservice
- **JDK Version**: 1.8
- **Spring Boot**: 2.7.18
- **Spring Cloud**: 2021.0.9
- **Build Tool**: Maven
- **Database**: MySQL (database name: pipeline)

## Module Architecture

The project follows a multi-module Maven structure:

- **pipeline-server-common**: 公共模块 - 通用工具类、常量、异常定义、统一返回结果封装
- **pipeline-server-dao**: 数据访问层 - MyBatis-Plus Entity、Mapper 接口、XML 映射文件
- **pipeline-server-facade**: 对外接口层 - API 接口定义、DTO 对象、Feign 客户端接口
- **pipeline-server-service**: 业务层 - Controller、Service 实现、启动类、配置文件

## Module Dependencies

```
service → dao → common
service → facade → common
```

## Package Structure

```
com.ci.pipeline
├── common
│   ├── constants      # 常量定义
│   ├── exception      # 异常类和全局异常处理
│   └── result         # 统一返回结果封装
├── dao
│   ├── config         # MyBatis-Plus 配置
│   ├── entity         # 数据库实体类
│   └── mapper         # Mapper 接口
├── facade
│   ├── api            # 对外 API 接口定义
│   └── dto            # 数据传输对象
└── service
    ├── bootstrap      # 启动类
    ├── controller     # REST 控制器
    ├── service        # 业务逻辑服务
    └── remote         # Feign 远程调用客户端
```

## Build & Run

```bash
# 编译
mvn clean compile

# 打包
mvn clean package -DskipTests

# 运行
java -jar pipeline-server-service/target/pipeline-server-service-1.0.0.jar

# 指定环境运行
java -jar pipeline-server-service/target/pipeline-server-service-1.0.0.jar --spring.profiles.active=local
```

## Key Conventions

- All REST APIs return `Result<T>` wrapper
- Use Lombok annotations (`@Data`, `@Slf4j`, etc.) to reduce boilerplate
- MyBatis-Plus for ORM, mapper XML files under `pipeline-server-dao/src/main/resources/mapper/`
- Database table naming: snake_case; Entity field naming: camelCase (auto-mapped by MyBatis-Plus)
- API interfaces defined in facade module, implemented in service module

## 编码规范 (Coding Standards)

### 常量定义规范 (Constants)

- **禁止在业务类中直接定义常量。** Controller / Service / 等业务类中不允许出现业务相关的 `static final` 常量（如固定的 namespace、container 名称、状态码字符串、魔法值等）。业务类只负责流程编排，常量值必须集中管理。
- **统一放置到 `com.ci.pipeline.common.constants` 包下的常量类中。** 该包位于 `pipeline-server-common` 模块，所有模块均可依赖（`service → dao → common`，`service → facade → common`）。
- **按业务域分文件归类**，不要把所有常量堆进一个文件。新增域内常量时优先复用已有同名常量类，例如：
  - `CommonConstants` — 通用常量（应用名、状态码、分页默认值等）
  - `KubernetesConstants` — Kubernetes / Argo 相关常量（如 `ARGO_NAMESPACE = "argo"`、`DEFAULT_LOG_CONTAINER = "main"`）
- **常量类本身需遵循固定写法：**
  - 声明为 `final class`，禁止被继承；
  - 提供私有无参构造函数 `private XxxConstants() {}`，禁止实例化；
  - 字段统一使用 `public static final`，命名采用全大写 + 下划线（`UPPER_SNAKE_CASE`）；
  - 每个常量补充 Javadoc 注释说明含义。

**示例：**

```java
// ❌ 禁止：在 DemoController 这类业务类中直接写常量
public class DemoController {
    private static final String POD_LOG_NAMESPACE = "argo";
    private static final String POD_LOG_CONTAINER = "main";
}

// ✅ 正确：放到 constants 包下的常量类
public final class KubernetesConstants {
    private KubernetesConstants() {}

    /** Argo Workflow 所在命名空间 */
    public static final String ARGO_NAMESPACE = "argo";
    /** Pod 默认读取日志的容器名称 */
    public static final String DEFAULT_LOG_CONTAINER = "main";
}
```

业务类中通过静态引用使用：`KubernetesConstants.ARGO_NAMESPACE`。

## Health Check

- `GET /admin/health` - 健康检查接口
- `GET /admin/info` - 应用信息接口
- Actuator: `GET /actuator/health`

## TODO

- [~] 添加详细的编码规范约束（已完成「常量定义规范」，其余待补充）
- [ ] 添加 Git 提交规范
- [ ] 添加 API 文档规范
- [ ] 添加单元测试规范
