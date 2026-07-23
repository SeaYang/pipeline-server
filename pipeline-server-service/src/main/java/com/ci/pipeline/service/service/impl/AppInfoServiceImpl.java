package com.ci.pipeline.service.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.common.constants.AppInfoConstants;
import com.ci.pipeline.common.constants.CommonConstants;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.common.util.SortUtil;
import com.ci.pipeline.dao.entity.AppInfo;
import com.ci.pipeline.dao.repository.AppInfoRepository;
import com.ci.pipeline.facade.request.AppInfoCreateRequest;
import com.ci.pipeline.facade.request.AppInfoQueryRequest;
import com.ci.pipeline.facade.request.AppInfoUpdateRequest;
import com.ci.pipeline.facade.response.AppInfoResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.service.service.AppInfoService;
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
 * 应用基础信息业务实现
 */
@Slf4j
@Service
public class AppInfoServiceImpl implements AppInfoService {

    /**
     * 分页排序字段白名单：出参字段名（camelCase） → 数据库列名（snake_case）
     */
    private static final Map<String, String> SORT_FIELD_MAP;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("id", "id");
        m.put("appName", "app_name");
        m.put("programmingLanguage", "programming_language");
        m.put("description", "description");
        m.put("gitSshUrl", "git_ssh_url");
        m.put("createTime", "create_time");
        m.put("updateTime", "update_time");
        SORT_FIELD_MAP = Collections.unmodifiableMap(m);
    }

    @Autowired
    private AppInfoRepository appInfoRepository;

    @Override
    public AppInfoResponse create(AppInfoCreateRequest request) {
        validateRequired(request);
        // 唯一性校验：app_name 在未删除记录中唯一
        if (appInfoRepository.countByAppName(request.getAppName(), null) > 0) {
            throw new BusinessException(String.format(
                    AppInfoConstants.MSG_APP_NAME_DUPLICATED, request.getAppName()));
        }
        AppInfo entity = new AppInfo();
        BeanUtils.copyProperties(request, entity);
        appInfoRepository.insert(entity);
        log.info("新增应用成功, appName={}, id={}", entity.getAppName(), entity.getId());
        return toResponse(appInfoRepository.selectById(entity.getId()));
    }

    @Override
    public AppInfoResponse update(AppInfoUpdateRequest request) {
        if (request.getId() == null) {
            throw new BusinessException(AppInfoConstants.MSG_APP_ID_REQUIRED);
        }
        AppInfo existing = appInfoRepository.selectById(request.getId());
        if (existing == null) {
            throw new BusinessException(AppInfoConstants.MSG_APP_NOT_EXIST);
        }
        // 若传入 app_name，需校验非空
        if (request.getAppName() != null && !StringUtils.hasText(request.getAppName())) {
            throw new BusinessException(AppInfoConstants.MSG_APP_NAME_REQUIRED);
        }
        // 唯一性校验：排除自身
        if (request.getAppName() != null
                && appInfoRepository.countByAppName(request.getAppName(), request.getId()) > 0) {
            throw new BusinessException(String.format(
                    AppInfoConstants.MSG_APP_NAME_DUPLICATED, request.getAppName()));
        }
        AppInfo entity = new AppInfo();
        BeanUtils.copyProperties(request, entity);
        appInfoRepository.updateById(entity);
        log.info("修改应用成功, id={}", request.getId());
        return toResponse(appInfoRepository.selectById(request.getId()));
    }

    @Override
    public void deleteById(Long id) {
        AppInfo existing = appInfoRepository.selectById(id);
        if (existing == null) {
            throw new BusinessException(AppInfoConstants.MSG_APP_NOT_EXIST);
        }
        appInfoRepository.deleteById(id);
        log.info("删除应用成功, id={}, appName={}", id, existing.getAppName());
    }

    @Override
    public AppInfoResponse getById(Long id) {
        AppInfo entity = appInfoRepository.selectById(id);
        if (entity == null) {
            throw new BusinessException(AppInfoConstants.MSG_APP_NOT_EXIST);
        }
        return toResponse(entity);
    }

    @Override
    public PageResponse<AppInfoResponse> page(AppInfoQueryRequest query) {
        long pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        long pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();
        // 解析排序字段（白名单映射）与方向（默认 desc）；sortField 为空时走默认排序
        String sortField = SortUtil.resolveField(query.getSortField(), SORT_FIELD_MAP);
        String sortOrder = sortField != null ? SortUtil.resolveOrder(query.getSortOrder()) : null;
        IPage<AppInfo> pageResult = appInfoRepository.pageQuery(
                pageNum, pageSize, query.getAppName(), sortField, sortOrder);
        List<AppInfoResponse> records = pageResult.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResponse.of(records, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize(), pageResult.getPages());
    }

    /**
     * 新增必填字段校验
     */
    private void validateRequired(AppInfoCreateRequest request) {
        if (!StringUtils.hasText(request.getAppName())) {
            throw new BusinessException(AppInfoConstants.MSG_APP_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(request.getProgrammingLanguage())) {
            throw new BusinessException(AppInfoConstants.MSG_PROGRAMMING_LANGUAGE_REQUIRED);
        }
        if (!StringUtils.hasText(request.getGitSshUrl())) {
            throw new BusinessException(AppInfoConstants.MSG_GIT_SSH_URL_REQUIRED);
        }
    }

    private AppInfoResponse toResponse(AppInfo entity) {
        if (entity == null) {
            return null;
        }
        AppInfoResponse response = new AppInfoResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
