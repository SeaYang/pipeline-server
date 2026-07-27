# GitLab API 集成技术设计

> 文档版本：v1.1  
> 创建日期：2026-07-27  
> 模块：pipeline-server

---

## 1. 背景与目标

### 1.1 背景

pipeline-server 目前缺乏对 GitLab API 的访问能力。在流水线编排场景中，多处需要与 GitLab 交互，例如：

- 根据应用绑定的 `gitSshUrl` 查询仓库信息（repoId、默认分支等）
- 查询仓库分支列表，供用户选择构建分支
- 浏览仓库目录树（选择构建脚本 / Dockerfile 路径）
- 查询分支 / commit 信息（回填执行记录）

### 1.2 目标

引入 **GitLab4J-API**（`org.gitlab4j:gitlab4j-api`）SDK，封装统一的 GitLab 访问 Agent，并将 **repoId 落地到 `app_info` 表**，后续分支查询、目录树查询均通过 repoId 直接访问，无需重复解析 gitUrl。

本期实现：

| # | 能力 | 说明 |
|---|------|------|
| 1 | app_info 新增 repo_id 字段 | 创建/修改应用时，根据 gitSshUrl 查询 GitLab repoId 并保存 |
| 2 | 查询分支列表 | 前端传 appName → 反查 repoId → 查询仓库全部分支 |
| 3 | 查询目录树 | 前端传 appName + path → 反查 repoId → 懒加载单层目录 |

### 1.3 非目标

- 本期不实现 commit 详情查询、文件内容读取、MR / Webhook 等能力（后续按需扩展）
- 不支持 HTTPS 格式的 gitUrl（仅 SSH）
- 不做多 token / 多用户隔离（全局单一 access token）

---

## 2. 技术选型

### 2.1 SDK 对比

| 方案 | Maven 坐标 | 结论 |
|------|-----------|------|
| **GitLab4J-API** ✅ | `org.gitlab4j:gitlab4j-api:5.8.0` | 活跃维护，API 全面（Project / Repository / Branch / Commit / Tree），封装友好 |
| GitLab Java API Client | `org.gitlab:java-gitlab-api` | 基本停更，功能不全，不采用 |
| 自行 HTTP 调用 | 无 | 需自行管理请求 / 响应类与 URL，维护成本高，不采用 |

### 2.2 选型结论

采用 **GitLab4J-API**，理由：

- 无需自行定义各 API 的请求 / 响应模型类
- 无需手动拼接 URL，SDK 内部管理
- 与项目现有 Argo SDK（`argo-client-java`）、K8s SDK（`kubernetes-client`）的 Agent 封装模式一致

---

## 3. 配置管理

### 3.1 复用 GenericConfig 配置中心

GitLab 的域名和 access token 通过系统已有的 **GenericConfig（KV 配置中心）** 管理，不写死在 `application.yml` 中。变更时直接修改配置即可，无需重启。

| configKey | valueFormat | 说明 | 示例值 |
|-----------|-------------|------|--------|
| `gitlab.api.url` | txt | GitLab 实例地址 | `https://gitlab.com` |
| `gitlab.api.token` | txt | 全局 access token | `glpat-xxxxxxxxxxxxxxxxxxxx` |

### 3.2 配置读取方式

```java
@Autowired
private GenericConfigService genericConfigService;

String apiUrl = (String) genericConfigService.getValueByKey(GitLabConstants.CONFIG_KEY_API_URL);
String token  = (String) genericConfigService.getValueByKey(GitLabConstants.CONFIG_KEY_API_TOKEN);
```

> `getValueByKey` 对 `txt` 格式直接返回 `String`，对 `json` 格式返回解析后的 `JSONObject/JSONArray`。GitLab 配置均为 `txt` 格式。

---

## 4. 整体架构

遵循项目现有分层约定：

```
facade（DTO）
  ├── request
  │    └── GitTreeQueryRequest        # 目录树查询请求（appName + path）
  └── response
       ├── GitBranchResponse          # 分支信息
       └── GitTreeNodeResponse        # 目录树节点

service
  ├── controller
  │    ├── AppInfoController          # （已有）create/update 时查 repoId 并保存
  │    └── GitLabController           # （新增）REST 入口 /gitlab
  ├── service
  │    ├── AppInfoService             # （已有）create/update 逻辑调整
  │    ├── GitLabService              # （新增）业务接口
  │    └── impl/GitLabServiceImpl     # （新增）appName→repoId 反查 + 调 Agent
  └── remote
       ├── GitLabAgent                # （新增）GitLab SDK 封装接口
       └── impl/GitLabAgentImpl       # （新增）GitLab4J-API 实现

common
  └── constants
       └── GitLabConstants            # （新增）GitLab 相关常量 + 配置 key

dao
  ├── entity/AppInfo                  # （已有）新增 repoId 字段
  └── ...                             # Mapper XML 无需改动（MyBatis-Plus 自动映射）
```

