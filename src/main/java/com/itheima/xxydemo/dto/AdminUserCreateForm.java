package com.itheima.xxydemo.dto;

import com.itheima.xxydemo.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AdminUserCreateForm {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 40)
    private String username;

    @NotBlank(message = "初始密码不能为空")
    @Size(min = 6, max = 80)
    private String password;

    @NotNull(message = "请选择角色")
    private Role role = Role.USER;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
