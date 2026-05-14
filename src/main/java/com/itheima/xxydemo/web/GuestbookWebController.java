package com.itheima.xxydemo.web;

import com.itheima.xxydemo.dto.NewThreadForm;
import com.itheima.xxydemo.model.MessageCategory;
import com.itheima.xxydemo.security.AccountPrincipal;
import com.itheima.xxydemo.service.AnnouncementService;
import com.itheima.xxydemo.service.GuestbookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GuestbookWebController {

    private static final int DEFAULT_SIZE = 8;
    private static final int MAX_SIZE = 30;

    private final GuestbookService guestbookService;
    private final AnnouncementService announcementService;

    public GuestbookWebController(GuestbookService guestbookService, AnnouncementService announcementService) {
        this.guestbookService = guestbookService;
        this.announcementService = announcementService;
    }

    @GetMapping("/")
    public String home(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "cat", required = false) String cat,
            @AuthenticationPrincipal AccountPrincipal principal,
            Model model) {
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        int safePage = Math.max(page, 0);
        var pageable = PageRequest.of(safePage, safeSize);
        MessageCategory category = GuestbookService.parseCategory(cat);
        model.addAttribute(
                "threads",
                guestbookService.listThreads(
                        pageable, principal != null ? principal.getUser() : null, q, category));
        model.addAttribute("newThreadForm", new NewThreadForm());
        model.addAttribute("projectTitle", "在线留言");
        model.addAttribute("appName", "留言板1111111111");
        model.addAttribute("announcements", announcementService.listLatestForHome());
        model.addAttribute("categories", MessageCategory.values());
        model.addAttribute("q", q != null ? q : "");
        model.addAttribute("cat", cat != null ? cat : "");
        return "guestbook";
    }

    @GetMapping("/messages/{id}")
    public String messageDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal AccountPrincipal principal,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            var thread =
                    guestbookService.loadThreadDetail(
                            id, principal != null ? principal.getUser() : null);
            model.addAttribute("thread", thread);
            return "message-detail";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/messages")
    public String postRoot(
            @Valid @ModelAttribute("newThreadForm") NewThreadForm form,
            BindingResult bindingResult,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "cat", required = false) String cat,
            @AuthenticationPrincipal AccountPrincipal principal,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录后再发布留言");
            return "redirect:/";
        }
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", firstError(bindingResult));
            return redirectHome(page, size, q, cat);
        }
        try {
            MessageCategory c = GuestbookService.parseCategory(form.getCategory());
            if (c == null) {
                c = MessageCategory.DAILY;
            }
            guestbookService.postRoot(principal.getUser(), form.getTitle(), form.getContent(), c);
            redirectAttributes.addFlashAttribute("success", "留言已发布。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return redirectHome(page, size, q, cat);
    }

    @PostMapping("/messages/{parentId}/replies")
    public String postReply(
            @PathVariable Long parentId,
            @RequestParam("content") String content,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "cat", required = false) String cat,
            @RequestParam(name = "next", required = false) String next,
            @AuthenticationPrincipal AccountPrincipal principal,
            RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录后再回复");
            return afterReply(next, parentId, page, size, q, cat);
        }
        try {
            guestbookService.postReply(principal.getUser(), parentId, content);
            redirectAttributes.addFlashAttribute("success", "回复已发布。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return afterReply(next, parentId, page, size, q, cat);
    }

    private static String afterReply(String next, Long parentId, int page, int size, String q, String cat) {
        if (next != null && next.startsWith("/messages/") && !next.startsWith("//") && next.length() < 200) {
            return "redirect:" + next;
        }
        StringBuilder sb = new StringBuilder("/?page=").append(Math.max(page, 0)).append("&size=").append(clampSize(size));
        if (q != null && !q.isBlank()) {
            sb.append("&q=").append(java.net.URLEncoder.encode(q.trim(), java.nio.charset.StandardCharsets.UTF_8));
        }
        if (cat != null && !cat.isBlank()) {
            sb.append("&cat=").append(java.net.URLEncoder.encode(cat.trim(), java.nio.charset.StandardCharsets.UTF_8));
        }
        return "redirect:" + sb;
    }

    @PostMapping("/messages/{id}/edit")
    public String editRoot(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam String category,
            @AuthenticationPrincipal AccountPrincipal principal,
            RedirectAttributes redirectAttributes) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            MessageCategory c = GuestbookService.parseCategory(category);
            guestbookService.updateRootMessage(principal.getUser(), id, title, content, c);
            redirectAttributes.addFlashAttribute("success", "已保存修改。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/messages/" + id;
    }

    @PostMapping("/messages/{id}/delete")
    public String deleteMessage(
            @PathVariable Long id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "cat", required = false) String cat,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "redirectRootId", required = false) Long redirectRootId,
            @AuthenticationPrincipal AccountPrincipal principal,
            RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录");
            return "redirect:/";
        }
        try {
            guestbookService.deleteMessage(principal.getUser(), id);
            redirectAttributes.addFlashAttribute("success", "留言已删除");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        if (redirectRootId != null) {
            return "redirect:/messages/" + redirectRootId;
        }
        if ("detail".equals(from)) {
            return "redirect:/";
        }
        return redirectHome(page, size, q, cat);
    }

    @PostMapping("/messages/{id}/like")
    public String likeWeb(
            @PathVariable Long id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "" + DEFAULT_SIZE) int size,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "cat", required = false) String cat,
            @RequestParam(name = "from", required = false) String from,
            @AuthenticationPrincipal AccountPrincipal principal,
            RedirectAttributes redirectAttributes) {
        if (principal == null) {
            redirectAttributes.addFlashAttribute("error", "请先登录后再点赞");
            return "redirect:/";
        }
        try {
            guestbookService.toggleLike(principal.getUser(), id);
            redirectAttributes.addFlashAttribute("success", "已更新点赞");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        if ("detail".equals(from)) {
            return "redirect:/messages/" + id;
        }
        return redirectHome(page, size, q, cat);
    }

    private String redirectHome(int page, int size, String q, String cat) {
        StringBuilder sb = new StringBuilder("/?page=").append(Math.max(page, 0)).append("&size=").append(clampSize(size));
        if (q != null && !q.isBlank()) {
            sb.append("&q=").append(java.net.URLEncoder.encode(q.trim(), java.nio.charset.StandardCharsets.UTF_8));
        }
        if (cat != null && !cat.isBlank()) {
            sb.append("&cat=").append(java.net.URLEncoder.encode(cat.trim(), java.nio.charset.StandardCharsets.UTF_8));
        }
        return "redirect:" + sb;
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }

    private static String firstError(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().isEmpty()
                ? "提交内容不合法"
                : bindingResult.getFieldErrors().getFirst().getDefaultMessage();
    }
}
