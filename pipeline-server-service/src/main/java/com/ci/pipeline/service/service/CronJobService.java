package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.CronJobCreateRequest;
import com.ci.pipeline.facade.request.CronJobLogQueryRequest;
import com.ci.pipeline.facade.request.CronJobQueryRequest;
import com.ci.pipeline.facade.request.CronJobUpdateRequest;
import com.ci.pipeline.facade.response.CronJobLogResponse;
import com.ci.pipeline.facade.response.CronJobResponse;
import com.ci.pipeline.facade.response.PageResponse;

public interface CronJobService {

    CronJobResponse create(CronJobCreateRequest request);

    CronJobResponse update(CronJobUpdateRequest request);

    void delete(Long id);

    CronJobResponse getById(Long id);

    PageResponse<CronJobResponse> page(CronJobQueryRequest query);

    void enable(Long id);

    void disable(Long id);

    /**
     * 手动触发任务立即执行一次。
     *
     * @return 本次执行对应的执行日志ID
     */
    Long triggerManually(Long id);

    /**
     * 停止指定执行日志对应的任务。
     * <p>会先在数据库层面用 CAS 把状态标记为终态，再尽力通知实际执行任务的实例中断线程
     * （同实例直接调用，跨实例通过 InternalHttpClient 通知）。
     *
     * @return 该记录当时是否处于 running 状态（true 表示确实触发了停止流程）
     */
    boolean stopLog(Long logId);

    CronJobLogResponse getLogById(Long id);

    PageResponse<CronJobLogResponse> logPage(CronJobLogQueryRequest query);
}
