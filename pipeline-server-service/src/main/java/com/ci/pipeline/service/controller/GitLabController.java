package com.ci.pipeline.service.controller;

import com.ci.pipeline.common.auth.RequireLogin;
import com.ci.pipeline.common.result.Result;
import com.ci.pipeline.facade.request.GitTreeQueryRequest;
import com.ci.pipeline.facade.response.GitBranchResponse;
import com.ci.pipeline.facade.response.GitTreeNodeResponse;
import com.ci.pipeline.service.service.GitLabService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GitLab 相关接口控制器。
 * 提供分支列表查询、目录树懒加载查询。
 */
@Slf4j
@RestController
@RequestMapping("/gitlab")
@RequireLogin
public class GitLabController {

    @Autowired
    private GitLabService gitLabService;

    /**
     * 查询仓库分支列表。
     *
     * @param appName 应用名称
     */
    @GetMapping("/branches")
    public Result<List<GitBranchResponse>> listBranches(@RequestParam("appName") String appName) {
        return Result.success(gitLabService.listBranches(appName));
    }

    /**
     * 查询仓库目录树（懒加载单层）。
     *
     * @param appName 应用名称
     * @param path    查询路径，为空时查根目录
     */
    @GetMapping("/tree")
    public Result<List<GitTreeNodeResponse>> listTree(
            @RequestParam("appName") String appName,
            @RequestParam(value = "path", required = false) String path) {
        GitTreeQueryRequest request = new GitTreeQueryRequest();
        request.setAppName(appName);
        request.setPath(path);
        return Result.success(gitLabService.listTree(request));
    }
}
