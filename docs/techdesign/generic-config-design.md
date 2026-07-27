# 通用配置管理 - 技术设计方案

## 一、背景与目标

### 1.1 现状

pipeline-server 在运行过程中需要管理一些自定义的静态配置数据（如开关、阈值、环境地址、规则映射等）。业界通常使用 Apollo、Nacos 等成熟配置中心组件，但这些组件需要独立部署和维护，引入后会增加基础设施的运维成本和系统复杂度。

### 1.2 目标

基于数据库实现一个轻量级的通用配置管理模块，满足以下诉求：

1. **KV 配置管理**：支持以 key-value 形式管理静态配置，支持纯文本和 JSON 两种值格式。
2. **变更历史**：完整记录每次创建、修改、删除操作，支持按配置项查看历史，也支持全局查看所有变更记录，便于审计追溯。
3. **运行时取值**：提供按 key 查询配置值的接口，供业务模块在运行时读取配置。
4. **零外部依赖**：不引入额外中间件，仅依赖现有 MySQL，保持项目简单。

### 1.3 非目标

- 不做权限控制（本期所有登录用户均可读写）。
- 不做本地缓存（配置读取频率不高，直接查 DB）。
- 不做配置变更回滚（后续迭代视需要再加）。
- 不做配置变更通知 / 监听推送。

---

## 二、整体架构

### 2.1 分层职责

```
┌─────────────────────────────────────────────────────┐
│  前端 (pipeline-frontend)                            │
│  GenericConfig.vue（配置列表 + 新建/编辑弹窗）          │
│  GenericConfigHistory.vue（变更历史，支持全局/单条）     │
├─────────────────────────────────────────────────────┤
│  后端 (pipeline-server)                              │
│  GenericConfigController                             │
│    ├── 配置 CRUD                                     │
│    ├── 按 key 取值                                   │
│    ├── 单条变更历史                                   │
│    └── 全局变更历史（分页）                            │
│  GenericConfigService                                │
│    ├── 配置增删改查                                   │
│    ├── JSON 格式校验                                  │
│    └── 变更历史记录（create/update/delete 均记录）      │
├─────────────────────────────────────────────────────┤
│  数据层                                              │
│    generic_config（配置主表）                         │
│    generic_config_history（变更历史表）                │
└─────────────────────────────────────────────────────┘
```

### 2.2 模块归属

| 层 | 模块 | 包路径 |
|----|------|--------|
| Controller | pipeline-server-service | `com.ci.pipeline.service.controller` |
| Service | pipeline-server-service | `com.ci.pipeline.service.service` / `service.impl` |
| Request / Response | pipeline-server-facade | `com.ci.pipeline.facade.request.config` / `response.config` |
| Entity | pipeline-server-dao | `com.ci.pipeline.dao.entity` |
| Mapper | pipeline-server-dao | `com.ci.pipeline.dao.mapper` |
| Repository | pipeline-server-dao | `com.ci.pipeline.dao.repository` |
| Constants | pipeline-server-common | `com.ci.pipeline.common.constants` |
| Enum | pipeline-server-common | `com.ci.pipeline.common.enums` |

---

## 三、数据模型设计

### 3.1 配置主表 `generic_config`

```sql
CREATE TABLE `generic_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(200) NOT NULL COMMENT '配置键，全局唯一',
  `config_value` longtext DEFAULT NULL COMMENT '配置值，json格式时存序列化字符串',
  `value_format` varchar(20) NOT NULL DEFAULT 'txt' COMMENT '值格式：txt-纯文本 / json-JSON',
  `description` varchar(500) DEFAULT NULL COMMENT '备注说明',
  `creator` varchar(45) NOT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(45) DEFAULT NULL COMMENT '最后修改人',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utfmb3 COMMENT='通用配置表';
