# 流水线触发历史记录 - 技术设计方案

## 一、背景与目标

### 1.1 现状

流水线执行是平台的核心功能，目前支持两种触发方式：

1. **用户手动触发**：前端页面填写参数后，调用 `POST /pipeline/execute` 执行流水线。
2. **事件触发**：第三方系统通过 `POST /pipeline/event/trigger` 接口，以事件驱动方式自动触发流水线。

两种触发方式底层都调用 `PipelineServiceImpl.execute` 方法。虽然代码执行时有日志记录，但日志分散、检索困难，且无法结构化地回答"谁在什么时候触发了哪条流水线、结果如何"这类问题。

### 1.2 目标

引入**触发历史记录**机制，在两个触发入口统一记录每次触发的完整上下文：

1. **全量记录**：无论触发成功还是失败，都记录一条触发历史。
2. **结构化存储**：将触发人、触发类型、请求参数、执行结果、失败原因等信息持久化到数据库表，便于检索和排查。
3. **查询能力**：提供前端查询页面，支持按 pipeline 维度和 appName 维度查看触发历史列表及详情。

### 1.3 非目标（本期不实现）

- 触发历史的导出功能。
- 触发历史的统计报表和可视化图表。
- 触发历史的批量删除和清理策略。

---

## 二、整体架构

### 2.1 分层职责

```
┌──────────────────────────────────────────────────────────┐
│  前端 (pipeline-frontend)                                 │
│  ├── pipeline 列表 → 操作列 → 「触发历史」按钮             │
│  │    └── 按 pipeline_id 查看触发历史列表 + 详情           │
│  └── 流水线列表右上角 → 「触发历史」按钮                    │
│       └── 按 app_name 查看触发历史列表 + 详情              │
├──────────────────────────────────────────────────────────┤
│  后端 (pipeline-server)                                   │
│  PipelineTriggerHistoryController（查询入口）              │
│    ├── GET /pipeline/trigger-history/page（分页查询）      │
│    └── GET /pipeline/trigger-history/{id}（详情查询）      │
│  PipelineTriggerHistoryService（业务服务）                  │
│    ├── add()          — 记录触发历史                       │
│    ├── page()         — 分页查询                           │
│    └── getById()      — 详情查询                           │
│  触发历史记录点（两个入口，各自 finally 块记录）             │
│    ├── 手动触发: PipelineServiceImpl.executeWithHistory    │
│    └── 事件触发: PipelineEventServiceImpl.triggerAndExecute│
├──────────────────────────────────────────────────────────┤
│  数据层                                                   │
│    pipeline_trigger_history（触发历史记录表）               │
└──────────────────────────────────────────────────────────┘
```

### 2.2 核心设计决策

| 决策点 | 方案 | 说明 |
|--------|------|------|
| 记录位置 | 两个入口分别记录 | 手动触发在 Service 包装层记录，事件触发在 `triggerAndExecute` 记录，底层 `execute` 保持纯粹 |
| 记录时机 | `finally` 块 | 无论成功或异常，都保证记录一条触发历史 |
| 记录失败处理 | catch + 日志 | 触发历史是辅助功能，记录失败只打日志，不影响主流程 |
| 状态判断 | `pipelineRunId != null` | 触发成功会落地 `pipeline_run` 并返回 id，失败则无 id |

### 2.3 核心流程图

```mermaid
flowchart TD
    subgraph 手动触发
        A["前端调用<br/>POST /pipeline/execute"] --> B["PipelineController.execute"]
        B --> C["PipelineServiceImpl.executeWithHistory"]
        C --> D["pipelineService.execute"]
        D --> E{"执行结果"}
        E -- 成功 --> F["pipelineRunId 有值"]
        E -- 失败 --> G["pipelineRunId 为 null<br/>捕获 errorMessage"]
        F --> H["finally: 记录触发历史<br/>status=SUCCESS"]
        G --> H["finally: 记录触发历史<br/>status=FAILED"]
    end

    subgraph 事件触发
        I["三方调用<br/>POST /pipeline/event/trigger"] --> J["PipelineEventServiceImpl.triggerAndExecute"]
        J --> K["模板匹配 → pipeline 复用/创建<br/>→ 参数构建"]
        K --> L["pipelineService.execute"]
        L --> M{"执行结果"}
        M -- 成功 --> N["pipelineRunId 有值"]
        M -- 失败 --> O["pipelineRunId 为 null<br/>捕获 errorMessage"]
        N --> P["finally: 记录触发历史<br/>status=SUCCESS"]
        O --> P["finally: 记录触发历史<br/>status=FAILED"]
    end
```

---

## 三、数据模型设计

### 3.1 新增表：触发历史表 `pipeline_trigger_history`

