package com.zsj.meetingagent.common.vo;

import java.util.List;

/**
 * 通用分页响应对象。
 * 前后端统一使用 records、total、size、current、pages 字段展示列表数据。
 */
public record PageResponse<T>(
        List<T> records,
        long total,
        long size,
        long current,
        long pages
) {

    public static <T> PageResponse<T> of(List<T> records, long total, long current, long size) {
        long pages = size <= 0 ? 0 : (long) Math.ceil((double) total / size);
        return new PageResponse<>(records, total, size, current, pages);
    }
}
