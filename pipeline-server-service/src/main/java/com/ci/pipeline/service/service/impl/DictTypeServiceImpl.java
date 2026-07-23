package com.ci.pipeline.service.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.common.constants.CommonConstants;
import com.ci.pipeline.common.constants.DictConstants;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.common.util.SortUtil;
import com.ci.pipeline.dao.entity.DictType;
import com.ci.pipeline.dao.repository.DictDataRepository;
import com.ci.pipeline.dao.repository.DictTypeRepository;
import com.ci.pipeline.facade.request.DictTypeCreateRequest;
import com.ci.pipeline.facade.request.DictTypeQueryRequest;
import com.ci.pipeline.facade.request.DictTypeUpdateRequest;
import com.ci.pipeline.facade.response.DictTypeResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.service.service.DictTypeService;
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
 * 字典类型业务实现
 */
@Slf4j
@Service
public class DictTypeServiceImpl implements DictTypeService {

    /**
     * 分页排序字段白名单：出参字段名（camelCase） → 数据库列名（snake_case）
     */
    private static final Map<String, String> SORT_FIELD_MAP;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("id", "id");
        m.put("dictType", "dict_type");
        m.put("dictName", "dict_name");
        m.put("remark", "remark");
        m.put("createTime", "create_time");
        m.put("updateTime", "update_time");
        SORT_FIELD_MAP = Collections.unmodifiableMap(m);
    }

    @Autowired
    private DictTypeRepository dictTypeRepository;

    @Autowired
    private DictDataRepository dictDataRepository;

    @Override
    public DictTypeResponse create(DictTypeCreateRequest request) {
        validateRequired(request);
        // 唯一性校验：dict_type 在未删除记录中唯一
        if (dictTypeRepository.countByDictType(request.getDictType(), null) > 0) {
            throw new BusinessException(String.format(DictConstants.MSG_DICT_TYPE_DUPLICATED, request.getDictType()));
        }
        DictType entity = new DictType();
        BeanUtils.copyProperties(request, entity);
        dictTypeRepository.insert(entity);
        log.info("新增字典类型成功, dictType={}, id={}", entity.getDictType(), entity.getId());
        return toResponse(dictTypeRepository.selectById(entity.getId()));
    }

    @Override
    public DictTypeResponse update(DictTypeUpdateRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(DictConstants.MSG_DICT_TYPE_ID_REQUIRED);
        }
        DictType existing = dictTypeRepository.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException(DictConstants.MSG_DICT_TYPE_NOT_EXIST);
        }
        // 若传入 dict_type，需校验非空
        if (request.getDictType() != null && !StringUtils.hasText(request.getDictType())) {
            throw new BusinessException(DictConstants.MSG_DICT_TYPE_CODE_REQUIRED);
        }
        // 唯一性校验：排除自身
        if (request.getDictType() != null
                && dictTypeRepository.countByDictType(request.getDictType(), request.getId()) > 0) {
            throw new BusinessException(String.format(DictConstants.MSG_DICT_TYPE_DUPLICATED, request.getDictType()));
        }
        DictType entity = new DictType();
        BeanUtils.copyProperties(request, entity);
        dictTypeRepository.updateById(entity);
        log.info("修改字典类型成功, id={}", request.getId());
        return toResponse(dictTypeRepository.selectById(request.getId()));
    }

    @Override
    public void deleteById(Long id) {
        DictType existing = dictTypeRepository.selectById(id);
        if (existing == null) {
            throw new BusinessException(DictConstants.MSG_DICT_TYPE_NOT_EXIST);
        }
        // 删除前校验：该字典类型下若存在字典数据，则禁止删除
        long dataCount = dictDataRepository.countByDictType(existing.getDictType());
        if (dataCount > 0) {
            throw new BusinessException(String.format(DictConstants.MSG_DICT_TYPE_HAS_DATA, existing.getDictType()));
        }
        dictTypeRepository.deleteById(id);
        log.info("删除字典类型成功, id={}, dictType={}", id, existing.getDictType());
    }

    @Override
    public DictTypeResponse getById(Long id) {
        DictType entity = dictTypeRepository.selectById(id);
        if (entity == null) {
            throw new BusinessException(DictConstants.MSG_DICT_TYPE_NOT_EXIST);
        }
        return toResponse(entity);
    }

    @Override
    public PageResponse<DictTypeResponse> page(DictTypeQueryRequest query) {
        long pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        long pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();
        // 解析排序字段（白名单映射）与方向（默认 desc）；sortField 为空时走默认排序
        String sortField = SortUtil.resolveField(query.getSortField(), SORT_FIELD_MAP);
        String sortOrder = sortField != null ? SortUtil.resolveOrder(query.getSortOrder()) : null;
        IPage<DictType> pageResult = dictTypeRepository.pageQuery(
                pageNum, pageSize, query.getDictType(), query.getDictName(), sortField, sortOrder);
        List<DictTypeResponse> records = pageResult.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(records, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize(), pageResult.getPages());
    }

    /**
     * 新增必填字段校验
     */
    private void validateRequired(DictTypeCreateRequest request) {
        if (!StringUtils.hasText(request.getDictType())) {
            throw new BusinessException(DictConstants.MSG_DICT_TYPE_CODE_REQUIRED);
        }
        if (!StringUtils.hasText(request.getDictName())) {
            throw new BusinessException(DictConstants.MSG_DICT_TYPE_NAME_REQUIRED);
        }
    }

    private DictTypeResponse toResponse(DictType entity) {
        if (entity == null) {
            return null;
        }
        DictTypeResponse response = new DictTypeResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
