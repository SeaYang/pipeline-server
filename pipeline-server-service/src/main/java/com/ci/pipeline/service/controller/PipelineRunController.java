package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.PipelineRunQueryRequest;
import com.ci.pipeline.facade.response.PageResponse;
import com.ci.pipeline.facade.response.PipelineRunExecuteDetailResponse;
import com.ci.pipeline.facade.response.PipelineRunResponse;
import com.ci.pipeline.facade.response.PipelineRunSnapshotResponse;
import com.ci.pipeline.service.service.PipelineRunService;
import com.ci.pipeline.service.service.sse.PipelineRunLogService;
import com.ci.pipeline.service.service.sse.PipelineRunSseService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 流水线执行记录控制器
 */
@Slf4j
@RestController
@RequestMapping("/pipeline-run")
@RequireLogin
public class PipelineRunController {

    @Autowired
    private PipelineRunService pipelineRunService;

    @Autowired
    private PipelineRunSseService pipelineRunSseService;

    @Autowired
    private PipelineRunLogService pipelineRunLogService;

    /**
     * 根据主键查询执行记录
     */
    @GetMapping("/{id}")
    public Result<PipelineRunResponse> get(@PathVariable("id") Long id) {
        return Result.success(pipelineRunService.getById(id));
    }

    /**
     * 分页查询执行记录（支持 pipelineId / appName / status 过滤，默认按创建时间倒序）
     */
    @GetMapping("/page")
    public Result<PageResponse<PipelineRunResponse>> page(PipelineRunQueryRequest query) {
        return Result.success(pipelineRunService.page(query));
    }

    /**
     * 查询流水线最近一次执行记录（id 最大的一条），无执行记录返回 null
     */
    @GetMapping("/latest")
    public Result<PipelineRunResponse> latest(@RequestParam("pipelineId") Long pipelineId) {
        return Result.success(pipelineRunService.getLatestByPipelineId(pipelineId));
    }

    /**
     * 手动兜底状态同步：异步轮询中断（实例下线/发布）时，人工触发重新进入异步状态同步逻辑，直到终态。
     */
    @PostMapping("/{id}/sync")
    public Result<PipelineRunResponse> sync(@PathVariable("id") Long id) {
        return Result.success(pipelineRunService.syncRun(id));
    }

    /**
     * 重试执行（仅 Failed / Error 状态可重试）
     */
    @PostMapping("/{id}/retry")
    public Result<PipelineRunResponse> retry(@PathVariable("id") Long id) {
        return Result.success(pipelineRunService.retry(id));
    }

    /**
     * 停止执行（终止 Argo Workflow 并置为 Cancelled）
     */
    @PostMapping("/{id}/stop")
    public Result<PipelineRunResponse> stop(@PathVariable("id") Long id) {
        return Result.success(pipelineRunService.stop(id));
    }

    /**
     * 查询执行详情快照（结构同前端 go-cicd-workflow.json，供 vue-flow 渲染）
     */
    @GetMapping("/{id}/detail")
    public Result<JsonNode> detail(@PathVariable("id") Long id) {
        return Result.success(pipelineRunService.getDetail(id));
    }

    /**
     * 查询执行详情（Argo Workflow 实时数据 + 任务编码→中文名映射）
     */
    @GetMapping("/execute-detail")
    public Result<PipelineRunExecuteDetailResponse> executeDetail(@RequestParam("pipelineRunName") String pipelineRunName) {
        return Result.success(pipelineRunService.getExecuteDetail(pipelineRunName));
    }

    /**
     * 查询流水线执行快照（pipeline_run_snapshot.detail 原始 JSON 字符串）。
     * <p>无快照时返回错误提示。
     *
     * @param pipelineRunId 流水线执行记录 id
     */
    @GetMapping("/{id}/snapshot")
    public Result<PipelineRunSnapshotResponse> snapshot(@PathVariable("id") Long id) {
        return Result.success(pipelineRunService.getSnapshot(id));
    }

    /**
     * SSE 订阅执行详情：服务端定时（5s）轮询 DB，revision 变化时推送最新数据，无变化发心跳。
     * <p>非终态数据来自 snapshot，终态由模板+task_run 组装。前端用 EventSource 订阅。
     *
     * @param pipelineRunName 流水线执行名称（Argo Workflow 名称）
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sse(@RequestParam("pipelineRunName") String pipelineRunName) {
        log.info("SSE 订阅, pipelineRunName={}", pipelineRunName);
        return pipelineRunSseService.subscribe(pipelineRunName);
    }

    /**
     * 查询任务节点日志（一次性获取）。
     * <p>非终态调 k8s 实时获取，终态从 pipeline_task_run.log_content 取。
     *
     * @param pipelineRunName 流水线执行名称
     * @param taskCode        任务节点编码
     */
    @GetMapping("/task-log")
    public Result<String> taskLog(@RequestParam("pipelineRunName") String pipelineRunName,
                                  @RequestParam("taskCode") String taskCode) {
        return Result.success(pipelineRunLogService.getTaskLog(pipelineRunName, taskCode));
    }

    /**
     * SSE 订阅任务节点日志：服务端持续推送增量日志，Pod 结束或终态后自动关闭。
     * <p>非终态用 k8s follow=true 流式读取；终态从 DB 一次性取。
     *
     * @param pipelineRunName 流水线执行名称
     * @param taskCode        任务节点编码
     */
    @GetMapping(value = "/log/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter watchLog(@RequestParam("pipelineRunName") String pipelineRunName,
                               @RequestParam("taskCode") String taskCode) {
        log.info("日志 SSE 订阅, pipelineRunName={}, taskCode={}", pipelineRunName, taskCode);
        return pipelineRunLogService.watchLog(pipelineRunName, taskCode);
    }
}
