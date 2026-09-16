package com.zifang.z.wf.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

/**
 * 分页结果封装
 *
 * @author zifang
 * @since 1.0.0
 */
@Schema(description = "分页结果")
public class PageResult<T> {

    @Schema(description = "当前页码")
    private Integer pageNum; // 当前页码（从1开始）

    @Schema(description = "每页大小")
    private Integer pageSize; // 每页记录数

    @Schema(description = "总记录数")
    private Long total; // 总记录数

    @Schema(description = "总页数")
    private Integer pages; // 总页数

    @Schema(description = "数据列表")
    private List<T> list; // 分页数据列表

    public PageResult() {
    }

    public static <T> PageResult<T> of(List<T> list, long total, int pageNum, int pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setPages((int) Math.ceil((double) total / pageSize));
        return result;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }

        if (o == null || getClass() != o.getClass()) return false;

        PageResult<?> that = (PageResult<?>) o;
        return Objects.equals(pageNum, that.pageNum) && Objects.equals(pageSize, that.pageSize) && Objects.equals(total, that.total) && Objects.equals(pages, that.pages) && Objects.equals(list, that.list);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageNum, pageSize, total, pages, list);
    }

    @Override
    public String toString() {
        return "PageResult{" +
                "pageNum=" + pageNum +
                ", pageSize=" + pageSize +
                ", total=" + total +
                ", pages=" + pages +
                ", list=" + list +
                '}';
    }
}