```sql
CREATE TABLE `pipeline_trigger_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `app_name` varchar(200) NOT NULL COMMENT '应用名称，对应 app_info.app_name',
  `pipeline_id` bigint NOT NULL COMMENT '流水线id，对应 pipeline.id',
  `pipeline_run_id` bigint DEFAULT NULL COMMENT '流水线执行记录id，对应 pipeline_run.id；触发失败时为 NULL',
  `pipeline_event_bind_id` bigint NOT NULL DEFAULT '0' COMMENT '事件绑定记录id，对应 pipeline_event_bind.id；手动触发固定为 0',
  `status` varchar(30) NOT NULL COMMENT '触发状态：SUCCESS-成功，FAILED-失败',
  `type` varchar(100) NOT NULL COMMENT '触发类型：手动触发为 user，事件触发为对应的事件类型 eventType',
  `creator` varchar(45) NOT NULL COMMENT '触发人；手动触发取登录用户，事件触发优先取 operator 参数，无则取 eventType',
  `request_body` longtext COMMENT '触发请求的请求体（JSON 字符串）',
  `error_message` text COMMENT '触发失败时的错误信息',
  `pipeline_template_code` varchar(200) NOT NULL COMMENT '触发的流水线模板编码',
  `pipeline_template_version` varchar(30) DEFAULT NULL COMMENT '触发的流水线模板版本；触发失败时可能为 NULL',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_pipeline_id` (`pipeline_id`),
  KEY `idx_app_name` (`app_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf4mb3 COMMENT='流水线触发历史记录表';
```

### 3.2 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | `bigint` | 是 | 自增主键 |
| `app_name` | `varchar(200)` | 是 | 应用名称 |
| `pipeline_id` | `bigint` | 是 | 流水线 id |
| `pipeline_run_id` | `bigint` | 否 | 流水线执行记录 id；触发成功时有值（`pipeline_run` 主键），触发失败时为 `NULL` |
| `pipeline_event_bind_id` | `bigint` | 是 | 事件绑定记录 id（`pipeline_event_bind.id`）；手动触发固定为 `0`，事件触发为实际绑定记录 id |
| `status` | `varchar(30)` | 是 | 触发状态：`SUCCESS`（成功）、`FAILED`（失败） |
| `type` | `varchar(100)` | 是 | 触发类型：手动触发固定为 `user`，事件触发为对应的 `eventType`（如 `epTestApply`） |
| `creator` | `varchar(45)` | 是 | 触发人；手动触发取 `UserContext.getUserId()`，事件触发优先取 operator 参数，无则取 `eventType` |
| `request_body` | `longtext` | 否 | 触发请求的请求体，序列化为 JSON 字符串 |
| `error_message` | `text` | 否 | 触发失败时的错误信息 |
| `pipeline_template_code` | `varchar(200)` | 是 | 触发的流水线模板编码 |
| `pipeline_template_version` | `varchar(30)` | 否 | 触发的流水线模板版本；触发失败（如模板无生效版本）时可能为 `NULL` |
| `create_time` | `datetime` | 是 | 创建时间，自动填充 |
| `update_time` | `datetime` | 是 | 更新时间，自动更新 |
| `deleted` | `tinyint(1)` | 是 | 逻辑删除标记 |

### 3.3 索引设计

| 索引名 | 字段 | 用途 |
|--------|------|------|
| `PRIMARY` | `id` | 主键 |
| `idx_pipeline_id` | `pipeline_id` | 按 pipeline 维度查询触发历史（pipeline 列表操作列入口） |
| `idx_app_name` | `app_name` | 按 appName 维度查询触发历史（流水线列表右上角入口） |

### 3.4 状态枚举

| 枚举值 | 含义 | 判断条件 |
|--------|------|---------|
| `SUCCESS` | 触发成功 | `pipelineRunId != null`（底层 execute 正常返回，已落地 pipeline_run） |
| `FAILED` | 触发失败 | `pipelineRunId == null`（底层 execute 抛异常或返回 null） |

---

## 四、接口设计

### 4.1 分页查询触发历史

**接口路径**：`GET /pipeline/trigger-history/page`

**请求参数**（Query 参数）：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `pipelineId` | `Long` | 否 | 按流水线 id 过滤；与 `appName` 至少传一个 |
| `appName` | `String` | 否 | 按应用名称过滤；与 `pipelineId` 至少传一个 |
| `status` | `String` | 否 | 按触发状态过滤（`SUCCESS` / `FAILED`） |
| `type` | `String` | 否 | 按触发类型过滤（如 `user`、`epTestApply`） |
| `pageNum` | `Long` | 否 | 页码，从 1 开始，默认 1 |
| `pageSize` | `Long` | 否 | 每页条数，默认 10 |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 100,
        "appName": "demo-service",
        "pipelineId": 50,
        "pipelineRunId": 200,
        "pipelineEventBindId": 0,
        "status": "SUCCESS",
        "type": "user",
        "creator": "zhangsan",
        "requestBody": "{\"pipelineId\":50,\"parameters\":{\"git-branch\":\"master\"}}",
        "errorMessage": null,
        "pipelineTemplateCode": "java-build-deploy",
        "pipelineTemplateVersion": "1.0.0",
        "createTime": "2026-07-29 10:30:00"
      }
    ],
    "total": 50,
    "current": 1,
    "size": 10,
    "pages": 5
  }
}
```

### 4.2 查询触发历史详情

**接口路径**：`GET /pipeline/trigger-history/{id}`

**路径参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `id` | `Long` | 是 | 触发历史记录 id |

