# Argo WorkflowTemplate 更新报 resourceVersion 缺失排查与修复

> 问题：调用 `ArgoWorkflowAgentImpl.updateWorkflowTemplate(...)` 更新已存在的 WorkflowTemplate（如
> `git-sync`）时，返回：
>
> ```
> workflowtemplates.argoproj.io "git-sync" is invalid:
> metadata.resourceVersion: Invalid value: 0x0: must be specified for an update
> ```

## 1. 背景

任务模板版本在「发布」（`changeStatus` 置为生效中）时会调用 `ArgoWorkflowAgent.saveWorkflowTemplate`：
若 argo 中已存在同名模板则走更新分支，最终落到 `updateWorkflowTemplate`，内部通过 SDK 调用：

```java
workflowTemplateServiceApi.workflowTemplateServiceUpdateWorkflowTemplate(namespace, name, updateRequest);
```

其中 `updateRequest.template` 来自用户提交的 `templateDetail`（一段 Argo WorkflowTemplate 的 JSON）。
被更新的模板（如 `git-sync`）在 argo 中确实已存在，期望走更新，但调用直接被服务端拒绝。

## 2. 现象

更新已存在模板时，接口返回错误，关键信息：

```json
{
  "code": 3,
  "message": "workflowtemplates.argoproj.io \"git-sync\" is invalid: metadata.resourceVersion: Invalid value: 0x0: must be specified for an update"
}
```

- 错误由 **Kubernetes API Server** 在准入校验阶段抛出（不是 argo 自身逻辑），HTTP 已到达 apiserver。
- 字段是 `metadata.resourceVersion`，值为 `0x0`（即空 / 缺失），提示 `must be specified for an update`。
- 仅 **更新**路径报错；创建（`createWorkflowTemplate`）不涉及，因为 create 不要求 resourceVersion。

## 3. 排查过程

### 3.1 确认这是 Kubernetes 的硬性要求

`resourceVersion` 是 K8s 资源的**乐观并发控制（optimistic concurrency）**令牌，由服务端分配、不透明。
对已存在资源执行 **Update（HTTP PUT，全量替换）** 时，apiserver 要求请求体的 `metadata.resourceVersion`
必须存在且等于当前值；执行 **Patch** 时则不要求。

`workflowTemplateServiceUpdateWorkflowTemplate` 走的是 Update 语义，因此必须带上 resourceVersion。

### 3.2 确认请求里确实没有 resourceVersion

请求体里的 `template` 来源于用户提交的 `templateDetail` JSON。该 JSON 是「期望的模板定义」，
通常只包含 `apiVersion / kind / metadata.name / spec` 等，**不会**带 `resourceVersion`——
因为它是服务端运行期分配的字段，客户端不应手填。

因此 updateRequest 里的 `template.metadata.resourceVersion` 为空，到达 apiserver 后被校验拒绝，
表现为 `Invalid value: 0x0`。

### 3.3 确认 argo 不会自动补 resourceVersion

argoproj 的 `UpdateWorkflowTemplate` 在当前使用版本下，会把请求中的 template 对象转发给 K8s 做 Update，
**不会**自动补齐 resourceVersion。这与现象一致：客户端不传，则请求里就没有，apiserver 直接拒绝。

> 结论：客户端必须自己保证 resourceVersion 存在。这是 K8s/argo SDK 更新资源的标准用法——
> **先 GET 拿到当前 resourceVersion，回填后再 Update**（即 get-modify-update 模式）。

## 4. 根因

`updateWorkflowTemplate` 直接拿用户提交的 `templateDetail`（不含 `resourceVersion`）去发起 Update，
没有在更新前回填服务端的 `resourceVersion`，导致 K8s 校验失败：

```
metadata.resourceVersion: must be specified for an update
```

## 5. 解决方案

在 `updateWorkflowTemplate` 发起 Update 之前，先 `getWorkflowTemplate` 查回已存在模板，
取其 `metadata.resourceVersion` 回填到待提交的 template 上，再调用 update。

