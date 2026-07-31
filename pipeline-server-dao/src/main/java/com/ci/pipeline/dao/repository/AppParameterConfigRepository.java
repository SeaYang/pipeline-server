package com.ci.pipeline.dao.repository;

import com.ci.pipeline.dao.entity.AppParameterConfig;
import com.ci.pipeline.dao.mapper.AppParameterConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 应用参数配置数据访问，封装 Mapper 调用
 */
@Repository
public class AppParameterConfigRepository {

    @Autowired
    private AppParameterConfigMapper appParameterConfigMapper;

    public AppParameterConfig selectById(Long id) {
        return appParameterConfigMapper.selectById(id);
    }

    public AppParameterConfig selectByAppParamEnv(String appName, String parameterName, String env) {
        return appParameterConfigMapper.selectByAppParamEnv(appName, parameterName, env);
    }

    public long countByAppParamEnv(String appName, String parameterName, String env, Long excludeId) {
        return appParameterConfigMapper.countByAppParamEnv(appName, parameterName, env, excludeId);
    }

    public List<AppParameterConfig> selectListByAppEnv(String appName, String env) {
        return appParameterConfigMapper.selectListByAppEnv(appName, env);
    }

    public int insert(AppParameterConfig entity) {
        return appParameterConfigMapper.insert(entity);
    }

    public int updateById(AppParameterConfig entity) {
        return appParameterConfigMapper.updateById(entity);
    }

    public int deleteById(Long id) {
        return appParameterConfigMapper.deleteById(id);
    }
}
