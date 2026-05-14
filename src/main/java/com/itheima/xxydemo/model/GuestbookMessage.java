package com.itheima.xxydemo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "guestbook_message",
        indexes = {
                @Index(name = "idx_parent_created", columnList = "parent_id,created_at"),
                @Index(name = "idx_root_pin_created", columnList = "parent_id,pinned,created_at")
        }
)
public class GuestbookMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String author;

    /** 主贴标题；回复可为空 */
    @Column(length = 160)
    private String title;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private Instant createdAt;

    /** 为空表示主贴；非空表示回复对应主贴 id（仅允许一层回复） */
    @Column(name = "parent_id")
    private Long parentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 32)
    private MessageCategory category;

    @Column(nullable = false)
    private boolean pinned;

    @Column(nullable = false)
    private long viewCount;

    /** 屏蔽后主贴对普通用户不可见列表，详情提示已屏蔽 */
    @Column(nullable = false)
    private boolean blocked;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User authorUser;

    public GuestbookMessage() {
    }

    public GuestbookMessage(String author, String content, Long parentId, User authorUser) {
        this.author = author;
        this.content = content;
        this.parentId = parentId;
        this.authorUser = authorUser;
    }

    public GuestbookMessage(String author, String content, Long parentId) {
        this(author, content, parentId, null);
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (parentId == null) {
            if (title == null || title.isBlank()) {
                title = "无标题";
            }
            if (category == null) {
                category = MessageCategory.DAILY;
            }
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public MessageCategory getCategory() {
        return category;
    }

    public void setCategory(MessageCategory category) {
        this.category = category;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public long getViewCount() {
        return viewCount;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public User getAuthorUser() {
        return authorUser;
    }

    public void setAuthorUser(User authorUser) {
        this.authorUser = authorUser;
    }

    public boolean isRoot() {
        return parentId == null;
    }
}
