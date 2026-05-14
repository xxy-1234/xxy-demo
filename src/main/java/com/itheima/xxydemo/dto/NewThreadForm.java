package com.itheima.xxydemo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NewThreadForm {

    @NotBlank(message = "标题不能为空")
    @Size(max = 160, message = "标题最多 160 字")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(max = 2000, message = "内容最多 2000 字")
    private String content;

    /** DAILY / STUDY / FEEDBACK / CAMPUS */
    @NotBlank(message = "请选择分类")
    private String category = "DAILY";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
