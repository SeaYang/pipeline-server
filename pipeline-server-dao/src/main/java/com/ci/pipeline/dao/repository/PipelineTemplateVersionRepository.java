package com.ci.pipeline.dao.repository;

import com.ci.pipeline.dao.entity.PipelineTemplateVersion;
import com.ci.pipeline.dao.mapper.PipelineTemplateVersionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 流水线模板版本数据访问，封装 Mapper 调用，为 Service 层提供数据访问能力
 */
@Repository
public class PipelineTemplateVersionRepository {

    @Autowired
    private PipelineTemplateVersionMapper pipelineTemplateVersionMapper;

    /**
     * 根据主键查询（仅未删除）
     */
    public PipelineTemplateVersion selectById(Long id) {
        return pipelineTemplateVersionMapper.selectById(id);
    }

    /**
     * 根据流水线模板编码 + 版本号查询（仅未删除）
     */
    public PipelineTemplateVersion selectByCodeAndVersion(String pipelineTemplateCode, String version) {
        return pipelineTemplateVersionMapper.selectByCodeAndVersion(pipelineTemplateCode, version);
    }

    /**
     * 查询指定流水线模板编码下唯一的一个「生效中」版本（仅未删除），不存在返回 null。
     */
    public PipelineTemplateVersion selectEffectiveByCode(String pipelineTemplateCode) {
        return pipelineTemplateVersionMapper.selectEffectiveByCode(pipelineTemplateCode);
    }

    /**
     * 查询全部「生效中」的流水线模板版本（新集群接入全量同步用）
     */
    public List<PipelineTemplateVersion> listAllEffective() {
        return pipelineTemplateVersionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PipelineTemplateVersion>()
                        .eq(PipelineTemplateVersion::getStatus, "EFFECTIVE"));
    }

    /**
     * 统计指定流水线模板编码下的版本记录数（用于删除模板前的占用校验）
     */
    public long countByCode(String pipelineTemplateCode) {
        return pipelineTemplateVersionMapper.countByCode(pipelineTemplateCode);
    }

    /**
     * 查询指定流水线模板编码下的全部版本号（仅 version 列，用于递增校验求最大值）
     */
    public List<String> listVersionsByCode(String pipelineTemplateCode) {
        return pipelineTemplateVersionMapper.listVersionsByCode(pipelineTemplateCode);
    }

    /**
     * 查询指定流水线模板编码下的全部版本（含完整字段，用于版本列表）。SQL 不排序。
     */
    public List<PipelineTemplateVersion> listByCode(String pipelineTemplateCode) {
        return pipelineTemplateVersionMapper.listByCode(pipelineTemplateCode);
    }

    /**
     * 更新单个版本的状态（按流水线模板编码 + 版本号定位）
     */
    public int updateStatusByCodeAndVersion(String pipelineTemplateCode, String version, String status) {
        return pipelineTemplateVersionMapper.updateStatusByCodeAndVersion(pipelineTemplateCode, version, status);
    }

    /**
     * 某版本生效时，把指定模板下、除指定版本外、尚未失效的其它版本（生效中/草稿）统一置为已失效。
     */
    public int updateOtherStatusToExpired(String pipelineTemplateCode, String excludeVersion) {
        return pipelineTemplateVersionMapper.updateOtherStatusToExpired(pipelineTemplateCode, excludeVersion);
    }

    /**
     * 新增
     */
    public int insert(PipelineTemplateVersion entity) {
        return pipelineTemplateVersionMapper.insert(entity);
    }

    /**
     * 根据主键更新（null 字段不参与更新）
     */
    public int updateById(PipelineTemplateVersion entity) {
        return pipelineTemplateVersionMapper.updateById(entity);
    }

    /**
     * 根据主键逻辑删除
     */
    public int deleteById(Long id) {
        return pipelineTemplateVersionMapper.deleteById(id);
    }
}
