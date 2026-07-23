package com.ci.pipeline.facade.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果通用封装
 *
 * @param <T> 数据类型
 */
@Data
public class PageResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 当前页码
     */
    private long current;

    /**
     * 每页大小
     */
    private long size;

    /**
     * 总页数
     */
    private long pages;

    public static <T> PageResponse<T> of(List<T> records, long total, long current, long size, long pages) {
        PageResponse<T> result = new PageResponse<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setCurrent(current);
        result.setSize(size);
        result.setPages(pages);
        return result;
    }
}