**响应示例**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 100,
    "appName": "demo-service",
    "pipelineId": 50,
    "pipelineRunId": 200,
    "pipelineEventBindId": 0,
    "status": "SUCCESS",
    "type": "user",
    "creator": "zhangsan",
    "requestBody": "{\"pipelineId\":50,\"parameters\":{\"git-branch\":\"master\"}}",
    "errorMessage": null,
    "pipelineTemplateCode": "java-build-deploy",
    "pipelineTemplateVersion": "1.0.0",
    "createTime": "2026-07-29 10:30:00"
  }
}
```

---

## 五、核心模块设计

### 5.1 包结构

```
pipeline-server-common
└── com.ci.pipeline.common.constants
    └── PipelineTriggerHistoryConstants.java       # 触发历史常量

pipeline-server-dao
├── com.ci.pipeline.dao.entity
│   └── PipelineTriggerHistory.java                # 实体类
├── com.ci.pipeline.dao.mapper
│   └── PipelineTriggerHistoryMapper.java          # Mapper 接口
├── com.ci.pipeline.dao.repository
│   └── PipelineTriggerHistoryRepository.java      # Repository 封装
└── resources/mapper
    └── PipelineTriggerHistoryMapper.xml           # XML 映射

pipeline-server-facade
├── com.ci.pipeline.facade.request
│   └── PipelineTriggerHistoryQueryRequest.java    # 分页查询请求
└── com.ci.pipeline.facade.response
    └── PipelineTriggerHistoryResponse.java         # 触发历史响应

pipeline-server-service
├── com.ci.pipeline.service.controller
│   └── PipelineTriggerHistoryController.java       # 查询 Controller
├── com.ci.pipeline.service.service
│   └── PipelineTriggerHistoryService.java          # Service 接口
└── com.ci.pipeline.service.service.impl
    └── PipelineTriggerHistoryServiceImpl.java      # Service 实现
```

### 5.2 常量定义

**文件**：`pipeline-server-common/.../constants/PipelineTriggerHistoryConstants.java`

```java
package com.ci.pipeline.common.constants;

/**
 * 流水线触发历史相关常量
 */
public final class PipelineTriggerHistoryConstants {

    private PipelineTriggerHistoryConstants() {}

    /** 手动触发类型 */
    public static final String TRIGGER_TYPE_USER = "user";

    /** 触发状态 - 成功 */
    public static final String STATUS_SUCCESS = "SUCCESS";

    /** 触发状态 - 失败 */
    public static final String STATUS_FAILED = "FAILED";

    /** 手动触发的 pipelineEventBindId 固定值 */
    public static final long MANUAL_TRIGGER_ID = 0L;

    /** 提示信息 */
    public static final String MSG_TRIGGER_HISTORY_NOT_EXIST = "触发历史记录不存在";
    public static final String MSG_TRIGGER_HISTORY_QUERY_PARAM_REQUIRED =
            "pipelineId 和 appName 至少传一个";
}
```

### 5.3 Entity 设计

**文件**：`pipeline-server-dao/.../entity/PipelineTriggerHistory.java`

```java
package com.ci.pipeline.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("pipeline_trigger_history")
public class PipelineTriggerHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 应用名称 */
    private String appName;

    /** 流水线id */
    private Long pipelineId;

    /** 流水线执行记录id；触发失败时为 null */
    private Long pipelineRunId;

    /** 事件绑定记录id；手动触发固定为 0 */
    private Long pipelineEventBindId;

    /** 触发状态：SUCCESS / FAILED */
    private String status;

    /** 触发类型：手动触发为 user，事件触发为 eventType */
    private String type;

    /** 触发人 */
    private String creator;

    /** 触发请求的请求体（JSON 字符串） */
    private String requestBody;

    /** 触发失败时的错误信息 */
    private String errorMessage;

    /** 流水线模板编码 */
    private String pipelineTemplateCode;

    /** 流水线模板版本 */
    private String pipelineTemplateVersion;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /** 逻辑删除：0-未删除，1-已删除 */
    private Integer deleted;
}
```

### 5.4 Mapper 设计

**文件**：`pipeline-server-dao/.../mapper/PipelineTriggerHistoryMapper.java`

```java
package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.dao.entity.PipelineTriggerHistory;
import org.apache.ibatis.annotations.Param;

public interface PipelineTriggerHistoryMapper extends BaseMapper<PipelineTriggerHistory> {

    IPage<PipelineTriggerHistory> pageQuery(IPage<PipelineTriggerHistory> page,
                                            @Param("pipelineId") Long pipelineId,
                                            @Param("appName") String appName,
                                            @Param("status") String status,
                                            @Param("type") String type);
}
```

**文件**：`pipeline-server-dao/src/main/resources/mapper/PipelineTriggerHistoryMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ci.pipeline.dao.mapper.PipelineTriggerHistoryMapper">

    <resultMap id="BaseResultMap" type="com.ci.pipeline.dao.entity.PipelineTriggerHistory">
        <id column="id" property="id"/>
        <result column="app_name" property="appName"/>
        <result column="pipeline_id" property="pipelineId"/>
        <result column="pipeline_run_id" property="pipelineRunId"/>
        <result column="pipeline_event_bind_id" property="pipelineEventBindId"/>
        <result column="status" property="status"/>
        <result column="type" property="type"/>
        <result column="creator" property="creator"/>
        <result column="request_body" property="requestBody"/>
        <result column="error_message" property="errorMessage"/>
        <result column="pipeline_template_code" property="pipelineTemplateCode"/>
        <result column="pipeline_template_version" property="pipelineTemplateVersion"/>
        <result column="create_time" property="createTime"/>
        <result column="update_time" property="updateTime"/>
        <result column="deleted" property="deleted"/>
    </resultMap>

    <sql id="Base_Column_List">
        id, app_name, pipeline_id, pipeline_run_id, pipeline_event_bind_id, status, type,
        creator, request_body, error_message, pipeline_template_code, pipeline_template_version,
        create_time, update_time, deleted
    </sql>

    <select id="pageQuery" resultMap="BaseResultMap">
        SELECT <include refid="Base_Column_List"/>
        FROM pipeline_trigger_history
        <where>
            deleted = 0
            <if test="pipelineId != null">AND pipeline_id = #{pipelineId}</if>
            <if test="appName != null and appName != ''">AND app_name = #{appName}</if>
            <if test="status != null and status != ''">AND status = #{status}</if>
            <if test="type != null and type != ''">AND type = #{type}</if>
        </where>
        ORDER BY id DESC
    </select>
