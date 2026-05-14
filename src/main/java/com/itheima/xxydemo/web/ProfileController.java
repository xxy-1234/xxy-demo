package com.itheima.xxydemo.web;

import com.itheima.xxydemo.model.User;
import com.itheima.xxydemo.security.AccountPrincipal;
import com.itheima.xxydemo.service.GuestbookService;
import com.itheima.xxydemo.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final UserAccountService userAccountService;
    private final GuestbookService guestbookService;

    public ProfileController(UserAccountService userAccountService, GuestbookService guestbookService) {
        this.userAccountService = userAccountService;
        this.guestbookService = guestbookService;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal AccountPrincipal principal, Model model) {
        if (principal != null) {
            model.addAttribute("profileUser", userAccountService.requireUserByUsername(principal.getUsername()));
        }
        return "profile";
    }

    @GetMapping("/profile/my-posts")
    public String myPosts(@AuthenticationPrincipal AccountPrincipal principal, Model model) {
        User u = userAccountService.requireUserByUsername(principal.getUsername());
        model.addAttribute("roots", guestbookService.listMyRootMessages(u));
        return "profile-my-posts";
    }

    @GetMapping("/profile/my-replies")
    public String myReplies(@AuthenticationPrincipal AccountPrincipal principal, Model model) {
        User u = userAccountService.requireUserByUsername(principal.getUsername());
        model.addAttribute("replies", guestbookService.listMyReplies(u));
        return "profile-my-replies";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam String nickname,
            @RequestParam String email,
            @RequestParam(required = false) String avatarUrl,
            @RequestParam(required = false) String bio,
            RedirectAttributes redirectAttributes) {
        try {
            userAccountService.updateProfile(principal.getUser(), nickname, email, avatarUrl, bio);
            redirectAttributes.addFlashAttribute("success", "资料已更新。");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "新密码和确认密码不匹配");
            return "redirect:/profile";
        }
        try {
            User u = principal.getUser();
            userAccountService.changeOwnPassword(u, currentPassword, newPassword);
            new SecurityContextLogoutHandler().logout(request, response, null);
            return "redirect:/login?pwd=ok";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile";
        }
    }
}
