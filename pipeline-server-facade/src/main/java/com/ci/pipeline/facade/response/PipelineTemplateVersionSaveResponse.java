package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 流水线模板版本保存响应。
 * <p>无论参数校验是否通过都返回 200，前端根据 {@link #undefinedParams} 自行判断：
 * <ul>
 *   <li>空列表 / null：全部参数已定义，保存成功；</li>
 *   <li>非空列表：存在未定义参数，{@link #version} 为 null，前端弹框提示用户去配置。</li>
 * </ul>
 */
@Data
public class PipelineTemplateVersionSaveResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 保存成功时返回的版本详情；存在未定义参数时为 null */
    private PipelineTemplateVersionResponse version;

    /** 未在参数定义表中配置的参数名列表；为空表示全部已定义 */
    private List<String> undefinedParams;

    public static PipelineTemplateVersionSaveResponse ok(PipelineTemplateVersionResponse version) {
        PipelineTemplateVersionSaveResponse resp = new PipelineTemplateVersionSaveResponse();
        resp.setVersion(version);
        return resp;
    }

    public static PipelineTemplateVersionSaveResponse undefined(List<String> undefinedParams) {
        PipelineTemplateVersionSaveResponse resp = new PipelineTemplateVersionSaveResponse();
        resp.setUndefinedParams(undefinedParams);
        return resp;
    }
}