### 4.1 调用链路

**应用创建/修改时（repoId 落库）：**
```
AppInfoController.create/update
    └── AppInfoServiceImpl
          ├── 校验 gitSshUrl SSH 格式
          ├── gitLabService.getRepoId(gitUrl)   # 解析 URL → 查 GitLab → 拿 repoId
          └── 保存 repoId 到 app_info 表
```

**查询分支/目录树时（通过 appName 反查 repoId）：**
```
GitLabController.listBranches/listTree
    └── GitLabServiceImpl
          ├── appInfoRepository.selectByAppName(appName)  # 反查 repoId
          └── gitLabAgent.getBranches(repoId) / getRepositoryTree(repoId, path)
```

---

## 5. 详细设计

### 5.1 app_info 表新增 repo_id 字段

#### 5.1.1 DDL 变更

```sql
ALTER TABLE `app_info`
  ADD COLUMN `repo_id` bigint DEFAULT NULL COMMENT 'GitLab仓库ID' AFTER `git_ssh_url`;
```

> `repo_id` 允许 NULL：历史数据没有该字段值，新建/修改应用时回填。

#### 5.1.2 实体 / DTO 变更

| 类 | 变更 |
|----|------|
| `AppInfo` 实体 | 新增 `private Long repoId;` |
| `AppInfoCreateRequest` | 不新增字段（repoId 由后端自动查询填充，前端不传） |
| `AppInfoUpdateRequest` | 不新增字段（同上） |
| `AppInfoResponse` | 新增 `private Long repoId;`（列表/详情展示） |

### 5.2 GitLabAgent（SDK 封装层）

#### 5.2.1 设计要点

- **不使用 `@PostConstruct` 固定初始化客户端**。因为域名和 token 来自 GenericConfig（DB），可能随时变更，所以每次调用时实时读取配置、创建 `GitLabApi` 实例。
- `GitLabApi` 实例轻量，每次创建开销可忽略。
- 统一捕获 `GitLabApiException`，转抛 `BusinessException`，与项目异常处理模式一致。

#### 5.2.2 接口定义

```java
public interface GitLabAgent {

    /**
     * 根据 namespace/project 路径查询仓库信息。
     * 用于应用创建/修改时，根据 gitSshUrl 解析出的 projectPath 查询 repoId。
     *
     * @param projectPath namespace/project，如 "SeaYang2/go-web-demo"
     * @return GitLab Project 对象（含 id、name、webUrl 等）
     */
    Project getProject(String projectPath);

    /**
     * 根据 repoId 查询仓库全部分支（含最近 commit）。
     *
     * @param repoId GitLab 仓库数字 ID（从 app_info 表获取）
     */
    List<Branch> getBranches(Long repoId);

    /**
     * 根据 repoId 查询指定路径的单层目录树（懒加载）。
     *
     * @param repoId GitLab 仓库数字 ID（从 app_info 表获取）
     * @param path   查询路径，空串或 null 表示根目录
     */
    List<TreeItem> getRepositoryTree(Long repoId, String path);
}
```

> **参数设计说明**：三个方法的入参都是精确的标识——`getProject` 传 projectPath（用于首次查 repoId），`getBranches` 和 `getRepositoryTree` 传 repoId（从 app_info 表获取，不再需要解析 gitUrl）。

#### 5.2.3 客户端创建

```java
private GitLabApi createClient() {
    String url   = (String) genericConfigService.getValueByKey(GitLabConstants.CONFIG_KEY_API_URL);
    String token = (String) genericConfigService.getValueByKey(GitLabConstants.CONFIG_KEY_API_TOKEN);
    return new GitLabApi(url, token);
}
```

### 5.3 GitLabService（业务层）

#### 5.3.1 SSH URL 解析

输入格式：`git@gitlab.com:SeaYang2/go-web-demo.git`

解析规则（正则 `^git@([^:]+):(.+?)(\.git)?$`）：

| 分组 | 含义 | 示例 |
|------|------|------|
| group 1 | host（校验用） | `gitlab.com` |
| group 2 | namespace/project（去掉 `.git` 后缀） | `SeaYang2/go-web-demo` |

解析失败时抛 `BusinessException(GitLabConstants.MSG_INVALID_GIT_SSH_URL)`。

> 该解析逻辑放在 `GitLabServiceImpl` 中，作为 `getRepoId(gitUrl)` 的内部步骤。

#### 5.3.2 方法设计

