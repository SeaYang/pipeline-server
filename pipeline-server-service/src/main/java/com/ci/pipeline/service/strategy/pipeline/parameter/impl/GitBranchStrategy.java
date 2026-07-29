package com.ci.pipeline.service.strategy.pipeline.parameter.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ci.pipeline.dao.entity.PipelineParameter;
import com.ci.pipeline.facade.response.GitBranchResponse;
import com.ci.pipeline.service.service.GitLabService;
import com.ci.pipeline.service.strategy.ParamResolveContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * git-branch 参数策略：从 GitLab 获取真实分支列表，填充 optionConfig。
 * <p>若默认值不在分支列表中，则清空默认值，避免前端展示无效选项。
 */
@Slf4j
@Component("git-branch")
public class GitBranchStrategy extends DefaultPipelineParameterStrategy {

    @Autowired
    private GitLabService gitLabService;

    @Override
    public String buildParameter(PipelineParameter param, ParamResolveContext context) {
        // 先走默认策略链获取基础值
        String defaultValue = super.buildParameter(param, context);

        // 从 GitLab 获取真实分支列表，填充 optionConfig
        if (context != null && context.getAppName() != null && !context.getAppName().isEmpty()) {
            try {
                List<GitBranchResponse> branches = gitLabService.listBranches(context.getAppName());
                List<String> branchNames = branches.stream()
                        .map(GitBranchResponse::getName)
                        .collect(Collectors.toList());

                // 将分支列表序列化为 optionConfig JSON：[{"value":"main","label":"main"}, ...]
                JSONArray optionArr = new JSONArray();
                for (String name : branchNames) {
                    JSONObject option = new JSONObject();
                    option.put("value", name);
                    option.put("label", name);
                    optionArr.add(option);
                }
                param.setOptionConfig(optionArr.toJSONString());

                // 校验默认值是否在分支列表中，不在则清空
                if (defaultValue != null && !branchNames.contains(defaultValue)) {
                    log.info("git-branch 默认值[{}]不在分支列表{}中，已清空", defaultValue, branchNames);
                    param.setDefaultValue(null);
                    return null;
                }
            } catch (Exception e) {
                log.warn("获取 GitLab 分支列表失败，appName={}", context.getAppName(), e);
            }
        }
        return defaultValue;
    }
}
