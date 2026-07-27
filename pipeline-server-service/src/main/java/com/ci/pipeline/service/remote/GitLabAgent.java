package com.ci.pipeline.service.remote;

import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.TreeItem;

import java.util.List;

/**
 * GitLab SDK 封装接口。
 * 封装 GitLab4J-API 调用，统一异常处理。
 */
public interface GitLabAgent {

    /**
     * 根据 namespace/project 路径查询仓库信息。
     * 用于应用创建/修改时，根据 gitSshUrl 解析出的 projectPath 查询 repoId。
     *
     * @param projectPath namespace/project，如 "SeaYang2/go-web-demo"
     * @return GitLab Project 对象（含 id、name、webUrl 等）
     */
    Project getProject(String projectPath);

    /**
     * 根据 repoId 查询仓库全部分支（含最近 commit）。
     *
     * @param repoId GitLab 仓库数字 ID（从 app_info 表获取）
     * @return 分支列表
     */
    List<Branch> getBranches(Long repoId);

    /**
     * 根据 repoId 查询指定路径的单层目录树（懒加载）。
     *
     * @param repoId GitLab 仓库数字 ID（从 app_info 表获取）
     * @param path   查询路径，空串或 null 表示根目录
     * @return 目录树节点列表
     */
    List<TreeItem> getRepositoryTree(Long repoId, String path);
}
