package com.ci.pipeline.dao.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ci.pipeline.dao.entity.GenericConfigHistory;
import com.ci.pipeline.dao.mapper.GenericConfigHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GenericConfigHistoryRepository {

    @Autowired
    private GenericConfigHistoryMapper genericConfigHistoryMapper;

    public int insert(GenericConfigHistory entity) {
        return genericConfigHistoryMapper.insert(entity);
    }

    public List<GenericConfigHistory> listByConfigId(Long configId) {
        return genericConfigHistoryMapper.listByConfigId(configId);
    }

    public IPage<GenericConfigHistory> pageQuery(long pageNum, long pageSize,
                                                  String configKey, String action, String operator) {
        IPage<GenericConfigHistory> page = new Page<>(pageNum, pageSize);
        return genericConfigHistoryMapper.pageQuery(page, configKey, action, operator);
    }
}