```

> **说明**：`config_key` 不设唯一索引，而设普通索引。原因是逻辑删除后用户可能需要重新创建相同 key 的配置，唯一索引会导致冲突。唯一性由 Service 层保证：创建时仅校验是否存在**未删除**的同 key 记录。

### 3.2 变更历史表 `generic_config_history`

```sql
CREATE TABLE `generic_config_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_id` bigint NOT NULL COMMENT '关联配置ID',
  `config_key` varchar(200) NOT NULL COMMENT '配置键快照',
  `action` varchar(20) NOT NULL COMMENT '操作类型：CREATE-新建 / UPDATE-修改 / DELETE-删除',
  `old_value` longtext DEFAULT NULL COMMENT '变更前值',
  `new_value` longtext DEFAULT NULL COMMENT '变更后值',
  `old_value_format` varchar(20) DEFAULT NULL COMMENT '变更前值格式',
  `new_value_format` varchar(20) DEFAULT NULL COMMENT '变更后值格式',
  `change_summary` varchar(500) DEFAULT NULL COMMENT '变更摘要，描述哪些字段发生了变化',
  `operator` varchar(45) NOT NULL COMMENT '操作人',
  `operate_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_config_id` (`config_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utfmb3 COMMENT='通用配置变更历史表';
```

**设计说明**：

- `config_value` / `old_value` / `new_value` 使用 `longtext`，支持存储大段 JSON 配置。
- 历史表不做逻辑删除，所有变更记录永久保留，用于审计。
- `config_key` 在历史表中做快照冗余，即使配置被删除，历史记录仍可独立展示。
- `change_summary` 以可读文本记录变更摘要，例如：`"值格式由 txt 变更为 json；配置值已修改"`。

---

## 四、枚举与常量设计

### 4.1 值格式枚举 `ConfigValueFormatEnum`

```java
public enum ConfigValueFormatEnum {

    TXT("txt", "纯文本"),
    JSON("json", "JSON");

    private final String code;
    private final String description;
    // ...
}
```

### 4.2 操作类型枚举 `ConfigActionEnum`

```java
public enum ConfigActionEnum {

    CREATE("CREATE", "新建"),
    UPDATE("UPDATE", "修改"),
    DELETE("DELETE", "删除");

    private final String code;
    private final String description;
    // ...
}
```

### 4.3 常量类 `GenericConfigConstants`

```java
public final class GenericConfigConstants {
    private GenericConfigConstants() {}

    /** key 长度上限 */
    public static final int KEY_MAX_LENGTH = 200;
    /** description 长度上限 */
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    /** key 不能为空 */
    public static final String MSG_KEY_REQUIRED = "配置键不能为空";
    /** key 长度超限 */
    public static final String MSG_KEY_TOO_LONG = "配置键长度不能超过%d个字符";
    /** key 已存在 */
    public static final String MSG_KEY_DUPLICATED = "已存在相同配置键[%s]，请修改";
    /** 配置不存在 */
    public static final String MSG_NOT_FOUND = "配置项不存在";
    /** 值不能为空 */
    public static final String MSG_VALUE_REQUIRED = "配置值不能为空";
    /** 值格式不支持 */
    public static final String MSG_FORMAT_UNSUPPORTED = "不支持的值格式[%s]，仅支持 txt / json";
    /** JSON 格式非法 */
    public static final String MSG_JSON_INVALID = "配置值必须是合法的JSON对象或数组";
}
```

---

## 五、接口设计

### 5.1 接口清单

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询全部配置 | GET | `/generic-config/list` | 查出全部配置（不分页），支持按 key 模糊搜索 |
| 查询单条配置 | GET | `/generic-config/{id}` | 按主键查询配置详情 |
| 按 key 取值 | GET | `/generic-config/value` | 运行时按 key 查询配置值 |
| 新建配置 | POST | `/generic-config` | 新增一条配置 |
| 修改配置 | PUT | `/generic-config` | 修改配置值 / 格式 / 备注 |
| 删除配置 | DELETE | `/generic-config/{id}` | 逻辑删除 |
| 单条变更历史 | GET | `/generic-config/{id}/history` | 查询指定配置的变更历史（不分页） |
| 全局变更历史 | GET | `/generic-config/history/page` | 分页查询所有配置的变更历史 |

### 5.2 请求 / 响应定义

#### 5.2.1 新建配置 `GenericConfigCreateRequest`

```java
@Data
public class GenericConfigCreateRequest implements Serializable {
    @NotBlank(message = "配置键不能为空")
    @Size(max = 200, message = "配置键长度不能超过200")
    private String configKey;

    @NotNull(message = "配置值不能为空")
    private Object configValue;

    @NotBlank(message = "值格式不能为空")
    private String valueFormat;       // txt / json

    private String description;
}
```

#### 5.2.2 修改配置 `GenericConfigUpdateRequest`

```java
@Data
public class GenericConfigUpdateRequest implements Serializable {
    @NotNull(message = "配置ID不能为空")
    private Long id;

    @NotNull(message = "配置值不能为空")
    private Object configValue;

    @NotBlank(message = "值格式不能为空")
    private String valueFormat;

    private String description;
}
```

> **说明**：修改时不允许变更 `configKey`（key 作为业务标识应保持稳定）。如需修改 key，建议删除后重建。

#### 5.2.3 列表查询 `GenericConfigListRequest`

```java
@Data
public class GenericConfigListRequest implements Serializable {
    private String configKey;   // 可选，模糊搜索
}
```

#### 5.2.4 全局历史分页查询 `GenericConfigHistoryQueryRequest`

```java
@Data
public class GenericConfigHistoryQueryRequest implements Serializable {
    private String configKey;   // 可选，按配置键模糊过滤
    private String action;      // 可选，按操作类型过滤：CREATE / UPDATE / DELETE
    private String operator;    // 可选，按操作人模糊过滤
    private Long pageNum;
    private Long pageSize;
}
```

#### 5.2.5 配置响应 `GenericConfigResponse`

```java
@Data
public class GenericConfigResponse implements Serializable {
    private Long id;
    private String configKey;
    private Object configValue;     // json 格式时返回解析后的对象，txt 返回字符串
    private String valueFormat;
    private String description;
    private String creator;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    private String updater;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
```

#### 5.2.6 历史响应 `GenericConfigHistoryResponse`

```java
@Data
public class GenericConfigHistoryResponse implements Serializable {
    private Long id;
    private Long configId;
    private String configKey;
    private String action;
    private Object oldValue;        // json 格式时返回解析后的对象
    private Object newValue;
    private String oldValueFormat;
    private String newValueFormat;
    private String changeSummary;
    private String operator;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date operateTime;
}
```

### 5.3 接口示例

#### 查询全部配置

```
GET /generic-config/list?configKey=timeout
```

#### 按 key 取值

```
GET /generic-config/value?configKey=deploy.timeout
```

#### 新建配置

```
POST /generic-config
{
    "configKey": "deploy.timeout",
    "configValue": 300,
    "valueFormat": "txt",
    "description": "部署超时时间，单位秒"
}
```

#### 修改配置

```
PUT /generic-config
{
    "id": 1,
    "configValue": 600,
    "valueFormat": "txt",
    "description": "部署超时时间，单位秒"
}
```

#### 删除配置

```
DELETE /generic-config/1
```

#### 单条变更历史

```
GET /generic-config/1/history
```

#### 全局变更历史（分页）

```
GET /generic-config/history/page?configKey=timeout&action=UPDATE&pageNum=1&pageSize=10
```

---

## 六、核心业务逻辑

### 6.1 新建配置

```
1. 校验 configKey 非空、长度合法
2. 校验 valueFormat 合法（仅允许 txt / json）
3. 唯一性校验：按 configKey 查询，若已存在（含未删除）则抛异常
4. 若 valueFormat = json，校验 configValue 为合法 JSON 对象或数组，序列化为字符串存储
5. 若 valueFormat = txt，将 configValue 转为字符串存储
6. 填充 creator / updater = 当前用户
7. insert 主表
8. 记录变更历史：action=CREATE，old_value=null，new_value=当前值，change_summary="新建配置"
```

### 6.2 修改配置

```
1. 校验 id 非空，查询现有配置，不存在则抛异常
2. 校验 valueFormat 合法
3. 若 valueFormat = json，校验并序列化
4. 逐字段对比新旧值，判断是否有变化：
   - 值格式变化 → changeSummary 追加 "值格式由 xxx 变更为 yyy"
   - 配置值变化 → changeSummary 追加 "配置值已修改"
   - 备注变化   → changeSummary 追加 "备注已修改"
5. 若无任何字段变化，直接抛出业务异常提示"配置内容无变化"，不更新主表，不记录历史
6. 若有变化，update 主表（configValue / valueFormat / description / updater / updateTime）
7. 记录变更历史：action=UPDATE，old_value=旧值，new_value=新值，change_summary=拼接的变更描述
```

> **关键**：只有确认至少一个字段发生变化时，才执行更新操作和记录历史。避免无意义的更新和冗余历史记录。

### 6.3 删除配置

```
1. 校验 id 非空，查询现有配置，不存在则抛异常
2. 逻辑删除：update deleted=1
3. 记录变更历史：action=DELETE，old_value=删除前值，new_value=null，change_summary="删除配置"
```

### 6.4 按 key 取值

```
1. 按 configKey 查询未删除的配置
2. 不存在则抛异常
3. 若 valueFormat = json，将存储的字符串解析为有序 JSON 对象返回
4. 若 valueFormat = txt，直接返回字符串
```

### 6.5 变更历史查询

- **单条历史**：按 `config_id` 查询，按 `operate_time DESC` 排序，不分页（单条配置历史量可控）。
- **全局历史**：支持按 `configKey` / `action` / `operator` 过滤，分页查询，按 `operate_time DESC` 排序。

---

## 七、DAO 层设计

### 7.1 Entity

- `GenericConfig`：对应 `generic_config` 表，`@TableName("generic_config")`，审计字段 `creator` / `createTime` / `updater` / `updateTime` / `deleted`。
- `GenericConfigHistory`：对应 `generic_config_history` 表，无逻辑删除字段。

### 7.2 Mapper

- `GenericConfigMapper extends BaseMapper<GenericConfig>`
- `GenericConfigHistoryMapper extends BaseMapper<GenericConfigHistory>`

自定义查询方法（XML）：

```xml
<!-- GenericConfigMapper -->
<select id="listBySearch" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM generic_config
    WHERE deleted = 0
    <if test="configKey != null and configKey != ''">
        AND config_key LIKE CONCAT('%', #{configKey}, '%')
    </if>
    ORDER BY id DESC
</select>

<select id="getByKey" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM generic_config
    WHERE deleted = 0 AND config_key = #{configKey}
    LIMIT 1
</select>

<!-- GenericConfigHistoryMapper -->
<select id="pageQuery" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM generic_config_history
    <where>
        <if test="configKey != null and configKey != ''">
            AND config_key LIKE CONCAT('%', #{configKey}, '%')
        </if>
        <if test="action != null and action != ''">
            AND action = #{action}
        </if>
        <if test="operator != null and operator != ''">
            AND operator LIKE CONCAT('%', #{operator}, '%')
        </if>
    </where>
    ORDER BY operate_time DESC, id DESC
</select>
```

### 7.3 Repository

- `GenericConfigRepository`：封装 `listBySearch` / `getByKey` / `selectById` / `insert` / `updateById`。
- `GenericConfigHistoryRepository`：封装 `insert` / `listByConfigId` / `pageQuery`。

---

## 八、Service 层设计

### 8.1 接口定义 `GenericConfigService`

```java
public interface GenericConfigService {

    /** 查询全部配置（支持按 key 模糊搜索） */
    List<GenericConfigResponse> list(String configKey);

    /** 查询单条配置 */
    GenericConfigResponse getById(Long id);

    /** 按 key 取值（运行时调用） */
    Object getValueByKey(String configKey);

    /** 新建配置 */
    GenericConfigResponse create(GenericConfigCreateRequest request);

    /** 修改配置 */
    GenericConfigResponse update(GenericConfigUpdateRequest request);

    /** 删除配置 */
    void delete(Long id);

    /** 查询单条配置的变更历史 */
    List<GenericConfigHistoryResponse> historyByConfigId(Long configId);

    /** 分页查询全局变更历史 */
    PageResponse<GenericConfigHistoryResponse> historyPage(GenericConfigHistoryQueryRequest query);
}
```

### 8.2 实现要点 `GenericConfigServiceImpl`

- 注入 `GenericConfigRepository` / `GenericConfigHistoryRepository`。
- JSON 校验逻辑：尝试解析为 JSONObject，失败再尝试 JSONArray，均失败则抛 `BusinessException`。
- 变更历史记录封装为私有方法 `recordHistory(...)`，在 create / update / delete 中统一调用。
- `changeSummary` 生成逻辑：逐字段对比，拼接变化描述，用分号分隔。
- 用户标识取自 `UserContext.getUserId()`。

---

## 九、Controller 层设计

```java
@Slf4j
@RestController
@RequestMapping("/generic-config")
@RequireLogin
public class GenericConfigController {

    @Autowired
    private GenericConfigService genericConfigService;

    @GetMapping("/list")
    public Result<List<GenericConfigResponse>> list(@RequestParam(required = false) String configKey);

    @GetMapping("/{id}")
    public Result<GenericConfigResponse> get(@PathVariable Long id);

    @GetMapping("/value")
    public Result<Object> getValue(@RequestParam String configKey);

    @PostMapping
    public Result<GenericConfigResponse> create(@RequestBody @Valid GenericConfigCreateRequest request);

    @PutMapping
    public Result<GenericConfigResponse> update(@RequestBody @Valid GenericConfigUpdateRequest request);

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id);

    @GetMapping("/{id}/history")
    public Result<List<GenericConfigHistoryResponse>> history(@PathVariable Long id);

    @GetMapping("/history/page")
    public Result<PageResponse<GenericConfigHistoryResponse>> historyPage(GenericConfigHistoryQueryRequest query);
}
```

---

## 十、前端设计

### 10.1 菜单与路由

在「后台配置」一级菜单下新增「通用配置」菜单项，路由建议 `/config/generic`。

进入通用配置页面后，顶部为 **Tab 切换栏**，包含两个标签页：

| Tab | 说明 |
|-----|------|
| **配置列表** | 默认激活，展示全部配置，支持搜索、新建、编辑、删除、查看单条历史 |
| **变更历史** | 全局变更历史，分页展示，支持按 key / 操作类型 / 操作人筛选 |

两个 Tab 共享同一个页面组件，切换时保持各自的状态（搜索条件、分页位置），用户可随时在两个 Tab 间切换。

### 10.2 配置列表 Tab

**布局**：

- 顶部搜索栏：配置键输入框 + 查询按钮 + 新建按钮。
- 表格列：配置键、值格式、配置值（截断展示，点击查看完整）、备注、创建人、创建时间、最后修改人、最后修改时间、操作列。
- 操作列按钮：编辑、删除（二次确认）、查看历史。
- 新建 / 编辑：弹窗形式，字段包括配置键（新建时可编辑，编辑时只读）、值格式（下拉：txt / json）、配置值（txt 为文本域，json 为代码编辑器）、备注。
- 配置值展示：json 格式时格式化展示，超长截断并提供弹窗查看完整内容。

### 10.3 变更历史 Tab

**布局**：

- 顶部筛选栏：配置键、操作类型（下拉：全部 / 新建 / 修改 / 删除）、操作人、查询按钮。
- 表格列：配置键、操作类型（带颜色标签）、变更前值（截断）、变更后值（截断）、变更摘要、操作人、操作时间。
- 分页：底部分页组件。
- 点击变更前值 / 变更后值可弹窗查看完整内容（json 格式化展示）。

### 10.4 前端组件建议

- JSON 编辑 / 展示：复用项目中已有的代码编辑器组件（如 Monaco Editor / CodeMirror）。
- 操作类型标签：用不同颜色区分（新建-绿色、修改-蓝色、删除-红色）。

---

## 十一、开发任务拆解

| 序号 | 任务 | 模块 |
|------|------|------|
| 1 | 建表 SQL：`generic_config` + `generic_config_history` | sql |
| 2 | 枚举：`ConfigValueFormatEnum` / `ConfigActionEnum` | common |
| 3 | 常量：`GenericConfigConstants` | common |
| 4 | Entity：`GenericConfig` / `GenericConfigHistory` | dao |
| 5 | Mapper + XML：`GenericConfigMapper` / `GenericConfigHistoryMapper` | dao |
| 6 | Repository：`GenericConfigRepository` / `GenericConfigHistoryRepository` | dao |
| 7 | Request / Response：create / update / list / history 相关 DTO | facade |
| 8 | Service 接口 + 实现：`GenericConfigService` / `GenericConfigServiceImpl` | service |
| 9 | Controller：`GenericConfigController` | service |
| 10 | 前端：配置列表页 + 新建编辑弹窗 | pipeline-frontend |
| 11 | 前端：变更历史页（全局 + 单条入口） | pipeline-frontend |
| 12 | 联调测试 | 全链路 |

---

## 十二、风险与注意事项

1. **config_key 唯一性**：数据库层使用普通索引（非唯一索引），Service 层在创建时校验是否存在**未删除**的同 key 记录。这样逻辑删除后可以重新创建相同 key 的配置，不会产生冲突。
2. **JSON 值序列化**：存储时统一序列化为字符串，返回时按 valueFormat 解析，保证 JSON 对象的 key 顺序稳定（使用 `Feature.OrderedField`）。
3. **历史表数据量**：长期运行后历史表数据量会增长，`idx_config_id` 和 `idx_operate_time` 索引保障查询效率。后续可视数据量考虑定期归档。
4. **大文本存储**：`config_value` 使用 `longtext`，前端展示时需截断，避免列表页渲染卡顿。
