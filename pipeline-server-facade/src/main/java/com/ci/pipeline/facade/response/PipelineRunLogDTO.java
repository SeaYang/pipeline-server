package com.ci.pipeline.facade.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 日志 SSE 推送 DTO（每次推送一个批次）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PipelineRunLogDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 本次推送的日志文本（增量内容） */
    private String content;

    /** 是否已全部推送完毕（Pod 结束或终态一次性返回） */
    private boolean completed;
}
