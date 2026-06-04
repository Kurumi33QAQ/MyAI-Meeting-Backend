package com.zsj.meetingagent.chat.vo;

import java.util.List;

/**
 * 旧前端分页响应结构。
 * 兼容旧前端分页字段；records 当前可以承载 MongoDB 中查询到的真实会话和历史消息。
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
