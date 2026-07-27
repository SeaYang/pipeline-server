package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ci.pipeline.dao.entity.GenericConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GenericConfigMapper extends BaseMapper<GenericConfig> {

    /**
     * 查询全部未删除的配置，支持按 configKey 模糊搜索。
     *
     * @param configKey 配置键（可选，模糊匹配）
     * @return 配置列表
     */
    List<GenericConfig> listBySearch(@Param("configKey") String configKey);

    /**
     * 按配置键精确查询未删除的配置。
     *
     * @param configKey 配置键
     * @return 配置实体，不存在返回 null
     */
    GenericConfig getByKey(@Param("configKey") String configKey);
}
