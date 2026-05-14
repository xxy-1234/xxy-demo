package com.itheima.xxydemo.model;

/** 留言分类（主贴可选） */
public enum MessageCategory {
    DAILY("日常留言"),
    STUDY("学习求助"),
    FEEDBACK("吐槽建议"),
    CAMPUS("校园公告");

    private final String label;

    MessageCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
