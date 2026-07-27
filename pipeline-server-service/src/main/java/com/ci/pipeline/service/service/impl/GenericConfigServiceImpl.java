package com.ci.pipeline.service.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.common.auth.UserContext;
import com.ci.pipeline.common.constants.CommonConstants;
import com.ci.pipeline.common.constants.GenericConfigConstants;
import com.ci.pipeline.common.enums.ConfigActionEnum;
import com.ci.pipeline.common.enums.ConfigValueFormatEnum;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.dao.entity.GenericConfig;
import com.ci.pipeline.dao.entity.GenericConfigHistory;
import com.ci.pipeline.dao.repository.GenericConfigHistoryRepository;
import com.ci.pipeline.dao.repository.GenericConfigRepository;
import com.ci.pipeline.facade.request.GenericConfigCreateRequest;
import com.ci.pipeline.facade.request.GenericConfigHistoryQueryRequest;
import com.ci.pipeline.facade.request.GenericConfigUpdateRequest;
import com.ci.pipeline.facade.response.GenericConfigHistoryResponse;
import com.ci.pipeline.facade.response.GenericConfigResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.service.service.GenericConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GenericConfigServiceImpl implements GenericConfigService {

    @Autowired
    private GenericConfigRepository genericConfigRepository;

    @Autowired
    private GenericConfigHistoryRepository genericConfigHistoryRepository;

    // ============================== 查询 ==============================

    @Override
    public List<GenericConfigResponse> list(String configKey) {
        List<GenericConfig> list = genericConfigRepository.listBySearch(configKey);
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public GenericConfigResponse getById(Long id) {
        GenericConfig entity = genericConfigRepository.selectById(id);
        if (entity == null) {
            throw new BusinessException(GenericConfigConstants.MSG_NOT_FOUND);
        }
        return toResponse(entity);
    }

    @Override
    public Object getValueByKey(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            throw new BusinessException(GenericConfigConstants.MSG_KEY_REQUIRED);
        }
        GenericConfig entity = genericConfigRepository.getByKey(configKey.trim());
        if (entity == null) {
            throw new BusinessException(GenericConfigConstants.MSG_NOT_FOUND);
        }
        return parseValue(entity.getConfigValue(), entity.getValueFormat());
    }

    // ============================== 新建 ==============================

    @Override
    public GenericConfigResponse create(GenericConfigCreateRequest request) {
        String userId = requireLogin();

        // 校验必填字段
        validateKeyRequired(request.getConfigKey());
        validateKeyLength(request.getConfigKey());
        validateValueRequired(request.getConfigValue());

        // 校验值格式
        validateValueFormat(request.getValueFormat());

        // 唯一性校验（仅校验未删除的记录）
        GenericConfig exist = genericConfigRepository.getByKey(request.getConfigKey());
        if (exist != null) {
            throw new BusinessException(String.format(
                    GenericConfigConstants.MSG_KEY_DUPLICATED, request.getConfigKey()));
        }

        // 序列化配置值
        String storedValue = serializeValue(request.getConfigValue(), request.getValueFormat());

        GenericConfig entity = new GenericConfig();
        entity.setConfigKey(request.getConfigKey());
        entity.setConfigValue(storedValue);
        entity.setValueFormat(request.getValueFormat());
        entity.setDescription(request.getDescription());
        entity.setCreator(userId);
        entity.setUpdater(userId);
        entity.setDeleted(0);
        genericConfigRepository.insert(entity);
        log.info("新建配置成功, key={}, id={}", entity.getConfigKey(), entity.getId());

        // 记录变更历史
        recordHistory(entity, ConfigActionEnum.CREATE, null, null, null, null,
                "新建配置", userId);

        return toResponse(genericConfigRepository.selectById(entity.getId()));
    }

    // ============================== 修改 ==============================

    @Override
    public GenericConfigResponse update(GenericConfigUpdateRequest request) {
        String userId = requireLogin();

        if (request.getId() == null) {
            throw new BusinessException(GenericConfigConstants.MSG_ID_REQUIRED);
        }

        GenericConfig existing = genericConfigRepository.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException(GenericConfigConstants.MSG_NOT_FOUND);
        }

        validateValueRequired(request.getConfigValue());
        validateValueFormat(request.getValueFormat());

        // 序列化新值
        String newStoredValue = serializeValue(request.getConfigValue(), request.getValueFormat());

        // 判断配置值是否有变化（只有值变化才记录变更历史）
        boolean valueChanged = !Objects.equals(existing.getConfigValue(), newStoredValue);

        // 判断其他字段是否有变化
        boolean formatChanged = !Objects.equals(existing.getValueFormat(), request.getValueFormat());
        boolean descChanged = !Objects.equals(existing.getDescription(), request.getDescription());

        // 无任何变化则直接提示
        if (!valueChanged && !formatChanged && !descChanged) {
            throw new BusinessException(GenericConfigConstants.MSG_NO_CHANGE);
        }

        // 更新主表（值、格式、备注均更新）
        GenericConfig update = new GenericConfig();
        update.setId(existing.getId());
        update.setConfigValue(newStoredValue);
        update.setValueFormat(request.getValueFormat());
        update.setDescription(request.getDescription());
        update.setUpdater(userId);
        genericConfigRepository.updateById(update);
        log.info("修改配置成功, key={}, id={}, valueChanged={}", existing.getConfigKey(), existing.getId(), valueChanged);

        // 只有配置值变化才记录变更历史；格式和备注变更只更新主表，不记录历史
        if (valueChanged) {
            recordHistory(existing, ConfigActionEnum.UPDATE,
                    existing.getConfigValue(), newStoredValue,
                    existing.getValueFormat(), request.getValueFormat(),
                    "配置值已修改", userId);
        }

        return toResponse(genericConfigRepository.selectById(existing.getId()));
    }

    // ============================== 删除 ==============================

    @Override
    public void delete(Long id) {
        String userId = requireLogin();

        GenericConfig existing = genericConfigRepository.selectById(id);
        if (existing == null) {
            throw new BusinessException(GenericConfigConstants.MSG_NOT_FOUND);
        }

        // 逻辑删除
        GenericConfig update = new GenericConfig();
        update.setId(existing.getId());
        update.setDeleted(1);
        genericConfigRepository.updateById(update);
        log.info("删除配置成功, key={}, id={}", existing.getConfigKey(), id);

        // 记录变更历史
        recordHistory(existing, ConfigActionEnum.DELETE,
                existing.getConfigValue(), null,
                existing.getValueFormat(), null,
                "删除配置", userId);
    }

    // ============================== 变更历史 ==============================

    @Override
    public List<GenericConfigHistoryResponse> historyByConfigId(Long configId) {
        List<GenericConfigHistory> list = genericConfigHistoryRepository.listByConfigId(configId);
        return list.stream().map(this::toHistoryResponse).collect(Collectors.toList());
    }

    @Override
    public PageResponse<GenericConfigHistoryResponse> historyPage(GenericConfigHistoryQueryRequest query) {
        long pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        long pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();

        IPage<GenericConfigHistory> pageResult = genericConfigHistoryRepository.pageQuery(
                pageNum, pageSize, query.getConfigKey(), query.getAction(), query.getOperator());

        List<GenericConfigHistoryResponse> records = pageResult.getRecords().stream()
                .map(this::toHistoryResponse)
                .collect(Collectors.toList());

        return PageResponse.of(records, pageResult.getTotal(), pageResult.getCurrent(),
                pageResult.getSize(), pageResult.getPages());
    }

    // ============================== 私有辅助方法 ==============================

    /**
     * 校验值格式是否合法。
     */
    private void validateValueFormat(String valueFormat) {
        if (!ConfigValueFormatEnum.isValidCode(valueFormat)) {
            throw new BusinessException(String.format(
                    GenericConfigConstants.MSG_FORMAT_UNSUPPORTED, valueFormat));
        }
    }

    /**
     * 校验配置键非空。
     */
    private void validateKeyRequired(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            throw new BusinessException(GenericConfigConstants.MSG_KEY_REQUIRED);
        }
    }

    /**
     * 校验配置键长度。
     */
    private void validateKeyLength(String configKey) {
        if (configKey.length() > GenericConfigConstants.KEY_MAX_LENGTH) {
            throw new BusinessException(String.format(
                    GenericConfigConstants.MSG_KEY_TOO_LONG, GenericConfigConstants.KEY_MAX_LENGTH));
        }
    }

    /**
     * 校验配置值非空。
     */
    private void validateValueRequired(Object configValue) {
        if (configValue == null) {
            throw new BusinessException(GenericConfigConstants.MSG_VALUE_REQUIRED);
        }
    }

    /**
     * 将前端传入的值序列化为存储字符串。
     * <p>json 格式时校验合法性并序列化（保持 key 有序）；txt 格式时直接转字符串。
     */
    private String serializeValue(Object value, String valueFormat) {
        if (ConfigValueFormatEnum.JSON.getCode().equals(valueFormat)) {
            validateJson(value);
            return JSONObject.toJSONString(value, SerializerFeature.SortField);
        }
        return value.toString();
    }

    /**
     * 校验值是否为合法的 JSON 对象或数组。
     */
    private void validateJson(Object value) {
        String jsonStr = JSONObject.toJSONString(value);
        try {
            JSONObject.parseObject(jsonStr);
        } catch (Exception e1) {
            try {
                JSONArray.parseArray(jsonStr);
            } catch (Exception e2) {
                throw new BusinessException(GenericConfigConstants.MSG_JSON_INVALID);
            }
        }
    }

    /**
     * 将存储的字符串解析为返回值。
     * <p>json 格式时解析为有序 JSONObject / JSONArray；txt 格式时直接返回字符串。
     */
    private Object parseValue(String storedValue, String valueFormat) {
        if (storedValue == null) {
            return null;
        }
        if (ConfigValueFormatEnum.JSON.getCode().equals(valueFormat)) {
            try {
                Object parsed = JSONObject.parse(storedValue);
                return parsed;
            } catch (Exception e) {
                // 存储的 JSON 解析失败，降级返回原始字符串
                return storedValue;
            }
        }
        return storedValue;
    }

    /**
     * 记录变更历史。
     */
    private void recordHistory(GenericConfig config, ConfigActionEnum action,
                               String oldValue, String newValue,
                               String oldValueFormat, String newValueFormat,
                               String changeSummary, String operator) {
        GenericConfigHistory history = new GenericConfigHistory();
        history.setConfigId(config.getId());
        history.setConfigKey(config.getConfigKey());
        history.setAction(action.getCode());
        history.setOldValue(oldValue);
        history.setNewValue(newValue);
        history.setOldValueFormat(oldValueFormat);
        history.setNewValueFormat(newValueFormat);
        history.setChangeSummary(changeSummary);
        history.setOperator(operator);
        history.setOperateTime(new Date());
        genericConfigHistoryRepository.insert(history);
    }

    /**
     * 要求用户已登录，返回用户ID。
     */
    private String requireLogin() {
        String userId = UserContext.getUserId();
        if (!StringUtils.hasText(userId)) {
            throw new BusinessException("用户未登录");
        }
        return userId;
    }

    /**
     * Entity → Response 转换。
     */
    private GenericConfigResponse toResponse(GenericConfig entity) {
        GenericConfigResponse response = new GenericConfigResponse();
        response.setId(entity.getId());
        response.setConfigKey(entity.getConfigKey());
        response.setConfigValue(parseValue(entity.getConfigValue(), entity.getValueFormat()));
        response.setValueFormat(entity.getValueFormat());
        response.setDescription(entity.getDescription());
        response.setCreator(entity.getCreator());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdater(entity.getUpdater());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }

    /**
     * History Entity → History Response 转换。
     */
    private GenericConfigHistoryResponse toHistoryResponse(GenericConfigHistory entity) {
        GenericConfigHistoryResponse response = new GenericConfigHistoryResponse();
        response.setId(entity.getId());
        response.setConfigId(entity.getConfigId());
        response.setConfigKey(entity.getConfigKey());
        response.setAction(entity.getAction());
        response.setOldValue(parseValue(entity.getOldValue(), entity.getOldValueFormat()));
        response.setNewValue(parseValue(entity.getNewValue(), entity.getNewValueFormat()));
        response.setOldValueFormat(entity.getOldValueFormat());
        response.setNewValueFormat(entity.getNewValueFormat());
        response.setChangeSummary(entity.getChangeSummary());
        response.setOperator(entity.getOperator());
        response.setOperateTime(entity.getOperateTime());
        return response;
    }
}
