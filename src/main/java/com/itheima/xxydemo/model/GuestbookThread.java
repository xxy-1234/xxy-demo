package com.itheima.xxydemo.model;

import java.util.List;

/** 主贴 + 回复 + 点赞统计（仅主贴可点赞） */
public record GuestbookThread(
        GuestbookMessage root,
        List<GuestbookMessage> replies,
        long likeCount,
        boolean likedByCurrentUser
) {
}
