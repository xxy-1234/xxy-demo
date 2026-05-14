package com.itheima.xxydemo.service;

import com.itheima.xxydemo.model.GuestbookMessage;
import com.itheima.xxydemo.model.GuestbookThread;
import com.itheima.xxydemo.model.MessageCategory;
import com.itheima.xxydemo.model.MessageLike;
import com.itheima.xxydemo.model.Role;
import com.itheima.xxydemo.model.User;
import com.itheima.xxydemo.repository.GuestbookMessageRepository;
import com.itheima.xxydemo.repository.MessageLikeRepository;
import com.itheima.xxydemo.util.SensitiveWordSanitizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class GuestbookService {

    private static final int MAX_CONTENT = 2000;
    private static final int MAX_TITLE = 160;

    private final GuestbookMessageRepository messageRepository;
    private final MessageLikeRepository likeRepository;
    private final SensitiveWordSanitizer sanitizer;

    public GuestbookService(
            GuestbookMessageRepository messageRepository,
            MessageLikeRepository likeRepository,
            SensitiveWordSanitizer sanitizer) {
        this.messageRepository = messageRepository;
        this.likeRepository = likeRepository;
        this.sanitizer = sanitizer;
    }

    @Transactional(readOnly = true)
    public Page<GuestbookThread> listThreads(
            Pageable pageable, User currentUser, String query, MessageCategory category) {
        boolean admin = currentUser != null && currentUser.getRole() == Role.ADMIN;
        boolean qEmpty = query == null || query.isBlank();
        String q = qEmpty ? "" : query.trim();
        Pageable sorted =
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("createdAt")));
        Page<GuestbookMessage> roots =
                messageRepository.findRootsForPublicOrAdmin(q, qEmpty, category, admin, sorted);
        if (roots.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        List<Long> rootIds = roots.getContent().stream().map(GuestbookMessage::getId).toList();
        Map<Long, Long> likeCountMap = loadLikeCounts(rootIds);
        List<Long> likedIds =
                currentUser != null
                        ? likeRepository.findLikedMessageIds(rootIds, currentUser.getId())
                        : List.of();
        var likedSet = new HashSet<>(likedIds);

        List<GuestbookThread> combined = new ArrayList<>();
        for (GuestbookMessage root : roots.getContent()) {
            List<GuestbookMessage> replies = messageRepository.findByParentIdOrderByCreatedAtAsc(root.getId());
            long likes = likeCountMap.getOrDefault(root.getId(), 0L);
            boolean liked = likedSet.contains(root.getId());
            combined.add(new GuestbookThread(root, replies, likes, liked));
        }
        return new PageImpl<>(combined, pageable, roots.getTotalElements());
    }

    private Map<Long, Long> loadLikeCounts(List<Long> rootIds) {
        List<Object[]> rows = likeRepository.countLikesGrouped(rootIds);
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    @Transactional(readOnly = true)
    public long countRootMessages() {
        return messageRepository.countByParentIdIsNull();
    }

    @Transactional
    public GuestbookMessage postRoot(User author, String title, String content, MessageCategory category) {
        Objects.requireNonNull(author, "author");
        String t = validateTitle(title);
        String c = sanitizer.sanitize(validateContent(content));
        GuestbookMessage m = new GuestbookMessage();
        m.setAuthor(author.getNickname());
        m.setTitle(sanitizer.sanitize(t));
        m.setContent(c);
        m.setParentId(null);
        m.setAuthorUser(author);
        m.setCategory(category != null ? category : MessageCategory.DAILY);
        m.setPinned(false);
        m.setViewCount(0);
        m.setBlocked(false);
        return messageRepository.save(m);
    }

    /** 兼容旧调用：无标题时使用默认 */
    @Transactional
    public GuestbookMessage postRoot(User author, String content) {
        return postRoot(author, "无标题", content, MessageCategory.DAILY);
    }

    @Transactional
    public GuestbookMessage postReply(User author, Long parentId, String content) {
        Objects.requireNonNull(author, "author");
        if (parentId == null) {
            throw new IllegalArgumentException("parentId is required");
        }
        GuestbookMessage parent =
                messageRepository.findById(parentId).orElseThrow(() -> new IllegalArgumentException("主贴不存在"));
        if (!parent.isRoot()) {
            throw new IllegalArgumentException("只能回复主贴");
        }
        if (parent.isBlocked() && author.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("该主贴已屏蔽，无法回复");
        }
        String c = sanitizer.sanitize(validateContent(content));
        GuestbookMessage m = new GuestbookMessage(author.getNickname(), c, parentId, author);
        return messageRepository.save(m);
    }

    @Transactional
    public boolean toggleLike(User user, Long messageId) {
        Objects.requireNonNull(user, "user");
        GuestbookMessage msg =
                messageRepository.findById(messageId).orElseThrow(() -> new IllegalArgumentException("留言不存在"));
        if (!msg.isRoot()) {
            throw new IllegalArgumentException("仅主贴可点赞");
        }
        if (msg.isBlocked() && user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("该主贴已屏蔽");
        }
        if (likeRepository.existsByMessage_IdAndUser_Id(messageId, user.getId())) {
            likeRepository.deleteByMessage_IdAndUser_Id(messageId, user.getId());
            return false;
        }
        likeRepository.save(new MessageLike(msg, user));
        return true;
    }

    @Transactional(readOnly = true)
    public long getLikeCount(Long messageId) {
        return likeRepository.countByMessage_Id(messageId);
    }

    @Transactional
    public void deleteMessage(User actor, Long messageId) {
        Objects.requireNonNull(actor, "actor");
        GuestbookMessage msg =
                messageRepository.findById(messageId).orElseThrow(() -> new IllegalArgumentException("留言不存在"));
        assertCanDelete(actor, msg);
        if (msg.isRoot()) {
            List<GuestbookMessage> replies = messageRepository.findByParentIdOrderByCreatedAtAsc(msg.getId());
            for (GuestbookMessage r : replies) {
                likeRepository.deleteByMessage_Id(r.getId());
                messageRepository.delete(r);
            }
            likeRepository.deleteByMessage_Id(msg.getId());
            messageRepository.delete(msg);
        } else {
            likeRepository.deleteByMessage_Id(msg.getId());
            messageRepository.delete(msg);
        }
    }

    @Transactional
    public GuestbookMessage updateRootMessage(User actor, Long rootId, String title, String content, MessageCategory category) {
        GuestbookMessage msg =
                messageRepository.findById(rootId).orElseThrow(() -> new IllegalArgumentException("留言不存在"));
        if (!msg.isRoot()) {
            throw new IllegalArgumentException("只能修改主贴");
        }
        boolean canEdit = actor.getRole() == Role.ADMIN;
        if (!canEdit) {
            if (msg.getAuthorUser() != null) {
                canEdit = msg.getAuthorUser().getId().equals(actor.getId());
            } else if (msg.getAuthor() != null) {
                canEdit =
                        msg.getAuthor().equals(actor.getNickname())
                                || msg.getAuthor().equals(actor.getUsername());
            }
        }
        if (!canEdit) {
            throw new IllegalArgumentException("只能修改自己发布的主贴");
        }
        msg.setTitle(sanitizer.sanitize(validateTitle(title)));
        msg.setContent(sanitizer.sanitize(validateContent(content)));
        if (category != null) {
            msg.setCategory(category);
        }
        return messageRepository.save(msg);
    }

    @Transactional
    public GuestbookThread loadThreadDetail(Long rootId, User viewer) {
        GuestbookMessage root =
                messageRepository.findDetailById(rootId).orElseThrow(() -> new IllegalArgumentException("留言不存在"));
        if (!root.isRoot()) {
            throw new IllegalArgumentException("不是主贴");
        }
        boolean admin = viewer != null && viewer.getRole() == Role.ADMIN;
        if (root.isBlocked() && !admin) {
            throw new IllegalArgumentException("该内容已屏蔽或不存在");
        }
        root.setViewCount(root.getViewCount() + 1);
        List<GuestbookMessage> replies = messageRepository.findByParentIdOrderByCreatedAtAsc(root.getId());
        long likes = likeRepository.countByMessage_Id(root.getId());
        boolean liked =
                viewer != null && likeRepository.existsByMessage_IdAndUser_Id(root.getId(), viewer.getId());
        return new GuestbookThread(root, replies, likes, liked);
    }

    @Transactional
    public void setPinned(User admin, Long rootId, boolean pinned) {
        assertAdmin(admin);
        GuestbookMessage msg =
                messageRepository.findById(rootId).orElseThrow(() -> new IllegalArgumentException("留言不存在"));
        if (!msg.isRoot()) {
            throw new IllegalArgumentException("只能置顶主贴");
        }
        msg.setPinned(pinned);
    }

    @Transactional
    public void setBlocked(User admin, Long rootId, boolean blocked) {
        assertAdmin(admin);
        GuestbookMessage msg =
                messageRepository.findById(rootId).orElseThrow(() -> new IllegalArgumentException("留言不存在"));
        if (!msg.isRoot()) {
            throw new IllegalArgumentException("只能屏蔽主贴");
        }
        msg.setBlocked(blocked);
    }

    private static void assertAdmin(User admin) {
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("需要管理员权限");
        }
    }

    private void assertCanDelete(User actor, GuestbookMessage msg) {
        boolean admin = actor.getRole() == Role.ADMIN;
        boolean owner = msg.getAuthorUser() != null && msg.getAuthorUser().getId().equals(actor.getId());
        if (!owner && msg.getAuthor() != null) {
            owner =
                    msg.getAuthor().equals(actor.getNickname())
                            || msg.getAuthor().equals(actor.getUsername());
        }
        if (!admin && !owner) {
            throw new IllegalArgumentException("无权删除该留言");
        }
    }

    private String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            return "无标题";
        }
        String t = title.trim();
        if (t.length() > MAX_TITLE) {
            throw new IllegalArgumentException("标题过长（最多 " + MAX_TITLE + " 字）");
        }
        return t;
    }

    private String validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("内容不能为空");
        }
        String c = content.trim();
        if (c.length() > MAX_CONTENT) {
            throw new IllegalArgumentException("内容过长（最多 " + MAX_CONTENT + " 字）");
        }
        return c;
    }

    @Transactional(readOnly = true)
    public List<GuestbookMessage> listAllMessagesNewestFirst() {
        return messageRepository.findAll(
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public GuestbookMessage findMessageOrThrow(Long id) {
        return messageRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("留言不存在"));
    }

    @Transactional(readOnly = true)
    public Page<GuestbookMessage> listMessagesForAdmin(String authorQuery, Pageable pageable) {
        if (authorQuery == null || authorQuery.isBlank()) {
            return messageRepository.findAll(pageable);
        }
        return messageRepository.findByAuthorContainingIgnoreCaseOrderByCreatedAtDesc(authorQuery.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public long countMessagesCreatedAfter(Instant after) {
        return messageRepository.countByCreatedAtAfter(after);
    }

    @Transactional(readOnly = true)
    public long countRootMessagesOnly() {
        return messageRepository.countByParentIdIsNull();
    }

    @Transactional(readOnly = true)
    public long countReplyMessages() {
        return messageRepository.countByParentIdIsNotNull();
    }

    @Transactional(readOnly = true)
    public long countTotalLikes() {
        return likeRepository.count();
    }

    @Transactional(readOnly = true)
    public long countAllMessages() {
        return messageRepository.count();
    }

    @Transactional(readOnly = true)
    public List<GuestbookMessage> listRecentMessagesForDashboard(int limit) {
        int n = Math.max(1, Math.min(limit, 50));
        Pageable first = PageRequest.of(0, n, Sort.by(Sort.Direction.DESC, "createdAt"));
        return messageRepository.findAll(first).getContent();
    }

    @Transactional(readOnly = true)
    public long countMessagesSinceStartOfTodayShanghai() {
        ZoneId z = ZoneId.of("Asia/Shanghai");
        Instant start = ZonedDateTime.now(z).toLocalDate().atStartOfDay(z).toInstant();
        return countMessagesCreatedAfter(start);
    }

    @Transactional(readOnly = true)
    public List<GuestbookMessage> listMyRootMessages(User user) {
        return messageRepository.findByAuthorUser_IdAndParentIdIsNullOrderByCreatedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public List<GuestbookMessage> listMyReplies(User user) {
        return messageRepository.findByAuthorUser_IdAndParentIdIsNotNullOrderByCreatedAtDesc(user.getId());
    }

    public static MessageCategory parseCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return MessageCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 管理员删除用户前，清理其所有留言与回复 */
    @Transactional
    public void purgeAllMessagesForUser(User admin, Long userId) {
        assertAdmin(admin);
        List<GuestbookMessage> roots =
                messageRepository.findByAuthorUser_IdAndParentIdIsNullOrderByCreatedAtDesc(userId);
        for (GuestbookMessage r : roots) {
            deleteMessage(admin, r.getId());
        }
        List<GuestbookMessage> replies =
                messageRepository.findByAuthorUser_IdAndParentIdIsNotNullOrderByCreatedAtDesc(userId);
        for (GuestbookMessage r : replies) {
            deleteMessage(admin, r.getId());
        }
    }
}
