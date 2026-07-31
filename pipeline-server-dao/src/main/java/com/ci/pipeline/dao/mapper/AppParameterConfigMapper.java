package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ci.pipeline.dao.entity.AppParameterConfig;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 应用参数配置 Mapper
 */
public interface AppParameterConfigMapper extends BaseMapper<AppParameterConfig> {

    /** 按 appName + parameterName + env 精确查询（用于策略查询） */
    AppParameterConfig selectByAppParamEnv(@Param("appName") String appName,
                                           @Param("parameterName") String parameterName,
                                           @Param("env") String env);

    /** 按 appName + parameterName + env 计数（用于唯一性校验） */
    long countByAppParamEnv(@Param("appName") String appName,
                            @Param("parameterName") String parameterName,
                            @Param("env") String env,
                            @Param("excludeId") Long excludeId);

    /** 按 appName + env 查询列表 */
    List<AppParameterConfig> selectListByAppEnv(@Param("appName") String appName,
                                                @Param("env") String env);
}
