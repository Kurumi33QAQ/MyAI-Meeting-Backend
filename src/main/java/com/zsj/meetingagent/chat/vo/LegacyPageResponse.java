package com.zsj.meetingagent.chat.vo;

import java.util.List;

/**
 * 旧前端分页响应结构。
 * 阶段 3 用于临时兼容 AI 页面，阶段 4 接入真实会话存储后再替换 records 数据来源。
 */
public record LegacyPageResponse<T>(
        List<T> records,
        long total,
        long size,
        long current,
        long pages
) {

    public static <T> LegacyPageResponse<T> empty(long current, long size) {
        return new LegacyPageResponse<>(List.of(), 0, size, current, 0);
    }

    public static <T> LegacyPageResponse<T> single(T record) {
        return new LegacyPageResponse<>(List.of(record), 1, 100, 1, 1);
    }

    public static <T> LegacyPageResponse<T> of(List<T> records, long total, long current, long size) {
        long pages = size <= 0 ? 0 : (long) Math.ceil((double) total / size);
        return new LegacyPageResponse<>(records, total, size, current, pages);
    }
}
