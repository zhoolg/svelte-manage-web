package com.zhoolg.manage.infrastructure.auth;

import com.zhoolg.manage.service.UserDirectoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时播种密码：
 * 若配置了 app.auth.seed-password，则为所有 passwordHash 为空的账号生成哈希并写回。
 * 这样开发环境可以用统一密码快速登录，生产环境留空则强制手动设置。
 */
@Component
@Order(1)
public class DataInitializer implements ApplicationRunner {
    private final UserDirectoryService userDirectory;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.seed-password:}")
    private String seedPassword;

    public DataInitializer(UserDirectoryService userDirectory, PasswordEncoder passwordEncoder) {
        this.userDirectory = userDirectory;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (seedPassword == null || seedPassword.isBlank()) {
            return;
        }

        // 为所有 passwordHash 为空的账号播种密码
        for (UserDirectoryService.Account account : userDirectory.findAll()) {
            if (account.passwordHash() == null || account.passwordHash().isBlank()) {
                String hash = passwordEncoder.encode(seedPassword);
                userDirectory.savePasswordHash(account.id(), hash);
            }
        }
    }
}
