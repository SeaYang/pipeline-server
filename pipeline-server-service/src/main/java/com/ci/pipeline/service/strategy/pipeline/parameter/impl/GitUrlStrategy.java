package com.ci.pipeline.service.strategy.pipeline.parameter.impl;

import com.ci.pipeline.common.constants.AppInfoConstants;
import com.ci.pipeline.common.constants.PipelineConstants;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.dao.entity.AppInfo;
import com.ci.pipeline.dao.entity.PipelineParameter;
import com.ci.pipeline.dao.repository.AppInfoRepository;
import com.ci.pipeline.service.strategy.ParamResolveContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * git-url 参数策略：根据 appName 从 app_info 表获取 git_ssh_url。
 * <p>若应用不存在或 git_ssh_url 为空，抛出 {@link BusinessException}。
 */
@Slf4j
@Component("git-url")
public class GitUrlStrategy extends DefaultPipelineParameterStrategy {

    @Autowired
    private AppInfoRepository appInfoRepository;

    @Override
    public String buildParameter(PipelineParameter param, ParamResolveContext context) {
        if (context == null || context.getAppName() == null || context.getAppName().isEmpty()) {
            throw new BusinessException(AppInfoConstants.MSG_APP_NAME_REQUIRED);
        }
        AppInfo appInfo = appInfoRepository.selectByAppName(context.getAppName());
        if (appInfo == null) {
            throw new BusinessException(String.format(
                    PipelineConstants.MSG_APP_NOT_EXIST, context.getAppName()));
        }
        if (appInfo.getGitSshUrl() == null || appInfo.getGitSshUrl().isEmpty()) {
            throw new BusinessException(AppInfoConstants.MSG_GIT_SSH_URL_REQUIRED);
        }
        return appInfo.getGitSshUrl();
    }
}
