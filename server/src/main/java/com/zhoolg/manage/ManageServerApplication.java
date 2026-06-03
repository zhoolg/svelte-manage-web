package com.zhoolg.manage;

import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ManageServerApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ManageServerApplication.class);
        application.setDefaultProperties(defaultProperties());
        application.run(args);
    }

    static Map<String, Object> defaultProperties() {
        URL location = ManageServerApplication.class.getProtectionDomain().getCodeSource().getLocation();
        String classPath = System.getProperty("java.class.path", "");
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.profiles.default", defaultProfileForRuntime(location, classPath));
        executableJarConfigLocation(location, classPath).ifPresent(value ->
                properties.put("spring.config.additional-location", value));
        return properties;
    }

    static String defaultProfileForLocation(URL location) {
        return defaultProfileForRuntime(location, "");
    }

    static String defaultProfileForRuntime(URL location, String classPath) {
        if (isExecutableJarLocation(location) || isSingleJarClassPath(classPath)) {
            return "prod";
        }
        return "dev";
    }

    static Optional<String> executableJarConfigLocation(URL location) {
        return executableJarConfigLocation(location, "");
    }

    static Optional<String> executableJarConfigLocation(URL location, String classPath) {
        Optional<Path> jarPath = executableJarPath(location, classPath);
        if (jarPath.isEmpty()) {
            return Optional.empty();
        }
        Path jarDirectory = jarPath.get().getParent();
        if (jarDirectory == null) {
            return Optional.empty();
        }
        return Optional.of("optional:" + jarDirectory.toUri());
    }

    private static Optional<Path> executableJarPath(URL location, String classPath) {
        Optional<Path> classPathJar = singleJarClassPath(classPath);
        if (classPathJar.isPresent()) {
            return classPathJar;
        }
        if (!isExecutableJarLocation(location)) {
            return Optional.empty();
        }
        try {
            if ("file".equalsIgnoreCase(location.getProtocol())) {
                return Optional.of(Path.of(location.toURI()));
            }
        } catch (Exception ignored) {
            // Spring Boot 可执行 jar 可能返回嵌套 URL，优先由 classpath 解析。
        }
        return Optional.empty();
    }

    private static boolean isExecutableJarLocation(URL location) {
        if (location == null) {
            return false;
        }
        String externalForm = location.toExternalForm().toLowerCase(Locale.ROOT);
        return externalForm.endsWith(".jar")
                || externalForm.contains(".jar!")
                || externalForm.contains(".jar/");
    }

    private static boolean isSingleJarClassPath(String classPath) {
        return singleJarClassPath(classPath).isPresent();
    }

    private static Optional<Path> singleJarClassPath(String classPath) {
        if (classPath == null || classPath.isBlank()) {
            return Optional.empty();
        }
        String[] entries = classPath.split(java.io.File.pathSeparator);
        if (entries.length != 1) {
            return Optional.empty();
        }
        String entry = entries[0].trim();
        if (entry.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            return Optional.of(Path.of(entry).toAbsolutePath());
        }
        return Optional.empty();
    }
}
