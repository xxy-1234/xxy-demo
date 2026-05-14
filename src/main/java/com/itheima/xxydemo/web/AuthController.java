package com.itheima.xxydemo.web;

import com.itheima.xxydemo.dto.RegisterForm;
import com.itheima.xxydemo.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserAccountService userAccountService;

    public AuthController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("adminPortal", Boolean.FALSE);
        return "login";
    }

    /** 管理员独立入口（与 /login 同一认证，仅展示提示文案） */
    @GetMapping("/admin/login")
    public String adminLogin(Model model) {
        model.addAttribute("adminPortal", Boolean.TRUE);
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerForm", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerForm") RegisterForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "register";
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.reject("password", "两次输入的密码不一致");
            return "register";
        }
        try {
            userAccountService.registerUser(
                    form.getUsername(), form.getPassword(), form.getNickname(), form.getEmail());
            redirectAttributes.addFlashAttribute("success", "注册成功，请使用新账号登录。");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            bindingResult.reject("global", e.getMessage());
            return "register";
        }
    }
}
