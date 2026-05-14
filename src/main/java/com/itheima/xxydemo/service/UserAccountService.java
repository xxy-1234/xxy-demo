package com.itheima.xxydemo.service;

import com.itheima.xxydemo.model.Role;
import com.itheima.xxydemo.model.User;
import com.itheima.xxydemo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class UserAccountService {

    private static final int MIN_USERNAME = 3;
    private static final int MAX_USERNAME = 40;
    private static final int MIN_PASSWORD = 6;
    private static final Pattern EMAIL =
            Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GuestbookService guestbookService;

    public UserAccountService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            GuestbookService guestbookService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.guestbookService = guestbookService;
    }

    @Transactional
    public User registerUser(String username, String rawPassword, String nickname, String email) {
        validateUsername(username);
        validatePassword(rawPassword);
        validateNickname(nickname);
        validateEmail(email);
        if (userRepository.existsByUsernameIgnoreCase(username.trim())) {
            throw new IllegalArgumentException("用户名已被占用");
        }
        if (userRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("邮箱已被占用");
        }
        User u = new User(username.trim().toLowerCase(), passwordEncoder.encode(rawPassword), Role.USER);
        u.setNickname(nickname.trim());
        u.setEmail(email.trim().toLowerCase());
        return userRepository.save(u);
    }

    @Transactional
    public User createUserByAdmin(String username, String rawPassword, Role role) {
        validateUsername(username);
        validatePassword(rawPassword);
        if (userRepository.existsByUsernameIgnoreCase(username.trim())) {
            throw new IllegalArgumentException("用户名已存在");
        }
        String un = username.trim().toLowerCase();
        User u = new User(un, passwordEncoder.encode(rawPassword), role);
        u.setNickname(un);
        u.setEmail(un + "@system.local");
        return userRepository.save(u);
    }

    @Transactional
    public void setEnabled(Long userId, boolean enabled) {
        User u = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        u.setEnabled(enabled);
    }

    @Transactional
    public void resetPassword(Long userId, String newRawPassword) {
        validatePassword(newRawPassword);
        User u = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        u.setPasswordHash(passwordEncoder.encode(newRawPassword));
    }

    @Transactional
    public void changeOwnPassword(User user, String oldRawPassword, String newRawPassword) {
        User managed = userRepository.findById(user.getId()).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (!passwordEncoder.matches(oldRawPassword, managed.getPasswordHash())) {
            throw new IllegalArgumentException("原密码不正确");
        }
        validatePassword(newRawPassword);
        managed.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userRepository.save(managed);
    }

    @Transactional
    public void updateProfile(User actor, String nickname, String email, String avatarUrl, String bio) {
        User managed = userRepository.findById(actor.getId()).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        validateNickname(nickname);
        validateEmail(email);
        String em = email.trim().toLowerCase();
        userRepository
                .findByEmailIgnoreCase(em)
                .filter(u -> !u.getId().equals(managed.getId()))
                .ifPresent(u -> {
                    throw new IllegalArgumentException("邮箱已被其他账号占用");
                });
        managed.setNickname(nickname.trim());
        managed.setEmail(em);
        String a = avatarUrl == null ? "" : avatarUrl.trim();
        managed.setAvatarUrl(a.isEmpty() ? null : a);
        String b = bio == null ? "" : bio.trim();
        managed.setBio(b.isEmpty() ? null : (b.length() > 200 ? b.substring(0, 200) : b));
        userRepository.save(managed);
    }

    @Transactional
    public void deleteUserAccount(User adminActor, Long targetUserId) {
        if (adminActor.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("需要管理员权限");
        }
        if (adminActor.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("不能删除自己的账号");
        }
        User target = userRepository.findById(targetUserId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (target.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("不能直接删除管理员账号（请先降级角色）");
        }
        guestbookService.purgeAllMessagesForUser(adminActor, targetUserId);
        userRepository.delete(target);
    }

    @Transactional(readOnly = true)
    public List<User> listAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public long countAllUsers() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public User requireUserByUsername(String username) {
        return userRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    @Transactional(readOnly = true)
    public long countEnabledUsers() {
        return userRepository.countByEnabledTrue();
    }

    @Transactional(readOnly = true)
    public long countUsersWithRole(Role role) {
        return userRepository.countByRole(role);
    }

    @Transactional
    public void setUserRole(Long userId, Role newRole, User adminActor) {
        Objects.requireNonNull(newRole, "newRole");
        Objects.requireNonNull(adminActor, "adminActor");
        User target = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (target.getId().equals(adminActor.getId()) && newRole != Role.ADMIN) {
            throw new IllegalArgumentException("不能取消自己的管理员角色");
        }
        target.setRole(newRole);
        userRepository.save(target);
    }

    private void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        String n = nickname.trim();
        if (n.length() > 64) {
            throw new IllegalArgumentException("昵称过长");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        String e = email.trim();
        if (e.length() > 120 || !EMAIL.matcher(e).matches()) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        String u = username.trim();
        if (u.length() < MIN_USERNAME || u.length() > MAX_USERNAME) {
            throw new IllegalArgumentException("用户名长度需在 " + MIN_USERNAME + "-" + MAX_USERNAME + " 之间");
        }
        if (!u.matches("^[a-zA-Z0-9_\\-]+$")) {
            throw new IllegalArgumentException("用户名仅允许字母、数字、下划线与短横线");
        }
    }

    private void validatePassword(String raw) {
        if (raw == null || raw.length() < MIN_PASSWORD) {
            throw new IllegalArgumentException("密码至少 " + MIN_PASSWORD + " 位");
        }
    }
}
