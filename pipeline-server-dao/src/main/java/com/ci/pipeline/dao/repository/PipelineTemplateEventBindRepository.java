package com.ci.pipeline.dao.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ci.pipeline.dao.entity.PipelineTemplateEventBind;
import com.ci.pipeline.dao.mapper.PipelineTemplateEventBindMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 事件-模板绑定数据访问，封装 Mapper 调用。
 */
@Repository
public class PipelineTemplateEventBindRepository {

    @Autowired
    private PipelineTemplateEventBindMapper mapper;

    /**
     * 新增
     */
    public int insert(PipelineTemplateEventBind entity) {
        return mapper.insert(entity);
    }

    /**
     * 根据主键查询（仅未删除）
     */
    public PipelineTemplateEventBind selectById(Long id) {
        return mapper.selectById(id);
    }

    /**
     * 根据主键更新（null 字段不参与更新）
     */
    public int updateById(PipelineTemplateEventBind entity) {
        return mapper.updateById(entity);
    }

    /**
     * 根据主键逻辑删除
     */
    public int deleteById(Long id) {
        return mapper.deleteById(id);
    }

    /**
     * 根据事件类型查询所有绑定的模板编码（仅未删除）
     */
    public List<String> listTemplateCodesByEventType(String eventType) {
        List<PipelineTemplateEventBind> list = mapper.selectList(
                new LambdaQueryWrapper<PipelineTemplateEventBind>()
                        .eq(PipelineTemplateEventBind::getEventType, eventType)
                        .orderByAsc(PipelineTemplateEventBind::getId));
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(PipelineTemplateEventBind::getPipelineTemplateCode)
                .collect(Collectors.toList());
    }

    /**
     * 统计指定事件类型 + 模板编码的记录数（用于唯一性校验）
     *
     * @param eventType           事件类型
     * @param pipelineTemplateCode 流水线模板编码
     * @param excludeId           排除的主键，可为 null
     */
    public long countByEventTypeAndTemplateCode(String eventType, String pipelineTemplateCode, Long excludeId) {
        return mapper.selectCount(
                new LambdaQueryWrapper<PipelineTemplateEventBind>()
                        .eq(PipelineTemplateEventBind::getEventType, eventType)
                        .eq(PipelineTemplateEventBind::getPipelineTemplateCode, pipelineTemplateCode)
                        .ne(excludeId != null, PipelineTemplateEventBind::getId, excludeId));
    }

    /**
     * 分页查询（支持事件类型精确过滤，支持按字段排序）。
     * <p>sortField 为 Service 层白名单映射后的 snake_case 列名，sortOrder 限定为 asc / desc。
     */
    public IPage<PipelineTemplateEventBind> pageQuery(long pageNum, long pageSize,
                                                       String eventType,
                                                       String sortField, String sortOrder) {
        Page<PipelineTemplateEventBind> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<PipelineTemplateEventBind> wrapper = new LambdaQueryWrapper<>();
        if (eventType != null && !eventType.isEmpty()) {
            wrapper.eq(PipelineTemplateEventBind::getEventType, eventType);
        }
        if (sortField != null && !sortField.isEmpty()) {
            // sortField / sortOrder 已在 Service 层白名单校验，安全拼接
            wrapper.last("ORDER BY " + sortField + " " + sortOrder + ", id DESC");
        } else {
            wrapper.orderByDesc(PipelineTemplateEventBind::getId);
        }
        return mapper.selectPage(page, wrapper);
    }
}
