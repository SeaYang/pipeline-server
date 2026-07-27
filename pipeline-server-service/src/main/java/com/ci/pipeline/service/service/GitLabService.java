package com.ci.pipeline.service.service;

import com.ci.pipeline.facade.request.GitTreeQueryRequest;
import com.ci.pipeline.facade.response.GitBranchResponse;
import com.ci.pipeline.facade.response.GitTreeNodeResponse;

import java.util.List;

/**
 * GitLab 业务接口。
 * 负责 SSH URL 解析、appName→repoId 反查、调用 GitLabAgent 并组装响应。
 */
public interface GitLabService {

    /**
     * 根据 gitSshUrl 查询 GitLab 仓库 repoId。
     * 供 AppInfoServiceImpl 在创建/修改应用时调用。
     *
     * @param gitUrl SSH 格式的 git 仓库地址，如 git@gitlab.com:SeaYang2/go-web-demo.git
     * @return GitLab 仓库 ID
     */
    Long getRepoId(String gitUrl);

    /**
     * 根据 appName 查询仓库全部分支列表。
     *
     * @param appName 应用名称
     * @return 分支列表
     */
    List<GitBranchResponse> listBranches(String appName);

    /**
     * 根据 appName + path 查询目录树（懒加载单层）。
     *
     * @param request 查询请求（appName + path）
     * @return 目录树节点列表
     */
    List<GitTreeNodeResponse> listTree(GitTreeQueryRequest request);
}
