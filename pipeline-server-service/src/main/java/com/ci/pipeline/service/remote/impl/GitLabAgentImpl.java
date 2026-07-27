package com.ci.pipeline.service.remote.impl;

import com.ci.pipeline.common.constants.GitLabConstants;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.service.remote.GitLabAgent;
import com.ci.pipeline.service.service.GenericConfigService;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.TreeItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * GitLab SDK 封装实现。
 * 每次调用时实时读取 GenericConfig 配置创建 GitLabApi 实例，
 * 因为域名和 token 可能随时变更，不做固定初始化。
 */
@Slf4j
@Component
public class GitLabAgentImpl implements GitLabAgent {

    @Autowired
    private GenericConfigService genericConfigService;

    /**
     * 实时读取配置创建 GitLabApi 客户端。
     * GitLabApi 实例轻量，每次创建开销可忽略。
     */
    private GitLabApi createClient() {
        String url = (String) genericConfigService.getValueByKey(GitLabConstants.CONFIG_KEY_API_URL);
        String token = (String) genericConfigService.getValueByKey(GitLabConstants.CONFIG_KEY_API_TOKEN);
        return new GitLabApi(url, token);
    }

    @Override
    public Project getProject(String projectPath) {
        log.info("查询 GitLab 仓库信息, projectPath={}", projectPath);
        try (GitLabApi gitLabApi = createClient()) {
            Project project = gitLabApi.getProjectApi().getProject(projectPath);
            log.info("查询 GitLab 仓库信息成功, projectPath={}, repoId={}", projectPath, project.getId());
            return project;
        } catch (GitLabApiException e) {
            log.error("查询 GitLab 仓库信息失败, projectPath={}, code={}, body={}",
                    projectPath, e.getHttpStatus(), e.getMessage(), e);
            throw new BusinessException(String.format(GitLabConstants.MSG_GET_PROJECT_FAILED, e.getMessage()));
        }
    }

    @Override
    public List<Branch> getBranches(Long repoId) {
        log.info("查询 GitLab 分支列表, repoId={}", repoId);
        try (GitLabApi gitLabApi = createClient()) {
            List<Branch> branches = gitLabApi.getRepositoryApi().getBranches(repoId);
            log.info("查询 GitLab 分支列表成功, repoId={}, count={}", repoId, branches.size());
            return branches;
        } catch (GitLabApiException e) {
            log.error("查询 GitLab 分支列表失败, repoId={}, code={}, body={}",
                    repoId, e.getHttpStatus(), e.getMessage(), e);
            throw new BusinessException(String.format(GitLabConstants.MSG_GET_BRANCHES_FAILED, e.getMessage()));
        }
    }

    @Override
    public List<TreeItem> getRepositoryTree(Long repoId, String path) {
        log.info("查询 GitLab 目录树, repoId={}, path={}", repoId, path);
        try (GitLabApi gitLabApi = createClient()) {
            List<TreeItem> tree = gitLabApi.getRepositoryApi().getTree(repoId, path, null);
            log.info("查询 GitLab 目录树成功, repoId={}, path={}, count={}", repoId, path, tree.size());
            return tree;
        } catch (GitLabApiException e) {
            log.error("查询 GitLab 目录树失败, repoId={}, path={}, code={}, body={}",
                    repoId, path, e.getHttpStatus(), e.getMessage(), e);
            throw new BusinessException(String.format(GitLabConstants.MSG_GET_TREE_FAILED, e.getMessage()));
        }
    }
}
