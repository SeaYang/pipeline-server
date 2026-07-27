package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.dao.entity.GenericConfigHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GenericConfigHistoryMapper extends BaseMapper<GenericConfigHistory> {

    /**
     * 按配置ID查询变更历史，按操作时间倒序。
     *
     * @param configId 配置ID
     * @return 历史列表
     */
    List<GenericConfigHistory> listByConfigId(@Param("configId") Long configId);

    /**
     * 分页查询全局变更历史，支持按配置键、操作类型、操作人过滤。
     *
     * @param page      分页参数
     * @param configKey 配置键（可选，模糊匹配）
     * @param action    操作类型（可选，精确匹配）
     * @param operator  操作人（可选，模糊匹配）
     * @return 分页结果
     */
    IPage<GenericConfigHistory> pageQuery(IPage<GenericConfigHistory> page,
                                          @Param("configKey") String configKey,
                                          @Param("action") String action,
                                          @Param("operator") String operator);
}
