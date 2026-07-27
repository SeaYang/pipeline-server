package com.ci.pipeline.service.service.impl;

import com.ci.pipeline.common.constants.GitLabConstants;
import com.ci.pipeline.common.exception.BusinessException;
import com.ci.pipeline.dao.entity.AppInfo;
import com.ci.pipeline.dao.repository.AppInfoRepository;
import com.ci.pipeline.facade.request.GitTreeQueryRequest;
import com.ci.pipeline.facade.response.GitBranchResponse;
import com.ci.pipeline.facade.response.GitTreeNodeResponse;
import com.ci.pipeline.service.remote.GitLabAgent;
import com.ci.pipeline.service.service.GitLabService;
import lombok.extern.slf4j.Slf4j;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Commit;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.TreeItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * GitLab 业务实现。
 * 负责 SSH URL 解析、appName→repoId 反查、调用 GitLabAgent 并组装响应。
 */
@Slf4j
@Service
public class GitLabServiceImpl implements GitLabService {

    /** SSH URL 解析正则预编译 */
    private static final Pattern GIT_SSH_URL_PATTERN = Pattern.compile(GitLabConstants.GIT_SSH_URL_REGEX);

    @Autowired
    private GitLabAgent gitLabAgent;

    @Autowired
    private AppInfoRepository appInfoRepository;

    @Override
    public Long getRepoId(String gitUrl) {
        if (!StringUtils.hasText(gitUrl)) {
            throw new BusinessException(GitLabConstants.MSG_INVALID_GIT_SSH_URL);
        }
        String projectPath = parseProjectPath(gitUrl);
        Project project = gitLabAgent.getProject(projectPath);
        return project.getId();
    }

    @Override
    public List<GitBranchResponse> listBranches(String appName) {
        Long repoId = getRepoIdByAppName(appName);
        List<Branch> branches = gitLabAgent.getBranches(repoId);
        return branches.stream()
                .map(this::toBranchResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<GitTreeNodeResponse> listTree(GitTreeQueryRequest request) {
        Long repoId = getRepoIdByAppName(request.getAppName());
        String path = StringUtils.hasText(request.getPath()) ? request.getPath() : "";
        List<TreeItem> tree = gitLabAgent.getRepositoryTree(repoId, path);
        return tree.stream()
                .map(this::toTreeNodeResponse)
                .collect(Collectors.toList());
    }

    // ===== 内部方法 =====

    /**
     * 解析 SSH URL，提取 namespace/project 路径。
     * 输入：git@gitlab.com:SeaYang2/go-web-demo.git
     * 输出：SeaYang2/go-web-demo
     */
    private String parseProjectPath(String gitUrl) {
        Matcher matcher = GIT_SSH_URL_PATTERN.matcher(gitUrl.trim());
        if (!matcher.matches()) {
            throw new BusinessException(GitLabConstants.MSG_INVALID_GIT_SSH_URL);
        }
        return matcher.group(2);
    }

    /**
     * 根据 appName 反查 app_info 表获取 repoId。
     */
    private Long getRepoIdByAppName(String appName) {
        if (!StringUtils.hasText(appName)) {
            throw new BusinessException(GitLabConstants.MSG_APP_NOT_FOUND);
        }
        AppInfo appInfo = appInfoRepository.selectByAppName(appName.trim());
        if (appInfo == null) {
            throw new BusinessException(String.format(GitLabConstants.MSG_APP_NOT_FOUND, appName));
        }
        if (appInfo.getRepoId() == null) {
            throw new BusinessException(String.format(GitLabConstants.MSG_REPO_ID_NOT_FOUND, appName));
        }
        return appInfo.getRepoId();
    }

    /**
     * Branch → GitBranchResponse
     */
    private GitBranchResponse toBranchResponse(Branch branch) {
        GitBranchResponse response = new GitBranchResponse();
        response.setName(branch.getName());
        Commit commit = branch.getCommit();
        if (commit != null) {
            response.setCommitId(commit.getId());
            response.setCommitMessage(commit.getMessage());
            response.setAuthorName(commit.getAuthorName());
            response.setCommittedDate(commit.getCommittedDate());
        }
        return response;
    }

    /**
     * TreeItem → GitTreeNodeResponse
     */
    private GitTreeNodeResponse toTreeNodeResponse(TreeItem item) {
        GitTreeNodeResponse response = new GitTreeNodeResponse();
        response.setName(item.getName());
        response.setPath(item.getPath());
        response.setType(item.getType() != null ? item.getType().toString() : null);
        response.setMode(item.getMode());
        return response;
    }
}