| 方法 | 入参 | 出参 | 说明 |
|------|------|------|------|
| `getRepoId` | `gitUrl` | `Long` | 解析 SSH URL → `agent.getProject(path)` → 返回 repoId。供 AppInfoServiceImpl 调用 |
| `listBranches` | `appName` | `List<GitBranchResponse>` | `appInfoRepository.selectByAppName(appName)` 取 repoId → `agent.getBranches(repoId)` |
| `listTree` | `GitTreeQueryRequest` | `List<GitTreeNodeResponse>` | 取 appName 对应 repoId → `agent.getRepositoryTree(repoId, path)` |

### 5.4 AppInfoServiceImpl 逻辑调整

#### 5.4.1 create 方法

```
1. 校验必填字段（appName / programmingLanguage / gitSshUrl）
2. 校验 appName 唯一性
3. 【新增】校验 gitSshUrl 的 SSH 格式
4. 【新增】调用 gitLabService.getRepoId(gitSshUrl) 查询 repoId
5. 保存实体（含 repoId）
```

#### 5.4.2 update 方法

```
1. 校验 id 必填、应用存在
2. 校验 appName（传入时非空 + 唯一性）
3. 【新增】如果请求中传了 gitSshUrl：
   a. 校验 SSH 格式
   b. 调用 gitLabService.getRepoId(gitSshUrl) 查询 repoId
   c. 一并更新 repoId 字段
4. 更新实体
```

> **注意**：update 时只有 gitSshUrl 发生变更才需要重新查 repoId。如果 gitSshUrl 没变（等于原值），可跳过查询。但为简化实现，本期只要传了 gitSshUrl 就重新查询（GitLab API 调用很快，可接受）。

#### 5.4.3 page 方法

无需逻辑调整——`AppInfoResponse` 新增 `repoId` 字段后，`BeanUtils.copyProperties` 自动映射，分页结果自动包含 repoId。

### 5.5 GitLabController（接口层）

| HTTP | 路径 | 方法 | 入参 | 说明 |
|------|------|------|------|------|
| GET | `/gitlab/branches` | `listBranches` | `appName` | 根据 appName 反查 repoId，返回分支列表 |
| GET | `/gitlab/tree` | `listTree` | `appName` + `path` | 根据 appName 反查 repoId，返回目录树 |

> **不提供 `/gitlab/repo` 接口**——仓库信息查询（getProject）仅在应用创建/修改时由后端内部调用，用于回填 repoId，前端不需要单独调用。

统一返回 `Result<T>`，加 `@RequireLogin`。

---

## 6. 数据模型（DTO）

### 6.1 GitBranchResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 分支名 |
| commitId | String | 最近 commit SHA |
| commitMessage | String | 最近 commit message |
| authorName | String | commit 作者 |
| committedDate | Date | commit 时间 |

### 6.2 GitTreeNodeResponse

| 字段 | 类型 | 说明 |
|------|------|------|
| name | String | 文件 / 目录名 |
| path | String | 完整路径 |
| type | String | `tree`（目录）/ `blob`（文件） |
| mode | String | 文件模式 |

### 6.3 GitTreeQueryRequest

| 字段 | 类型 | 说明 |
|------|------|------|
| appName | String | 应用名称（反查 repoId） |
| path | String | 查询路径，空串或 null 表示根目录 |

---

## 7. 常量定义

新增 `GitLabConstants`（`pipeline-server-common`），遵循项目常量规范（`final class` + 私有构造 + `public static final`）：

```java
public final class GitLabConstants {
    private GitLabConstants() {}

    /** GenericConfig 配置 key：GitLab API 地址 */
    public static final String CONFIG_KEY_API_URL = "gitlab.api.url";
    /** GenericConfig 配置 key：GitLab access token */
    public static final String CONFIG_KEY_API_TOKEN = "gitlab.api.token";

    /** SSH URL 解析正则 */
    public static final String GIT_SSH_URL_REGEX = "^git@([^:]+):(.+?)(\\.git)?$";

    /** 目录树节点类型：目录 */
    public static final String TREE_TYPE_TREE = "tree";
    /** 目录树节点类型：文件 */
    public static final String TREE_TYPE_BLOB = "blob";

    /** git 仓库地址格式不正确 */
    public static final String MSG_INVALID_GIT_SSH_URL = "git仓库地址格式不正确，仅支持SSH格式";
    /** 查询 GitLab 仓库信息失败 */
    public static final String MSG_GET_PROJECT_FAILED = "查询GitLab仓库信息失败：%s";
    /** 查询分支列表失败 */
    public static final String MSG_GET_BRANCHES_FAILED = "查询分支列表失败：%s";
    /** 查询目录树失败 */
    public static final String MSG_GET_TREE_FAILED = "查询目录树失败：%s";
    /** 应用未配置 GitLab 仓库（repoId 为空） */
    public static final String MSG_REPO_ID_NOT_FOUND = "应用[%s]未配置GitLab仓库";
}
```

