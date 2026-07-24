-- =============================================================================
-- 事件触发模块 - 字典初始化数据
-- =============================================================================
-- 说明：事件类型使用已有的 dict_type / dict_data 表管理，dict_type 固定为 pipeline_event_type。
-- =============================================================================

-- 确保字典类型存在
INSERT INTO `dict_type` (`dict_type`, `dict_name`, `remark`)
VALUES ('pipeline_event_type', '流水线触发事件类型', '定义所有支持的触发事件编码')
ON DUPLICATE KEY UPDATE `dict_name` = VALUES(`dict_name`), `remark` = VALUES(`remark`);

-- 事件类型数据
INSERT INTO `dict_data` (`dict_type`, `dict_key`, `dict_value`, `dict_sort`, `remark`, `enabled`)
VALUES ('pipeline_event_type', 'epTestApply', '效能平台提测', 1, '效能平台提测事件', 1)
ON DUPLICATE KEY UPDATE `dict_value` = VALUES(`dict_value`), `remark` = VALUES(`remark`);
