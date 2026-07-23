# Argo Workflow 实例查询反序列化失败排查与修复

> 问题：调用 `ArgoWorkflowAgent.getWorkflow(...)` 时，`workflowServiceGetWorkflow` 抛出
> `java.lang.IllegalStateException: Expected BEGIN_OBJECT but was STRING at line 1 column 1856 path $.status.startedAt`

## 1. 背景

在 `ArgoWorkflowAgent` / `ArgoWorkflowAgentImpl` 中新增了对 Argo Workflow 实例的查询与运维方法，其中
`getWorkflow(namespace, name)` 通过 SDK 的 `WorkflowServiceApi` 查询单个 Workflow 实例：

```java
IoArgoprojWorkflowV1alpha1Workflow result =
        workflowServiceApi.workflowServiceGetWorkflow(namespace, name, null, null);
```

项目使用的 SDK 为 `io.argoproj.workflow:argo-client-java:v3.4.8`（见 `pipeline-server-service/pom.xml`）。

## 2. 现象

运行时调用 `getWorkflow` 直接抛出异常，关键信息：

```
java.lang.IllegalStateException: Expected BEGIN_OBJECT but was STRING at line 1 column 1856 path $.status.startedAt
```

- 报错发生在 **响应体反序列化阶段**，而不是 HTTP 调用本身（请求已成功返回 200，JSON 体也已拿到）。
- Gson 在解析 JSON 路径 `$.status.startedAt` 时，期望读到的是一个 JSON **对象**（`{`），
  实际却是一个 JSON **字符串**（`"2024-06-13T08:30:00Z"`）。

## 3. 排查过程

### 3.1 定位出错字段

错误路径 `$.status.startedAt` 指向 Workflow `status` 下的 `startedAt` 字段。从 SDK 源码
（`argo-client-java-v3.4.8-sources.jar`）中查看模型类：

```
io/argoproj/workflow/models/IoArgoprojWorkflowV1alpha1WorkflowStatus.java
```

```java
public static final String SERIALIZED_NAME_STARTED_AT = "startedAt";
private java.time.Instant startedAt;

public static final String SERIALIZED_NAME_FINISHED_AT = "finishedAt";
private java.time.Instant finishedAt;
```

即 `startedAt` / `finishedAt` 在 Java 模型中被声明为 **`java.time.Instant`**。

### 3.2 确认 Argo 实际返回的格式

Argo 中 `metav1.Time` 字段序列化为 RFC3339 的 **字符串**，例如：

```json
{
  "status": {
    "phase": "Succeeded",
    "startedAt": "2024-06-13T08:30:00Z",
    "finishedAt": "2024-06-13T08:31:05Z"
  }
}
```

所以实际 JSON 是字符串，而 Java 模型期望 `Instant`。

### 3.3 确认 Gson 的处理方式

查看 SDK 的 `io.argoproj.workflow.JSON` 类（负责构造 Gson）：

```java
public JSON() {
    gson = ... createGson() ...
        .registerTypeAdapter(Date.class, dateTypeAdapter)
        .registerTypeAdapter(java.sql.Date.class, sqlDateTypeAdapter)
        .registerTypeAdapter(OffsetDateTime.class, offsetDateTimeTypeAdapter)
        .registerTypeAdapter(LocalDate.class, localDateTypeAdapter)
        .registerTypeAdapter(byte[].class, byteArrayAdapter)
        ...create();
}
```

可以看到 SDK 为 `Date`、`OffsetDateTime`、`LocalDate`、`byte[]` 都注册了 `TypeAdapter`，
**唯独没有注册 `java.time.Instant`**。

Gson 对没有注册适配器的类型，会退回到 `ReflectiveTypeAdapterFactory` 用反射去解析：
把目标类型当作一个普通对象，期望从 JSON 里读取它的字段（即 `BEGIN_OBJECT`）。
而 `Instant` 是 final 且无公开无参构造，反射解析时遇到字符串就会抛出：

```
Expected BEGIN_OBJECT but was STRING at $.status.startedAt
```

这与现象完全吻合。

## 4. 根因

`argo-client-java:v3.4.8` 的 `JSON` 类**未给 `java.time.Instant` 注册 Gson 适配器**。
当 Workflow 响应中包含 `startedAt` / `finishedAt` 等 `Instant` 字段时，Gson 走反射解析，
把字符串误当成对象去读，导致反序列化失败。