---

## 8. API 示例

### 8.1 查询分支列表

```
GET /gitlab/branches?appName=go-web-demo
```

响应：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "name": "main",
      "commitId": "a1b2c3d4e5f6...",
      "commitMessage": "feat: init project",
      "authorName": "SeaYang2",
      "committedDate": "2026-07-20 10:30:00"
    }
  ]
}
```

### 8.2 查询目录树（懒加载）

```
GET /gitlab/tree?appName=go-web-demo&path=
```

响应（根目录）：
```json
{
  "code": 200,
  "message": "success",
  "data": [
    { "name": "src", "path": "src", "type": "tree", "mode": "040000" },
    { "name": ".gitignore", "path": ".gitignore", "type": "blob", "mode": "100644" },
    { "name": "go.mod", "path": "go.mod", "type": "blob", "mode": "100644" },
    { "name": "README.md", "path": "README.md", "type": "blob", "mode": "100644" }
  ]
}
```

前端点击 `src` 目录后，请求下一层：
```
GET /gitlab/tree?appName=go-web-demo&path=src
```

### 8.3 应用创建（repoId 自动回填）

```
POST /app-info
{
  "appName": "go-web-demo",
  "programmingLanguage": "go",
  "gitSshUrl": "git@gitlab.com:SeaYang2/go-web-demo.git"
}
```

响应（repoId 由后端自动查询填充）：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "appName": "go-web-demo",
    "programmingLanguage": "go",
    "gitSshUrl": "git@gitlab.com:SeaYang2/go-web-demo.git",
    "repoId": 84836257,
    "createTime": "2026-07-27 15:00:00",
    "updateTime": "2026-07-27 15:00:00"
  }
}
```

---

## 9. 前置准备

### 9.1 GenericConfig 初始化数据

在 `generic_config` 表中插入两条配置（通过 GenericConfig 管理界面或 SQL）：

```sql
INSERT INTO generic_config (config_key, config_value, value_format, description, creator)
VALUES
  ('gitlab.api.url',   'https://gitlab.com', 'txt', 'GitLab API 地址', 'system'),
  ('gitlab.api.token', 'glpat-xxxxxxxxxxxx', 'txt', 'GitLab access token', 'system');
```

### 9.2 app_info 表 DDL 变更

```sql
ALTER TABLE `app_info`
  ADD COLUMN `repo_id` bigint DEFAULT NULL COMMENT 'GitLab仓库ID' AFTER `git_ssh_url`;
```

### 9.3 Maven 依赖

在 `pipeline-server-service/pom.xml` 中新增：

```xml
<dependency>
    <groupId>org.gitlab4j</groupId>
    <artifactId>gitlab4j-api</artifactId>
    <version>5.8.0</version>
</dependency>
```

---

## 10. 变更清单

| # | 文件 | 变更类型 | 说明 |
|---|------|----------|------|
| 1 | `pom.xml`（service） | 修改 | 新增 gitlab4j-api 依赖 |
| 2 | `GitLabConstants`（common） | 新增 | 配置 key、正则、提示信息 |
| 3 | `AppInfo` 实体（dao） | 修改 | 新增 repoId 字段 |
| 4 | `AppInfoResponse`（facade） | 修改 | 新增 repoId 字段 |
| 5 | `GitBranchResponse`（facade） | 新增 | 分支列表响应 DTO |
| 6 | `GitTreeNodeResponse`（facade） | 新增 | 目录树节点 DTO |
| 7 | `GitTreeQueryRequest`（facade） | 新增 | 目录树查询请求 DTO |
| 8 | `GitLabAgent` + `GitLabAgentImpl`（service/remote） | 新增 | GitLab SDK 封装 |
| 9 | `GitLabService` + `GitLabServiceImpl`（service） | 新增 | appName→repoId 反查 + 业务逻辑 |
| 10 | `GitLabController`（service/controller） | 新增 | REST 接口 /gitlab |
| 11 | `AppInfoServiceImpl`（service） | 修改 | create/update 时查 repoId 并保存 |
| 12 | `sql/app_info.sql` | 修改 | 新增 repo_id 列定义 |

---

## 11. 后续扩展方向

本期实现核心 API，后续可按需扩展：

- 查询 commit 详情 / commit diff
- 读取文件内容（`RepositoryApi.getFile`）
- 查询 Tag 列表
- Webhook 管理（推送事件触发流水线）
- 多 GitLab 实例支持（按应用绑定不同实例的配置 key）
