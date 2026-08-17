package com.ci.pipeline.dao.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ci.pipeline.dao.entity.ClusterInfo;
import com.ci.pipeline.dao.mapper.ClusterInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 执行集群定义 Repository（瘦封装，MP wrapper 查询自动追加逻辑删除条件）
 */
@Repository
public class ClusterInfoRepository {

    @Autowired
    private ClusterInfoMapper clusterInfoMapper;

    /**
     * 分页查询（wrapper 由调用方构造）
     */
    public IPage<ClusterInfo> pageQuery(long pageNum, long pageSize, LambdaQueryWrapper<ClusterInfo> wrapper) {
        return clusterInfoMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    public ClusterInfo selectById(Long id) {
        return clusterInfoMapper.selectById(id);
    }

    /**
     * 按集群名查询（未删除记录）
     */
    public ClusterInfo selectByClusterName(String clusterName) {
        return clusterInfoMapper.selectOne(new LambdaQueryWrapper<ClusterInfo>()
                .eq(ClusterInfo::getClusterName, clusterName)
                .last("LIMIT 1"));
    }

    /**
     * 查询全部未删除集群，按 id 升序（保证默认集群优先排在前面，行为稳定）
     */
    public List<ClusterInfo> listAll() {
        return clusterInfoMapper.selectList(new LambdaQueryWrapper<ClusterInfo>()
                .orderByAsc(ClusterInfo::getId));
    }

    /**
     * 查询启用（enabled=1）的集群
     */
    public List<ClusterInfo> listEnabled() {
        return clusterInfoMapper.selectList(new LambdaQueryWrapper<ClusterInfo>()
                .eq(ClusterInfo::getEnabled, 1)
                .orderByAsc(ClusterInfo::getId));
    }

    /**
     * 查询默认集群（is_default=1）
     */
    public ClusterInfo selectDefault() {
        return clusterInfoMapper.selectOne(new LambdaQueryWrapper<ClusterInfo>()
                .eq(ClusterInfo::getIsDefault, 1)
                .last("LIMIT 1"));
    }

    /**
     * 清除全部默认集群标记（设置新默认集群前调用，事务内先清后设保证全局唯一）
     */
    public int clearDefaultMark() {
        ClusterInfo update = new ClusterInfo();
        update.setIsDefault(0);
        return clusterInfoMapper.update(update, new LambdaQueryWrapper<ClusterInfo>()
                .eq(ClusterInfo::getIsDefault, 1));
    }

    public int insert(ClusterInfo entity) {
        return clusterInfoMapper.insert(entity);
    }

    public int updateById(ClusterInfo entity) {
        return clusterInfoMapper.updateById(entity);
    }

    /**
     * 逻辑删除（MyBatis-Plus 全局配置了 logic-delete-field=deleted，
     * deleteById 会自动转为 UPDATE SET deleted=1 WHERE id=? AND deleted=0）。
     */
    public int deleteById(Long id) {
        return clusterInfoMapper.deleteById(id);
    }
}