受影响的字段不止工作流级别的 `status.startedAt / status.finishedAt`，
节点级 `status.nodes[*].startedAt / finishedAt` 同样是 `Instant` 类型，都会触发同样的问题。

## 5. 解决方案

在 `ApiClient` 的 Gson 上补注册一个 `Instant` 的 `TypeAdapter`，负责字符串 ↔ `Instant` 转换。

### 5.1 改动位置

`pipeline-server-service/src/main/java/com/ci/pipeline/service/config/ArgoClientConfig.java`

`ApiClient` Bean 创建过程中，在 `Configuration.setDefaultApiClient(apiClient)` 之前补注册适配器：

```java
// 注册 Instant 类型适配器：Argo 返回的 status.startedAt / finishedAt 等 ISO 字符串
// 对应 SDK 中的 java.time.Instant 字段，而 argo-client-java 默认未注册该类型的适配器，
// 会导致反序列化报错 "Expected BEGIN_OBJECT but was STRING"。
registerInstantTypeAdapter(apiClient);
```

### 5.2 注册逻辑

通过 `Gson.newBuilder()` 在**保留 SDK 已有适配器**（Date / OffsetDateTime / LocalDate / byte[]）的基础上追加，
避免破坏现有的序列化行为：

```java
private void registerInstantTypeAdapter(ApiClient apiClient) {
    JSON json = apiClient.getJSON();
    Gson gson = json.getGson().newBuilder()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .create();
    json.setGson(gson);
}
```

### 5.3 适配器实现

`read` 将 RFC3339 字符串解析为 `Instant`；解析失败返回 `null`（与 SDK 其它时间适配器的容错风格一致），
并兼容极少数返回对象形式的字段（跳过避免整体反序列化失败）。`write` 序列化回 ISO 字符串。

```java
private static class InstantTypeAdapter extends TypeAdapter<Instant> {

    @Override
    public void write(JsonWriter out, Instant value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.toString());
        }
    }

    @Override
    public Instant read(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        if (token == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        // 兼容极少数返回对象形式的情况，跳过避免反序列化失败
        if (token == JsonToken.BEGIN_OBJECT) {
            in.skipValue();
            return null;
        }
        String value = in.nextString();
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            log.warn("解析 Instant 失败, value={}", value, e);
            return null;
        }
    }
}
```

### 5.4 作用范围

适配器注册在 `ApiClient` 上，是**全局**生效的，因此 `getWorkflow`、`listWorkflows` 以及
所有返回 `Workflow` / `WorkflowList` 的方法都受益（节点级时间字段同样是 `Instant`）。

## 6. 验证

编译通过：

```
mvn -pl pipeline-server-service -am compile -Dmaven.repo.local=<local-repo>
```

修复后重新调用 `getWorkflow`，Workflow 对象可正常反序列化返回，不再抛出
`Expected BEGIN_OBJECT but was STRING`。

## 7. 要点小结

- 看到 `Expected BEGIN_OBJECT but was STRING at $.<path>` 这类 Gson 错误，**根因几乎都是目标字段缺类型适配器**，
  Gson 退回反射把字符串当对象读。
- 排查思路：用错误中的 JSON `path` 去 SDK 模型源码里定位字段类型，再确认 SDK 的 `JSON` 类是否为该类型注册了适配器。
- `argo-client-java:v3.4.8` 漏注册了 `java.time.Instant`，遇到含 `startedAt / finishedAt` 的 Workflow 响应必现。
- 修复用 `gson.newBuilder()` 追加适配器，不要重建 Gson，以免丢失 SDK 已有的 `Date / OffsetDateTime` 等适配器。
- 如果后续再出现类似 `Expected ... but was ...` 的反序列化错误，多半是又有一个类型缺适配器，
  用同样的方法按 `path` 定位并补注册即可。

## 8. 涉及文件

| 文件 | 说明 |
| --- | --- |
| `pipeline-server-service/pom.xml` | 声明 `argo-client-java:v3.4.8` 依赖 |
| `pipeline-server-service/.../config/ArgoClientConfig.java` | 注册 `Instant` 适配器（本次修复） |
| `pipeline-server-service/.../remote/ArgoWorkflowAgent.java` | `getWorkflow` 等接口声明 |
| `pipeline-server-service/.../remote/impl/ArgoWorkflowAgentImpl.java` | `getWorkflow` 等实现，调用 `workflowServiceGetWorkflow` |
