package com.ci.pipeline.service.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.common.constants.ArtifactConstants;
import com.ci.pipeline.common.constants.CommonConstants;
import com.ci.pipeline.common.enums.ArtifactTypeEnum;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.common.util.SortUtil;
import com.ci.pipeline.dao.entity.Artifact;
import com.ci.pipeline.dao.entity.PipelineRun;
import com.ci.pipeline.dao.repository.ArtifactRepository;
import com.ci.pipeline.dao.repository.PipelineRunRepository;
import com.ci.pipeline.facade.request.ArtifactQueryRequest;
import com.ci.pipeline.facade.request.ArtifactUploadRequest;
import com.ci.pipeline.facade.response.ArtifactResponse;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.service.service.ArtifactService;
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

@Slf4j
@Service
public class ArtifactServiceImpl implements ArtifactService {

    /** 排序字段白名单：camelCase → snake_case */
    private static final Map<String, String> SORT_FIELD_MAP;
    static {
        Map<String, String> m = new HashMap<>();
        m.put("id", "id");
        m.put("appName", "app_name");
        m.put("name", "name");
        m.put("type", "type");
        m.put("gitBranch", "git_branch");
        m.put("env", "env");
        m.put("buildTime", "build_time");
        m.put("buildUser", "build_user");
        m.put("pipelineRunId", "pipeline_run_id");
        m.put("pipelineRunName", "pipeline_run_name");
        m.put("size", "size");
        m.put("sha256", "sha256");
        m.put("createTime", "create_time");
        m.put("updateTime", "update_time");
        SORT_FIELD_MAP = SortUtil.unmodifiableWhitelist(m);
    }

    @Autowired
    private ArtifactRepository artifactRepository;

    @Autowired
    private PipelineRunRepository pipelineRunRepository;

    @Override
    public Long upload(ArtifactUploadRequest request) {
        // 参数校验
        validateUploadRequest(request);

        Artifact entity = new Artifact();
        BeanUtils.copyProperties(request, entity);

        // 通过 pipelineRunName 反查 pipeline_run 记录，填充 pipelineRunId 和 buildUser
        if (StringUtils.hasText(request.getPipelineRunName())) {
            PipelineRun run = pipelineRunRepository.selectByName(request.getPipelineRunName());
            if (run != null) {
                entity.setPipelineRunId(run.getId());
                entity.setBuildUser(run.getCreator());
            }
        }

        artifactRepository.insert(entity);
        log.info("制品上传成功, appName={}, name={}, type={}, id={}",
                entity.getAppName(), entity.getName(), entity.getType(), entity.getId());
        return entity.getId();
    }

    @Override
    public ArtifactResponse getById(Long id) {
        Artifact entity = artifactRepository.selectById(id);
        if (entity == null) {
            throw new BusinessException(ArtifactConstants.MSG_ARTIFACT_NOT_EXIST);
        }
        return toResponse(entity);
    }

    @Override
    public PageResponse<ArtifactResponse> page(ArtifactQueryRequest query) {
        long pageNum = query.getPageNum() == null ? CommonConstants.DEFAULT_PAGE_NUM : query.getPageNum();
        long pageSize = query.getPageSize() == null ? CommonConstants.DEFAULT_PAGE_SIZE : query.getPageSize();
        // 默认按 id 倒序
        String sortField = SortUtil.resolveField(
                query.getSortField() != null ? query.getSortField() : "id", SORT_FIELD_MAP);
        String sortOrder = sortField != null
                ? SortUtil.resolveOrder(query.getSortOrder() != null ? query.getSortOrder() : "desc")
                : null;

        IPage<Artifact> pageResult = artifactRepository.pageQuery(
                pageNum, pageSize, query.getAppName(), query.getName(),
                query.getGitBranch(), query.getEnv(), query.getType(), sortField, sortOrder);

        List<ArtifactResponse> records = pageResult.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(records, pageResult.getTotal(), pageResult.getCurrent(),
                pageResult.getSize(), pageResult.getPages());
    }

    @Override
    public List<ArtifactResponse> listByPipelineRunName(String pipelineRunName) {
        if (!StringUtils.hasText(pipelineRunName)) {
            return Collections.emptyList();
        }
        return artifactRepository.selectByPipelineRunName(pipelineRunName).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private void validateUploadRequest(ArtifactUploadRequest request) {
        if (!StringUtils.hasText(request.getAppName())) {
            throw new BusinessException("应用名称不能为空");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException(ArtifactConstants.MSG_ARTIFACT_NAME_REQUIRED);
        }
        if (!StringUtils.hasText(request.getType())) {
            throw new BusinessException(ArtifactConstants.MSG_ARTIFACT_TYPE_REQUIRED);
        }
        if (!ArtifactTypeEnum.isValidCode(request.getType())) {
            throw new BusinessException(ArtifactConstants.MSG_ARTIFACT_TYPE_INVALID);
        }
    }

    private ArtifactResponse toResponse(Artifact entity) {
        if (entity == null) return null;
        ArtifactResponse response = new ArtifactResponse();
        BeanUtils.copyProperties(entity, response);
        return response;
    }
}
