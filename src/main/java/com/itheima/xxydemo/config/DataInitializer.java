package com.itheima.xxydemo.config;

import com.itheima.xxydemo.model.Role;
import com.itheima.xxydemo.model.User;
import com.itheima.xxydemo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.bootstrap-admin-password:}")
    private String bootstrapAdminPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        if (!StringUtils.hasText(bootstrapAdminPassword)) {
            log.warn("数据库中无用户，且未配置 app.security.bootstrap-admin-password，跳过默认管理员初始化。");
            return;
        }
        User admin = new User("admin", passwordEncoder.encode(bootstrapAdminPassword.trim()), Role.ADMIN);
        admin.setNickname("系统管理员");
        admin.setEmail("admin@localhost");
        userRepository.save(admin);
        log.warn("已创建默认管理员账号 admin，请尽快登录并修改密码（勿在生产环境使用弱口令）。");
    }
}
