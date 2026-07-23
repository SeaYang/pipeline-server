package com.ci.pipeline.service.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.common.constants.CommonConstants;
import com.ci.pipeline.common.constants.DictConstants;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.common.util.SortUtil;
import com.ci.pipeline.dao.entity.DictData;
import com.ci.pipeline.dao.entity.DictType;
import com.ci.pipeline.dao.repository.DictDataRepository;
import com.ci.pipeline.dao.repository.DictTypeRepository;
import com.ci.pipeline.facade.request.DictDataCreateRequest;
import com.ci.pipeline.facade.request.DictDataQueryRequest;
import com.ci.pipeline.facade.request.DictDataUpdateRequest;
import com.ci.pipeline.facade.response.DictDataResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.service.service.DictDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 字典数据业务实现
 */
@Slf4j
@Service
public class DictDataServiceImpl implements DictDataService {

    /**
     * 分页排序字段白名单：出参字段名（camelCase） → 数据库列名（snake_case）
     */
    private static final Map<String, String> SORT_FIELD_MAP;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("id", "id");
        m.put("dictType", "dict_type");
        m.put("dictKey", "dict_key");
        m.put("dictValue", "dict_value");
        m.put("dictSort", "dict_sort");
        m.put("remark", "remark");
        m.put("enabled", "enabled");
        m.put("createTime", "create_time");
        m.put("updateTime", "update_time");
        SORT_FIELD_MAP = Collections.unmodifiableMap(m);
    }

    @Autowired
    private DictDataRepository dictDataRepository;

    @Autowired
    private DictTypeRepository dictTypeRepository;

    @Override
    public DictDataResponse create(DictDataCreateRequest request) {
        validateRequired(request);
        // 字典类型必须存在
        if (dictTypeRepository.selectByDictType(request.getDictType()) == null) {
            throw new BusinessException(DictConstants.MSG_DICT_TYPE_NOT_EXIST);
        }
        // 唯一性校验：dict_type + dict_key 在未删除记录中唯一
        if (dictDataRepository.countByTypeAndKey(request.getDictType(), request.getDictKey(), null) > 0) {
            throw new BusinessException(String.format(
                    DictConstants.MSG_DICT_DATA_DUPLICATED, request.getDictType(), request.getDictKey()));
        }
        DictData entity = new DictData();
        BeanUtils.copyProperties(request, entity);
        normalize(entity);
        dictDataRepository.insert(entity);
        log.info("新增字典数据成功, dictType={}, dictKey={}, id={}", entity.getDictType(), entity.getDictKey(), entity.getId());
        return toResponse(dictDataRepository.selectById(entity.getId()));
    }

    @Override
    public DictDataResponse update(DictDataUpdateRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(DictConstants.MSG_DICT_DATA_ID_REQUIRED);
        }
        DictData existing = dictDataRepository.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException(DictConstants.MSG_DICT_DATA_NOT_EXIST);
        }
        // 计算生效的 dict_type / dict_key（未传入则沿用原值）
        String targetDictType = request.getDictType() != null ? request.getDictType() : existing.getDictType();
        String targetKey = request.getDictKey() != null ? request.getDictKey() : existing.getDictKey();
        // 字典类型必须存在
        if (dictTypeRepository.selectByDictType(targetDictType) == null) {
            throw new BusinessException(DictConstants.MSG_DICT_TYPE_NOT_EXIST);
        }
        // 唯一性校验：排除自身
        if (dictDataRepository.countByTypeAndKey(targetDictType, targetKey, request.getId()) > 0) {
            throw new BusinessException(String.format(
                    DictConstants.MSG_DICT_DATA_DUPLICATED, targetDictType, targetKey));
        }
        DictData entity = new DictData();
        BeanUtils.copyProperties(request, entity);
        dictDataRepository.updateById(entity);
        log.info("修改字典数据成功, id={}", request.getId());
        return toResponse(dictDataRepository.selectById(request.getId()));
    }

    @Override
    public void deleteById(Long id) {
        DictData existing = dictDataRepository.selectById(id);
        if (existing == null) {
            throw new BusinessException(DictConstants.MSG_DICT_DATA_NOT_EXIST);
        }
        dictDataRepository.deleteById(id);
        log.info("删除字典数据成功, id={}, dictType={}, dictKey={}", id, existing.getDictType(), existing.getDictKey());
    }

    @Override
    public DictDataResponse getById(Long id) {
        DictData entity = dictDataRepository.selectById(id);
        if (entity == null) {
            throw new BusinessException(DictConstants.MSG_DICT_DATA_NOT_EXIST);
        }
        return toResponse(entity);
    }

    @Override
    public List<DictDataResponse> listByDictType(String dictType) {
        if (!StringUtils.hasText(dictType)) {
            throw new BusinessException(DictConstants.MSG_DICT_TYPE_CODE_REQUIRED);
        }
        return dictDataRepository.listByDictType(dictType).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<DictDataResponse> page(DictDataQueryRequest query) {
        long pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        long pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();
        // 解析排序字段（白名单映射）与方向（默认 desc）；sortField 为空时走默认排序
        String sortField = SortUtil.resolveField(query.getSortField(), SORT_FIELD_MAP);
        String sortOrder = sortField != null ? SortUtil.resolveOrder(query.getSortOrder()) : null;
        IPage<DictData> pageResult = dictDataRepository.pageQuery(
                pageNum, pageSize, query.getDictType(), query.getDictKey(), query.getDictValue(), sortField, sortOrder);
        List<DictDataResponse> records = pageResult.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(records, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize(), pageResult.getPages());
    }

    /**
     * 新增必填字段校验
     */
    private void validateRequired(DictDataCreateRequest request) {
        if (!StringUtils.hasText(request.getDictType())) {
            throw new BusinessException(DictConstants.MSG_DICT_TYPE_CODE_REQUIRED);
        }
        if (!StringUtils.hasText(request.getDictKey())) {
            throw new BusinessException(DictConstants.MSG_DICT_KEY_REQUIRED);
        }
        if (!StringUtils.hasText(request.getDictValue())) {
            throw new BusinessException(DictConstants.MSG_DICT_VALUE_REQUIRED);
        }
    }

    /**
     * 规整 NOT NULL 字段的默认值（dict_sort、remark、enabled）
     */
    private void normalize(DictData entity) {
        if (entity.getDictSort() == null) {
            entity.setDictSort(DictConstants.DEFAULT_SORT);
        }
        if (!StringUtils.hasText(entity.getRemark())) {
            entity.setRemark("");
        }
        if (entity.getEnabled() == null) {
            entity.setEnabled(false);
        }
    }

    private DictDataResponse toResponse(DictData entity) {
        if (entity == null) {
            return null;
        }
        DictDataResponse response = new DictDataResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
