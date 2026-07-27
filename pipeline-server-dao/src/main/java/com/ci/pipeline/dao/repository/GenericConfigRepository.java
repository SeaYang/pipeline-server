package com.ci.pipeline.dao.repository;

import com.ci.pipeline.dao.entity.GenericConfig;
import com.ci.pipeline.dao.mapper.GenericConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GenericConfigRepository {

    @Autowired
    private GenericConfigMapper genericConfigMapper;

    public GenericConfig selectById(Long id) {
        return genericConfigMapper.selectById(id);
    }

    public GenericConfig getByKey(String configKey) {
        return genericConfigMapper.getByKey(configKey);
    }

    public List<GenericConfig> listBySearch(String configKey) {
        return genericConfigMapper.listBySearch(configKey);
    }

    public int insert(GenericConfig entity) {
        return genericConfigMapper.insert(entity);
    }

    public int updateById(GenericConfig entity) {
        return genericConfigMapper.updateById(entity);
    }

    /**
     * 逻辑删除（MyBatis-Plus 全局配置了 logic-delete-field=deleted，
     * deleteById 会自动转为 UPDATE SET deleted=1 WHERE id=? AND deleted=0）。
     */
    public int deleteById(Long id) {
        return genericConfigMapper.deleteById(id);
    }
}