### 5.1 改动位置

`pipeline-server-service/src/main/java/com/ci/pipeline/service/remote/impl/ArgoWorkflowAgentImpl.java`
的 `updateWorkflowTemplate` 方法，在构建 `IoArgoprojWorkflowV1alpha1WorkflowTemplateUpdateRequest` 之前插入回填逻辑：

```java
// argo/K8s 的 Update（PUT）要求 metadata.resourceVersion（乐观锁），
// 用户传入的模板 JSON 不含该字段（服务端分配），故先查询已存在模板取其 resourceVersion 回填，
// 否则会报 "metadata.resourceVersion: must be specified for an update"
IoArgoprojWorkflowV1alpha1WorkflowTemplate existing = getWorkflowTemplate(namespace, name);
V1ObjectMeta meta = workflowTemplate.getMetadata();
if (meta == null) {
    meta = new V1ObjectMeta();
    workflowTemplate.setMetadata(meta);
}
if (existing.getMetadata() != null) {
    meta.setResourceVersion(existing.getMetadata().getResourceVersion());
}
```

### 5.2 为什么先 GET 是正确的

- `resourceVersion` 是服务端不透明令牌，客户端无法凭空构造，必须从一个有效来源获取；
- 对已存在模板 GET 一次，既能拿到当前 `resourceVersion`，又顺带确认模板确实存在；
- 回填后发起的 Update 满足 apiserver 的乐观锁校验。

### 5.3 与 saveWorkflowTemplate 的关系

`saveWorkflowTemplate` 内部已经通过一次 `getWorkflowTemplate` 来探测存在性；走更新分支时
`updateWorkflowTemplate` 又会 GET 一次取 resourceVersion，因此更新路径整体会 GET 两次。
GET 是只读且轻量，为保持 `updateWorkflowTemplate` 自洽（单独调用也正确）暂未合并；
若后续对调用次数敏感，可把 `saveWorkflowTemplate` 探测拿到的 existing 透传给 update 复用。

## 6. 验证

编译通过：

```
mvn -pl pipeline-server-service -am compile
```

修复后重新发布更新 `git-sync`：update 请求带上回填的 `resourceVersion`，apiserver 校验通过，
不再报 `must be specified for an update`，模板 spec 被正确更新。

## 7. 要点小结

- 看到 `metadata.resourceVersion: ... must be specified for an update`，根因一定是
  **更新请求里缺 resourceVersion**，而 resourceVersion 只能从服务端 GET 得到，不能手填。
- K8s 的 Update（PUT）要求 resourceVersion（乐观锁），Patch 不要求；argo 的
  `workflowTemplateServiceUpdateWorkflowTemplate` 属于 Update 语义。
- 正确的更新姿势是 **get-modify-update**：先 GET 取 resourceVersion → 回填到待提交对象 → 再 Update。
  这同样适用于其它 K8s / argo 资源的更新接口（如 `Workflow`、`CronWorkflow` 等）。
- resourceVersion 是乐观锁：若回填后在发起 Update 之间该模板被他人改动，apiserver 会返回 409 Conflict，
  此时重试（重新 GET 再更新）即可，属预期行为。
- 客户端模板 JSON 只描述期望状态（apiVersion/kind/metadata.name/spec 等），不要在里面写
  `resourceVersion` / `uid` / `creationTimestamp` 等服务端字段——这些应由 GET 回填或交给服务端管理。

## 8. 涉及文件

| 文件 | 说明 |
| --- | --- |
| `pipeline-server-service/.../remote/impl/ArgoWorkflowAgentImpl.java` | `updateWorkflowTemplate`：更新前回填 resourceVersion（本次修复） |
| `pipeline-server-service/.../remote/ArgoWorkflowAgent.java` | `updateWorkflowTemplate` / `saveWorkflowTemplate` 接口声明 |
| `pipeline-server-service/.../service/impl/TaskTemplateVersionServiceImpl.java` | 发布（`changeStatus`）时调用 `saveWorkflowTemplate`，间接触发更新 |