</mapper>
```

### 5.5 Repository 设计

**文件**：`pipeline-server-dao/.../repository/PipelineTriggerHistoryRepository.java`

```java
package com.ci.pipeline.dao.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ci.pipeline.dao.entity.PipelineTriggerHistory;
import com.ci.pipeline.dao.mapper.PipelineTriggerHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class PipelineTriggerHistoryRepository {

    @Autowired
    private PipelineTriggerHistoryMapper pipelineTriggerHistoryMapper;

    public int insert(PipelineTriggerHistory entity) {
        return pipelineTriggerHistoryMapper.insert(entity);
    }

    public PipelineTriggerHistory selectById(Long id) {
        return pipelineTriggerHistoryMapper.selectById(id);
    }

    public IPage<PipelineTriggerHistory> pageQuery(long pageNum, long pageSize, Long pipelineId,
                                                    String appName, String status, String type) {
        Page<PipelineTriggerHistory> page = new Page<>(pageNum, pageSize);
        return pipelineTriggerHistoryMapper.pageQuery(page, pipelineId, appName, status, type);
    }
}
```

### 5.6 Facade 层设计

**文件**：`pipeline-server-facade/.../request/PipelineTriggerHistoryQueryRequest.java`

```java
package com.ci.pipeline.facade.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class PipelineTriggerHistoryQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 按流水线 id 过滤 */
    private Long pipelineId;

    /** 按应用名称过滤 */
    private String appName;

    /** 按触发状态过滤：SUCCESS / FAILED */
    private String status;

    /** 按触发类型过滤：如 user、epTestApply */
    private String type;

    /** 页码，从 1 开始 */
    private Long pageNum;

    /** 每页条数 */
    private Long pageSize;
}
```

**文件**：`pipeline-server-facade/.../response/PipelineTriggerHistoryResponse.java`

```java
package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class PipelineTriggerHistoryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 应用名称 */
    private String appName;

    /** 流水线id */
    private Long pipelineId;

    /** 流水线执行记录id */
    private Long pipelineRunId;

    /** 事件绑定记录id；手动触发为 0 */
    private Long pipelineEventBindId;

    /** 触发状态：SUCCESS / FAILED */
    private String status;

    /** 触发类型 */
    private String type;

    /** 触发人 */
    private String creator;

    /** 触发请求的请求体 */
    private String requestBody;

    /** 触发失败时的错误信息 */
    private String errorMessage;

    /** 流水线模板编码 */
    private String pipelineTemplateCode;

    /** 流水线模板版本 */
    private String pipelineTemplateVersion;

    /** 创建时间 */
    private Date createTime;
}
```

### 5.7 Service 层设计

**文件**：`pipeline-server-service/.../service/PipelineTriggerHistoryService.java`

```java
package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.PipelineTriggerHistoryQueryRequest;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineTriggerHistoryResponse;

public interface PipelineTriggerHistoryService {

    /**
     * 记录触发历史
     *
     * @param appName              应用名称
     * @param pipelineId           流水线id
     * @param pipelineRunId        流水线执行记录id（触发失败时为 null）
     * @param pipelineEventBindId  事件绑定记录id（手动触发为 0）
     * @param status               触发状态（SUCCESS / FAILED）
     * @param type                 触发类型（user / eventType）
     * @param creator              触发人
     * @param requestBody          触发请求体（JSON 字符串）
     * @param errorMessage         失败信息（成功时为 null）
     * @param pipelineTemplateCode 流水线模板编码
     * @param pipelineTemplateVersion 流水线模板版本（可能为 null）
     */
    void add(String appName, Long pipelineId, Long pipelineRunId, Long pipelineEventBindId,
             String status, String type, String creator, String requestBody,
             String errorMessage, String pipelineTemplateCode, String pipelineTemplateVersion);

    /**
     * 分页查询触发历史
     */
    PageResponse<PipelineTriggerHistoryResponse> page(PipelineTriggerHistoryQueryRequest query);

