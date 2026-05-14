package com.itheima.xxydemo.dto;

import com.itheima.xxydemo.model.GuestbookMessage;
import com.itheima.xxydemo.model.MessageCategory;

public record GuestbookMessageDto(
        Long id,
        String author,
        String title,
        String content,
        String category,
        long viewCount,
        boolean pinned,
        boolean blocked,
        String createdAt,
        Long parentId
) {
    public static GuestbookMessageDto from(GuestbookMessage msg) {
        return new GuestbookMessageDto(
                msg.getId(),
                msg.getAuthor(),
                msg.getTitle(),
                msg.getContent(),
                msg.getCategory() != null ? msg.getCategory().name() : null,
                msg.getViewCount(),
                msg.isPinned(),
                msg.isBlocked(),
                msg.getCreatedAt().toString(),
                msg.getParentId()
        );
    }
}
