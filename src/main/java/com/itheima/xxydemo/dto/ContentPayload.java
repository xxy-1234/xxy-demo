package com.itheima.xxydemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContentPayload {

    /** 主贴标题（API 发帖可选，缺省为「无标题」） */
    @Size(max = 160, message = "标题最多 160 字")
    private String title;

    /** DAILY / STUDY / FEEDBACK / CAMPUS，可选 */
    private String category;

    @NotBlank(message = "内容不能为空")
    @Size(max = 2000, message = "内容最多 2000 字")
    private String content;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