    /**
     * 查询触发历史详情
     */
    PipelineTriggerHistoryResponse getById(Long id);
}
```

**文件**：`pipeline-server-service/.../service/impl/PipelineTriggerHistoryServiceImpl.java`

```java
package com.ci.pipeline.service.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.common.constants.CommonConstants;
import com.ci.pipeline.common.constants.PipelineTriggerHistoryConstants;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.dao.entity.PipelineTriggerHistory;
import com.ci.pipeline.dao.repository.PipelineTriggerHistoryRepository;
import com.ci.pipeline.facade.request.PipelineTriggerHistoryQueryRequest;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineTriggerHistoryResponse;
import com.ci.pipeline.service.service.PipelineTriggerHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PipelineTriggerHistoryServiceImpl implements PipelineTriggerHistoryService {

    @Autowired
    private PipelineTriggerHistoryRepository pipelineTriggerHistoryRepository;

    @Override
    public void add(String appName, Long pipelineId, Long pipelineRunId, Long pipelineEventBindId,
                    String status, String type, String creator, String requestBody,
                    String errorMessage, String pipelineTemplateCode, String pipelineTemplateVersion) {
        try {
            PipelineTriggerHistory entity = new PipelineTriggerHistory();
            entity.setAppName(appName);
            entity.setPipelineId(pipelineId);
            entity.setPipelineRunId(pipelineRunId);
            entity.setPipelineEventBindId(pipelineEventBindId);
            entity.setStatus(status);
            entity.setType(type);
            entity.setCreator(creator);
            entity.setRequestBody(requestBody);
            entity.setErrorMessage(errorMessage);
            entity.setPipelineTemplateCode(pipelineTemplateCode);
            entity.setPipelineTemplateVersion(pipelineTemplateVersion);
            pipelineTriggerHistoryRepository.insert(entity);
        } catch (Exception e) {
            // 触发历史是辅助功能，记录失败只打日志，不影响主流程
            log.error("记录触发历史失败, appName={}, pipelineId={}, type={}", appName, pipelineId, type, e);
        }
    }

    @Override
    public PageResponse<PipelineTriggerHistoryResponse> page(PipelineTriggerHistoryQueryRequest query) {
        if (query.getPipelineId() == null && !StringUtils.hasText(query.getAppName())) {
            throw new BusinessException(
                    PipelineTriggerHistoryConstants.MSG_TRIGGER_HISTORY_QUERY_PARAM_REQUIRED);
        }
        long pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        long pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();
        IPage<PipelineTriggerHistory> pageResult = pipelineTriggerHistoryRepository.pageQuery(
                pageNum, pageSize, query.getPipelineId(), query.getAppName(),
                query.getStatus(), query.getType());
        List<PipelineTriggerHistoryResponse> records = pageResult.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(records, pageResult.getTotal(), pageResult.getCurrent(),
                pageResult.getSize(), pageResult.getPages());
    }

    @Override
    public PipelineTriggerHistoryResponse getById(Long id) {
        PipelineTriggerHistory entity = pipelineTriggerHistoryRepository.selectById(id);
        if (entity == null) {
            throw new BusinessException(PipelineTriggerHistoryConstants.MSG_TRIGGER_HISTORY_NOT_EXIST);
        }
        return toResponse(entity);
    }

    private PipelineTriggerHistoryResponse toResponse(PipelineTriggerHistory entity) {
        if (entity == null) return null;
        PipelineTriggerHistoryResponse response = new PipelineTriggerHistoryResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
```

### 5.8 Controller 设计

**文件**：`pipeline-server-service/.../controller/PipelineTriggerHistoryController.java`

```java
package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.PipelineTriggerHistoryQueryRequest;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineTriggerHistoryResponse;
import com.ci.pipeline.service.service.PipelineTriggerHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/pipeline/trigger-history")
@RequireLogin
public class PipelineTriggerHistoryController {

    @Autowired
    private PipelineTriggerHistoryService pipelineTriggerHistoryService;

    @GetMapping("/page")
    public Result<PageResponse<PipelineTriggerHistoryResponse>> page(
            PipelineTriggerHistoryQueryRequest query) {
        return Result.success(pipelineTriggerHistoryService.page(query));
    }

    @GetMapping("/{id}")
    public Result<PipelineTriggerHistoryResponse> get(@PathVariable("id") Long id) {
        return Result.success(pipelineTriggerHistoryService.getById(id));
    }
}
```

---

## 六、触发历史记录点设计

### 6.1 手动触发记录点

**改造位置**：`PipelineServiceImpl` 新增包装方法 `executeWithHistory`，`PipelineController.execute` 改为调用此方法。

**设计思路**：`execute` 方法保持纯粹（只负责执行），新增 `executeWithHistory` 包装方法负责在 `finally` 块中记录触发历史。

**改造后的 Controller**：

```java
@PostMapping("/execute")
public Result<PipelineExecuteResponse> execute(@RequestBody PipelineExecuteRequest request) {
    return Result.success(pipelineService.executeWithHistory(request));
}
```

**新增包装方法**（在 `PipelineServiceImpl` 中）：

```java
@Override
public PipelineExecuteResponse executeWithHistory(PipelineExecuteRequest request) {
    Long pipelineRunId = null;
    String errorMessage = null;
    Pipeline pipeline = null;
    PipelineTemplateVersion effective = null;
    try {
        // 先查出 pipeline 和模板信息，用于记录触发历史
        if (request != null && request.getPipelineId() != null) {
            pipeline = pipelineRepository.selectById(request.getPipelineId());
            if (pipeline != null) {
                effective = pipelineTemplateVersionRepository.selectEffectiveByCode(
                        pipeline.getPipelineTemplateCode());
            }
        }
        PipelineExecuteResponse response = execute(request);
        pipelineRunId = response != null ? response.getPipelineRunId() : null;
        return response;
    } catch (RuntimeException e) {
        errorMessage = e.getMessage();
        throw e;
    } finally {
        recordManualTriggerHistory(request, pipeline, effective, pipelineRunId, errorMessage);
    }
}

/**
 * 记录手动触发历史
 */
private void recordManualTriggerHistory(PipelineExecuteRequest request, Pipeline pipeline,
                                        PipelineTemplateVersion effective,
                                        Long pipelineRunId, String errorMessage) {
    try {
        if (pipeline == null) return;
        String status = pipelineRunId != null
                ? PipelineTriggerHistoryConstants.STATUS_SUCCESS
                : PipelineTriggerHistoryConstants.STATUS_FAILED;
        String requestBody = request != null ? JSON.toJSONString(request) : null;
        String templateCode = pipeline.getPipelineTemplateCode();
        String templateVersion = effective != null ? effective.getPipelineTemplateVersion() : null;
        String creator = UserContext.getUserId();

        pipelineTriggerHistoryService.add(
                pipeline.getAppName(),
                pipeline.getId(),
                pipelineRunId,
                PipelineTriggerHistoryConstants.MANUAL_TRIGGER_ID,
                status,
                PipelineTriggerHistoryConstants.TRIGGER_TYPE_USER,
                creator,
                requestBody,
                errorMessage,
                templateCode,
                templateVersion);
    } catch (Exception e) {
        log.error("记录手动触发历史失败", e);
    }
}
```

**PipelineService 接口新增方法**：

```java
/**
 * 执行流水线并记录触发历史（手动触发入口）
 */
PipelineExecuteResponse executeWithHistory(PipelineExecuteRequest request);
```

### 6.2 事件触发记录点

**改造位置**：`PipelineEventServiceImpl.triggerAndExecute` 方法，在 `finally` 块中记录触发历史。

**设计思路**：`triggerAndExecute` 方法本身包含模板匹配、pipeline 复用/创建、参数构建、执行等完整逻辑。在方法体外层用 try-finally 包裹，确保无论成功或失败都记录触发历史。

**改造后的 `triggerAndExecute`**：

```java
@Override
public Long triggerAndExecute(String eventType, String appName, Map<String, String> params) {
    Long pipelineRunId = null;
    String errorMessage = null;
    // 以下变量在流程中逐步赋值，用于 finally 记录触发历史
    String pipelineTemplateCode = null;
    String pipelineTemplateVersion = null;
    Long pipelineId = null;
    Long pipelineEventBindId = PipelineTriggerHistoryConstants.MANUAL_TRIGGER_ID;
    String creator = resolveEventCreator(eventType, params);

    try {
        // Step 1: 根据 eventType 查询绑定的模板编码列表
        List<String> templateCodes = pipelineTemplateEventBindService.listTemplateCodesByEventType(eventType);
        if (templateCodes == null || templateCodes.isEmpty()) {
            throw new BusinessException(String.format(
                    PipelineEventConstants.MSG_EVENT_NO_TEMPLATE_BIND, eventType));
        }

        // Step 2: 根据 appName 查询应用信息，获取编程语言
        AppInfo appInfo = appInfoRepository.selectByAppName(appName);
        if (appInfo == null) {
            throw new BusinessException(String.format(
                    PipelineEventConstants.MSG_APP_NOT_EXIST, appName));
        }
        String programmingLanguage = appInfo.getProgrammingLanguage();

        // Step 3: 根据编程语言过滤模板
        pipelineTemplateCode = matchTemplate(templateCodes, programmingLanguage, appName);

        // Step 4: 查询模板是否有生效版本
        PipelineTemplateVersion effectiveVersion =
                pipelineTemplateVersionRepository.selectEffectiveByCode(pipelineTemplateCode);
        if (effectiveVersion == null) {
            throw new BusinessException(String.format(
                    PipelineEventConstants.MSG_TEMPLATE_NO_EFFECTIVE_VERSION, pipelineTemplateCode));
        }
        pipelineTemplateVersion = effectiveVersion.getPipelineTemplateVersion();

        // Step 5: 查询或创建 pipeline_event_bind
        PipelineEventBind eventBind = pipelineEventBindService.getByAppNameAndEventTypeAndTemplateCode(
                appName, eventType, pipelineTemplateCode);
        if (eventBind != null) {
            pipelineId = eventBind.getPipelineId();
            pipelineEventBindId = eventBind.getId();
        } else {
            pipelineId = createPipelineForEvent(appName, eventType, pipelineTemplateCode);
            // 新创建的绑定记录
            PipelineEventBind newBind = pipelineEventBindService.create(
                    appName, eventType, pipelineTemplateCode, pipelineId);
            pipelineEventBindId = newBind != null ? newBind.getId() : 0L;
        }

        // Step 6: 构建执行参数
        Map<String, String> executeParams = buildExecuteParams(pipelineId, params);

        // Step 7: 执行流水线
        PipelineExecuteRequest executeRequest = new PipelineExecuteRequest();
        executeRequest.setPipelineId(pipelineId);
        executeRequest.setParameters(executeParams);
        PipelineExecuteResponse response = pipelineService.execute(executeRequest);
        pipelineRunId = response != null ? response.getPipelineRunId() : null;

        return pipelineRunId;
    } catch (RuntimeException e) {
        errorMessage = e.getMessage();
        throw e;
    } finally {
        recordEventTriggerHistory(eventType, appName, params, pipelineId, pipelineRunId,
                pipelineEventBindId, errorMessage, pipelineTemplateCode, pipelineTemplateVersion, creator);
    }
}

/**
 * 解析事件触发的 creator：优先取 operator 参数，无则取 eventType
 */
private String resolveEventCreator(String eventType, Map<String, String> params) {
    if (params != null) {
        String operator = params.get("operator");
        if (StringUtils.hasText(operator)) {
            return operator;
        }
    }
    return eventType;
}

/**
 * 记录事件触发历史
 */
private void recordEventTriggerHistory(String eventType, String appName, Map<String, String> params,
                                       Long pipelineId, Long pipelineRunId, Long pipelineEventBindId,
                                       String errorMessage, String pipelineTemplateCode,
                                       String pipelineTemplateVersion, String creator) {
    try {
        if (pipelineId == null) {
            // pipelineId 为 null 说明在执行之前就失败了（如模板匹配失败），
            // 此时 pipelineId 未知，使用 0 占位
            pipelineId = 0L;
        }
        String status = pipelineRunId != null
                ? PipelineTriggerHistoryConstants.STATUS_SUCCESS
                : PipelineTriggerHistoryConstants.STATUS_FAILED;
        String requestBody = params != null ? JSON.toJSONString(params) : null;

        pipelineTriggerHistoryService.add(
                appName,
                pipelineId,
                pipelineRunId,
                pipelineEventBindId,
                status,
                eventType,
                creator,
                requestBody,
                errorMessage,
                pipelineTemplateCode,
                pipelineTemplateVersion);
    } catch (Exception e) {
        log.error("记录事件触发历史失败, eventType={}, appName={}", eventType, appName, e);
    }
}
```

> **注意**：`pipelineEventBindService.create` 方法需要返回创建的 `PipelineEventBind` 实体（含 id），以便获取 `pipelineEventBindId`。如果当前 `create` 方法返回 void，需要改为返回 `PipelineEventBind`。

---

## 七、前端设计

### 7.1 入口一：pipeline 列表操作列

在 pipeline 列表页的操作列中，新增「触发历史」按钮：

- 点击后跳转到触发历史列表页
- 查询参数：`pipelineId = 当前行 pipeline id`
- 列表按 `id` 倒序展示
- 每行支持点击查看详情（展示完整字段，包括 `requestBody` 和 `errorMessage`）

### 7.2 入口二：流水线列表右上角

在流水线列表页右上角，「新建流水线」按钮右侧新增「触发历史」按钮：

- 点击后跳转到触发历史列表页
- 查询参数：`appName = 当前页选中的 appName`（或弹出选择框让用户选择 appName）
- 列表按 `id` 倒序展示
- 每行支持点击查看详情

### 7.3 触发历史列表页

**列表字段**：

| 列名 | 字段 | 说明 |
|------|------|------|
| ID | `id` | 触发历史记录 id |
| 应用 | `appName` | 应用名称 |
| 流水线ID | `pipelineId` | 流水线 id |
| 执行记录ID | `pipelineRunId` | 流水线执行记录 id（失败时为空） |
| 状态 | `status` | `SUCCESS` 绿色 / `FAILED` 红色 |
| 类型 | `type` | 触发类型（`user` / 事件类型） |
| 触发人 | `creator` | 触发人 |
| 模板编码 | `pipelineTemplateCode` | 流水线模板编码 |
| 模板版本 | `pipelineTemplateVersion` | 流水线模板版本 |
| 触发时间 | `createTime` | 创建时间 |
| 操作 | — | 「查看详情」按钮 |

**筛选条件**：状态（下拉选择）、类型（输入框）

**详情弹窗/页面**：展示所有字段，其中 `requestBody` 和 `errorMessage` 以格式化文本展示。

---

## 八、关键设计决策

### 8.1 为什么在两个入口分别记录，而不是在底层 execute 统一记录？

两个入口的上下文信息差异较大：

| 信息 | 手动触发 | 事件触发 |
|------|---------|---------|
| `type` | 固定 `user` | 对应的 `eventType` |
| `creator` | `UserContext.getUserId()` | operator 参数或 eventType |
| `pipelineEventBindId` | 固定 `0` | `PipelineEventBind.id` |
| `requestBody` | `PipelineExecuteRequest` | 事件触发的 params Map |

如果在底层 `execute` 统一记录，需要通过 ThreadLocal 或参数传递这些上下文信息，侵入性大且不自然。在各自入口组装更清晰。

### 8.2 触发失败时 pipeline_run_id 为什么用 NULL 而不是 0？

触发失败意味着底层 `execute` 抛异常，没有落地 `pipeline_run` 数据，自然没有对应的 id。使用 `NULL` 语义更明确（"没有值"），而 `0` 可能被误认为存在 id=0 的记录。查询时 `IS NULL` 也比 `= 0` 更清晰。

### 8.3 事件触发在执行前就失败时，pipeline_id 如何处理？

事件触发的 `triggerAndExecute` 方法在执行流水线之前有多个可能失败的步骤（模板匹配、查生效版本等）。如果在获取 `pipelineId` 之前就失败了，`pipelineId` 为 null。此时使用 `0` 占位记录，确保触发历史仍然被记录，`errorMessage` 中有具体的失败原因。

### 8.4 记录触发历史失败时的处理

触发历史是辅助功能，不应阻断流水线执行。因此在 `recordManualTriggerHistory` 和 `recordEventTriggerHistory` 方法内部都做了 try-catch，记录失败只打 error 日志，不抛异常。

### 8.5 pipelineEventBindId 的语义

pipeline-server 没有 trigger 实体概念，事件绑定使用的是 `PipelineEventBind`（事件-pipeline 绑定关系）。因此 `pipeline_event_bind_id` 字段存储的是 `PipelineEventBind.id`：

- **手动触发**：没有事件绑定关系，固定为 `0`
- **事件触发**：存储对应的 `PipelineEventBind.id`，可以通过该 id 关联查询事件绑定详情

---

## 九、实现清单

### 9.1 后端文件清单

| 类型 | 文件路径 | 操作 |
|------|---------|------|
| SQL | `sql/pipeline_trigger_history.sql` | 新增 |
| Entity | `pipeline-server-dao/.../entity/PipelineTriggerHistory.java` | 新增 |
| Mapper | `pipeline-server-dao/.../mapper/PipelineTriggerHistoryMapper.java` | 新增 |
| Mapper XML | `pipeline-server-dao/src/main/resources/mapper/PipelineTriggerHistoryMapper.xml` | 新增 |
| Repository | `pipeline-server-dao/.../repository/PipelineTriggerHistoryRepository.java` | 新增 |
| Request | `pipeline-server-facade/.../request/PipelineTriggerHistoryQueryRequest.java` | 新增 |
| Response | `pipeline-server-facade/.../response/PipelineTriggerHistoryResponse.java` | 新增 |
| Constants | `pipeline-server-common/.../constants/PipelineTriggerHistoryConstants.java` | 新增 |
| Service 接口 | `pipeline-server-service/.../service/PipelineTriggerHistoryService.java` | 新增 |
| Service 实现 | `pipeline-server-service/.../service/impl/PipelineTriggerHistoryServiceImpl.java` | 新增 |
| Controller | `pipeline-server-service/.../controller/PipelineTriggerHistoryController.java` | 新增 |
| Service 接口 | `pipeline-server-service/.../service/PipelineService.java` | 修改：新增 `executeWithHistory` |
| Service 实现 | `pipeline-server-service/.../service/impl/PipelineServiceImpl.java` | 修改：新增 `executeWithHistory` + `recordManualTriggerHistory` |
| Controller | `pipeline-server-service/.../controller/PipelineController.java` | 修改：`execute` 改调 `executeWithHistory` |
| Service 实现 | `pipeline-server-service/.../service/impl/PipelineEventServiceImpl.java` | 修改：`triggerAndExecute` 增加 finally 记录逻辑 |
| Service 接口 | `pipeline-server-service/.../service/PipelineEventBindService.java` | 修改：`create` 方法返回值改为 `PipelineEventBind` |
| Service 实现 | `pipeline-server-service/.../service/impl/PipelineEventBindServiceImpl.java` | 修改：`create` 方法返回创建的实体 |

### 9.2 前端文件清单

| 类型 | 说明 |
|------|------|
| 触发历史列表页 | 新增页面，支持分页查询、筛选、查看详情 |
| pipeline 列表页 | 修改：操作列新增「触发历史」按钮 |
| 流水线列表页 | 修改：右上角新增「触发历史」按钮 |
| API 接口封装 | 新增触发历史分页查询、详情查询接口 |

---

## 十、测试要点

### 10.1 功能测试

| 场景 | 验证点 |
|------|--------|
| 手动触发成功 | 触发历史记录 status=SUCCESS，pipelineRunId 有值，type=user，creator=登录用户 |
| 手动触发失败（参数校验） | 触发历史记录 status=FAILED，pipelineRunId=null，errorMessage 有值 |
| 手动触发失败（模板无生效版本） | 触发历史记录 status=FAILED，pipelineTemplateVersion=null |
| 事件触发成功 | 触发历史记录 status=SUCCESS，type=eventType，pipelineEventBindId=EventBind.id |
| 事件触发失败（模板未匹配） | 触发历史记录 status=FAILED，pipelineId=0，errorMessage 有值 |
| 事件触发失败（应用不存在） | 触发历史记录 status=FAILED，pipelineId=0 |
| 事件触发 creator 取值 | params 有 operator 时 creator=operator，无 operator 时 creator=eventType |
| 分页查询（按 pipelineId） | 只返回指定 pipelineId 的记录，按 id 倒序 |
| 分页查询（按 appName） | 只返回指定 appName 的记录 |
| 分页查询（按 status 过滤） | 只返回匹配状态的记录 |
| 详情查询 | 返回完整字段，包括 requestBody 和 errorMessage |
| 查询参数校验 | pipelineId 和 appName 都不传时返回错误提示 |

### 10.2 异常测试

| 场景 | 验证点 |
|------|--------|
| 记录触发历史时 DB 异常 | 主流程不受影响，流水线正常执行/正常抛异常，日志中有 error 记录 |
| 触发历史查询不存在的 id | 返回"触发历史记录不存在"错误提示 |
