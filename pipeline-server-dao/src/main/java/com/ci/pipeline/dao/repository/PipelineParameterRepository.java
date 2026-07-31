package com.ci.pipeline.dao.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.dao.entity.PipelineParameter;
import com.ci.pipeline.dao.mapper.PipelineParameterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class PipelineParameterRepository {

    @Autowired
    private PipelineParameterMapper pipelineParameterMapper;

    public PipelineParameter selectById(Long id) {
        return pipelineParameterMapper.selectById(id);
    }

    /**
     * 按参数名集合批量查询完整的参数定义实体（用于参数解析引擎）。
     *
     * @param names 参数名集合
     * @return 参数定义列表
     */
    public List<PipelineParameter> listByNames(Collection<String> names) {
        if (names == null || names.isEmpty()) {
            return Collections.emptyList();
        }
        return pipelineParameterMapper.selectList(
                new LambdaQueryWrapper<PipelineParameter>().in(PipelineParameter::getName, names));
    }

    /**
     * 查询所有未删除的参数定义名称集合。
     *
     * @return 参数名列表
     */
    public List<String> listAllNames() {
        List<PipelineParameter> list = pipelineParameterMapper.selectList(
                new LambdaQueryWrapper<PipelineParameter>().select(PipelineParameter::getName));
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(PipelineParameter::getName)
                .collect(Collectors.toList());
    }

    /**
     * 按参数名集合批量查询已配置的参数名（用于校验模板参数是否已定义）。
     *
     * @param names 待校验的参数名集合
     * @return 已配置的参数名集合
     */
    public List<String> listExistingNames(Collection<String> names) {
        if (names == null || names.isEmpty()) {
            return Collections.emptyList();
        }
        List<PipelineParameter> list = pipelineParameterMapper.selectList(
                new LambdaQueryWrapper<PipelineParameter>()
                        .select(PipelineParameter::getName)
                        .in(PipelineParameter::getName, names));
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream()
                .map(PipelineParameter::getName)
                .collect(Collectors.toList());
    }

    public PipelineParameter selectByName(String name) {
        return pipelineParameterMapper.selectByName(name);
    }

    public long countByName(String name, Long excludeId) {
        return pipelineParameterMapper.countByName(name, excludeId);
    }

    public IPage<PipelineParameter> pageQuery(long pageNum, long pageSize, String name, String label,
                                              String paramType, String paramGroup,
                                              String sortField, String sortOrder) {
        IPage<PipelineParameter> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        return pipelineParameterMapper.pageQuery(page, name, label, paramType, paramGroup, sortField, sortOrder);
    }

    /**
     * 查询所有未删除的参数定义（仅 name 和 label），用于依赖参数选择等场景。
     *
     * @return 参数定义列表（仅含 name、label）
     */
    public List<PipelineParameter> listAllSimple() {
        return pipelineParameterMapper.selectList(
                new LambdaQueryWrapper<PipelineParameter>()
                        .select(PipelineParameter::getName, PipelineParameter::getLabel));
    }

    /**
     * 查询指定组件类型且 param_type 匹配的参数定义（用于应用参数配置的可选参数列表）。
     *
     * @param componentTypes 组件类型集合
     * @param paramType      参数类型
     * @return 参数定义列表
     */
    public List<PipelineParameter> listByComponentTypesAndParamType(Collection<String> componentTypes, String paramType) {
        if (componentTypes == null || componentTypes.isEmpty()) {
            return Collections.emptyList();
        }
        return pipelineParameterMapper.selectList(
                new LambdaQueryWrapper<PipelineParameter>()
                        .select(PipelineParameter::getName,
                                PipelineParameter::getLabel,
                                PipelineParameter::getComponentType,
                                PipelineParameter::getParamType,
                                PipelineParameter::getOptionConfig)
                        .in(PipelineParameter::getComponentType, componentTypes)
                        .eq(PipelineParameter::getParamType, paramType));
    }

    public int insert(PipelineParameter entity) {
        return pipelineParameterMapper.insert(entity);
    }

    public int updateById(PipelineParameter entity) {
        return pipelineParameterMapper.updateById(entity);
    }

    public int deleteById(Long id) {
        return pipelineParameterMapper.deleteById(id);
    }
}
