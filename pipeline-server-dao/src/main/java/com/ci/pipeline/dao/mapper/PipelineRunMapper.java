package com.ci.pipeline.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ci.pipeline.dao.entity.PipelineRun;
import org.apache.ibatis.annotations.Param;

/**
 * 流水线执行记录 Mapper
 */
public interface PipelineRunMapper extends BaseMapper<PipelineRun> {

    /**
     * 分页查询流水线执行记录（支持 pipelineId / appName / status 精确过滤，支持按字段排序，默认按创建时间倒序）
     *
     * @param page        分页参数（由 MyBatis-Plus 分页插件自动改写）
     * @param pipelineId  流水线 id（精确，可为 null）
     * @param appName     应用名称（精确，可为 null）
     * @param status      执行状态（精确，可为 null）
     * @param sortField   排序列名（snake_case，已在 Service 层白名单校验，可为 null）
     * @param sortOrder   排序方向（asc / desc，可为 null）
     * @return 分页结果
     */
    IPage<PipelineRun> pageQuery(IPage<PipelineRun> page,
                                 @Param("pipelineId") Long pipelineId,
                                 @Param("appName") String appName,
                                 @Param("status") String status,
                                 @Param("sortField") String sortField,
                                 @Param("sortOrder") String sortOrder);

    /**
     * 按流水线 id 查询最近一次执行记录（id 最大的一条），无记录返回 null
     *
     * @param pipelineId 流水线 id
     * @return 最近一次执行记录，不存在返回 null
     */
    PipelineRun selectLatestByPipelineId(@Param("pipelineId") Long pipelineId);

    /**
     * 带乐观锁的状态回写：仅当 revision 与传入一致时更新成功并 revision 自增，
     * 用于异步轮询与兜底同步并发回写同一记录时的冲突控制。
     * <p>更新字段按需回写：status 必写；fail_type / fail_message / duration 仅在非 null 时写入，
     * 避免把非失败态/非终态的旧值清空。
     *
     * @param run 携带 id、当前 revision、待回写字段（status 等）的实体
     * @return 影响行数（1=成功，0=版本冲突或记录不存在）
     */
    int updateForSync(@Param("run") PipelineRun run);

    /**
     * 重试前的状态重置：将状态置回 Pending，并清空失败信息（fail_type / fail_message / duration），
     * 带乐观锁（revision）。
     *
     * @param id       执行记录 id
     * @param revision 当前 revision（乐观锁）
     * @return 影响行数（1=成功，0=版本冲突或记录不存在）
     */
    int resetForRetry(@Param("id") Long id, @Param("revision") Integer revision);
}
