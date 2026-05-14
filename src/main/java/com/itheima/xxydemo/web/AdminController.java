package com.itheima.xxydemo.web;

import com.itheima.xxydemo.dto.AdminUserCreateForm;
import com.itheima.xxydemo.model.GuestbookMessage;
import com.itheima.xxydemo.model.Role;
import com.itheima.xxydemo.model.User;
import com.itheima.xxydemo.security.AccountPrincipal;
import com.itheima.xxydemo.service.AnnouncementService;
import com.itheima.xxydemo.service.GuestbookService;
import com.itheima.xxydemo.service.UserAccountService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private static final int MESSAGE_PAGE_DEFAULT_SIZE = 20;

    private final UserAccountService userAccountService;
    private final GuestbookService guestbookService;
    private final AnnouncementService announcementService;

    public AdminController(
            UserAccountService userAccountService,
            GuestbookService guestbookService,
            AnnouncementService announcementService) {
        this.userAccountService = userAccountService;
        this.guestbookService = guestbookService;
        this.announcementService = announcementService;
    }

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        long totalUsers = userAccountService.countAllUsers();
        model.addAttribute("userCount", totalUsers);
        model.addAttribute("enabledUserCount", userAccountService.countEnabledUsers());
        model.addAttribute("adminUserCount", userAccountService.countUsersWithRole(Role.ADMIN));

        long allMessages = guestbookService.countAllMessages();
        model.addAttribute("messageCount", allMessages);
        model.addAttribute("rootMessageCount", guestbookService.countRootMessagesOnly());
        model.addAttribute("replyMessageCount", guestbookService.countReplyMessages());
        model.addAttribute("likeCountTotal", guestbookService.countTotalLikes());
        model.addAttribute("todayMessageCount", guestbookService.countMessagesSinceStartOfTodayShanghai());

        model.addAttribute("recentMessages", guestbookService.listRecentMessagesForDashboard(12));
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userAccountService.listAllUsers());
        model.addAttribute("createForm", new AdminUserCreateForm());
        return "admin/users";
    }

    @PostMapping("/users")
    public String createUser(
            @Valid @ModelAttribute("createForm") AdminUserCreateForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("users", userAccountService.listAllUsers());
            return "admin/users";
        }
        try {
            userAccountService.createUserByAdmin(form.getUsername(), form.getPassword(), form.getRole());
            redirectAttributes.addFlashAttribute("success", "用户已创建。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/disable")
    public String disable(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userAccountService.setEnabled(id, false);
            redirectAttributes.addFlashAttribute("success", "账号已禁用。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/enable")
    public String enable(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userAccountService.setEnabled(id, true);
            redirectAttributes.addFlashAttribute("success", "账号已启用。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/password")
    public String resetPassword(
            @PathVariable Long id,
            @RequestParam("newPassword") String newPassword,
            RedirectAttributes redirectAttributes) {
        try {
            userAccountService.resetPassword(id, newPassword);
            redirectAttributes.addFlashAttribute("success", "密码已重置。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(
            @PathVariable Long id,
            @RequestParam("role") Role role,
            @AuthenticationPrincipal AccountPrincipal principal,
            RedirectAttributes redirectAttributes) {
        try {
            userAccountService.setUserRole(id, role, principal.getUser());
            redirectAttributes.addFlashAttribute("success", "角色已更新。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/messages")
    public String messages(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + MESSAGE_PAGE_DEFAULT_SIZE) int size,
            Model model) {
        int safeSize = Math.min(100, Math.max(5, size));
        int safePage = Math.max(0, page);
        var pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        var messagePage = guestbookService.listMessagesForAdmin(q, pageable);
        model.addAttribute("messagePage", messagePage);
        model.addAttribute("q", q != null ? q : "");
        model.addAttribute("todayMessageCount", guestbookService.countMessagesSinceStartOfTodayShanghai());
        model.addAttribute("totalMessageCount", guestbookService.countAllMessages());
        model.addAttribute("rootTotal", guestbookService.countRootMessagesOnly());
        model.addAttribute("replyTotal", guestbookService.countReplyMessages());
        return "admin/messages";
    }

    @PostMapping("/messages/{id}/delete")
    public String deleteMessage(
            @PathVariable Long id,
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + MESSAGE_PAGE_DEFAULT_SIZE) int size,
            @RequestParam(required = false) String q,
            RedirectAttributes redirectAttributes) {
        try {
            User actor = principal.getUser();
            guestbookService.deleteMessage(actor, id);
            redirectAttributes.addFlashAttribute("success", "留言已删除。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return redirectToMessages(Math.max(0, page), Math.min(100, Math.max(5, size)), q);
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal AccountPrincipal principal,
            RedirectAttributes redirectAttributes) {
        try {
            userAccountService.deleteUserAccount(principal.getUser(), id);
            redirectAttributes.addFlashAttribute("success", "用户及其留言已删除。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/messages/{id}/pin")
    public String pinMessage(
            @PathVariable Long id,
            @RequestParam boolean pinned,
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + MESSAGE_PAGE_DEFAULT_SIZE) int size,
            @RequestParam(required = false) String q,
            RedirectAttributes redirectAttributes) {
        try {
            guestbookService.setPinned(principal.getUser(), id, pinned);
            redirectAttributes.addFlashAttribute("success", pinned ? "已置顶" : "已取消置顶");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return redirectToMessages(Math.max(0, page), Math.min(100, Math.max(5, size)), q);
    }

    @PostMapping("/messages/{id}/block")
    public String blockMessage(
            @PathVariable Long id,
            @RequestParam boolean blocked,
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + MESSAGE_PAGE_DEFAULT_SIZE) int size,
            @RequestParam(required = false) String q,
            RedirectAttributes redirectAttributes) {
        try {
            guestbookService.setBlocked(principal.getUser(), id, blocked);
            redirectAttributes.addFlashAttribute("success", blocked ? "已屏蔽主贴" : "已解除屏蔽");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return redirectToMessages(Math.max(0, page), Math.min(100, Math.max(5, size)), q);
    }

    @GetMapping("/announcements")
    public String announcements(Model model) {
        model.addAttribute("items", announcementService.listAllNewestFirst());
        return "admin/announcements";
    }

    @PostMapping("/announcements")
    public String createAnnouncement(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam String title,
            @RequestParam String content,
            RedirectAttributes redirectAttributes) {
        try {
            announcementService.create(principal.getUser(), title, content);
            redirectAttributes.addFlashAttribute("success", "公告已发布。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/announcements";
    }

    @PostMapping("/announcements/{id}/delete")
    public String deleteAnnouncement(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        announcementService.delete(id);
        redirectAttributes.addFlashAttribute("success", "公告已删除。");
        return "redirect:/admin/announcements";
    }

    private static String redirectToMessages(int page, int size, String q) {
        String base = "/admin/messages?page=" + page + "&size=" + size;
        if (q == null || q.isBlank()) {
            return "redirect:" + base;
        }
        return "redirect:" + base + "&q=" + URLEncoder.encode(q.trim(), StandardCharsets.UTF_8);
    }

    @GetMapping("/export/messages.csv")
    public void exportMessagesCsv(HttpServletResponse response) throws IOException {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"guestbook-messages.csv\"");
        List<GuestbookMessage> rows = guestbookService.listAllMessagesNewestFirst();
        PrintWriter w = response.getWriter();
        w.write('\ufeff');
        w.println("id,type,title,category,pinned,blocked,view_count,author,parent_id,created_at_utc,content");
        for (GuestbookMessage m : rows) {
            w.print(m.getId());
            w.print(',');
            w.print(m.isRoot() ? "root" : "reply");
            w.print(',');
            w.print(csvCell(m.getTitle()));
            w.print(',');
            w.print(m.getCategory() != null ? m.getCategory().name() : "");
            w.print(',');
            w.print(m.isPinned());
            w.print(',');
            w.print(m.isBlocked());
            w.print(',');
            w.print(m.getViewCount());
            w.print(',');
            w.print(csvCell(m.getAuthor()));
            w.print(',');
            w.print(m.getParentId() == null ? "" : m.getParentId());
            w.print(',');
            w.print(csvCell(m.getCreatedAt() == null ? "" : m.getCreatedAt().toString()));
            w.print(',');
            w.println(csvCell(m.getContent()));
        }
        w.flush();
    }

    private static String csvCell(String raw) {
        if (raw == null) {
            return "\"\"";
        }
        return "\"" + raw.replace("\"", "\"\"") + "\"";
    }

    @GetMapping("/system")
    public String systemGuide() {
        return "admin/system";
    }
}
