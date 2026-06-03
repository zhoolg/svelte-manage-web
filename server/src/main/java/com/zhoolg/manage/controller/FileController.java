package com.zhoolg.manage.controller;

import com.zhoolg.manage.entity.base.ApiResponse;
import com.zhoolg.manage.entity.base.CurrentUser;
import com.zhoolg.manage.exception.ApiException;
import com.zhoolg.manage.service.AuditLogService;
import com.zhoolg.manage.service.IAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 管理端和 Web 端共用的图片上传入口。
 */
@RestController
@RequestMapping("/file")
public class FileController {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Map<String, byte[]> MAGIC_NUMBERS = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "image/gif", new byte[]{0x47, 0x49, 0x46, 0x38}
    );
    private static final Map<String, Set<String>> MIME_EXTENSIONS = Map.of(
            "image/jpeg", Set.of(".jpg", ".jpeg"),
            "image/png", Set.of(".png"),
            "image/gif", Set.of(".gif"),
            "image/webp", Set.of(".webp")
    );

    private final IAuthService authService;
    private final AuditLogService auditLogService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.upload.public-path:/uploads}")
    private String publicPath;

    public FileController(IAuthService authService, AuditLogService auditLogService) {
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/uploadImg")
    public ApiResponse<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) throws IOException {
        CurrentUser user = authService.requireUser(authorization);

        if (file.isEmpty()) {
            throw new ApiException(400, "上传文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException(400, "文件大小超过限制（最大 10MB）");
        }

        String originalName = sanitizeFilename(file.getOriginalFilename());
        String extension = extractExtension(originalName);

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ApiException(400, "不支持的文件类型，仅允许：" + String.join(", ", ALLOWED_EXTENSIONS));
        }

        byte[] fileBytes = file.getBytes();
        String detectedMimeType = detectMimeType(fileBytes);
        if (detectedMimeType == null) {
            throw new ApiException(400, "文件格式校验失败，请上传有效的图片文件");
        }
        if (!MIME_EXTENSIONS.getOrDefault(detectedMimeType, Set.of()).contains(extension.toLowerCase())) {
            throw new ApiException(400, "文件扩展名与实际图片格式不一致");
        }

        LocalDate today = LocalDate.now();
        Path directory = Path.of(uploadDir, String.valueOf(today.getYear()), String.format("%02d", today.getMonthValue()))
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(directory);

        String filename = UUID.randomUUID() + extension;
        Path target = directory.resolve(filename);

        if (!target.normalize().startsWith(directory)) {
            throw new ApiException(400, "非法文件路径");
        }

        Files.write(target, fileBytes, StandardOpenOption.CREATE_NEW);

        String url = publicPath + "/" + today.getYear() + "/" + String.format("%02d", today.getMonthValue()) + "/" + filename;
        auditLogService.success(user, "file", "upload", "file", filename, "上传图片 " + originalName, Map.of(
                "size", file.getSize(),
                "mimeType", detectedMimeType,
                "url", url
        ));
        return ApiResponse.ok(Map.of(
                "url", url,
                "name", originalName,
                "size", file.getSize(),
                "mimeType", detectedMimeType
        ), "上传成功");
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex);
    }

    private String detectMimeType(byte[] fileBytes) {
        if (isWebp(fileBytes)) {
            return "image/webp";
        }
        for (Map.Entry<String, byte[]> entry : MAGIC_NUMBERS.entrySet()) {
            byte[] magic = entry.getValue();
            if (fileBytes.length >= magic.length) {
                boolean match = true;
                for (int i = 0; i < magic.length; i++) {
                    if (fileBytes[i] != magic[i]) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    private boolean isWebp(byte[] fileBytes) {
        return fileBytes.length >= 12
                && fileBytes[0] == 0x52
                && fileBytes[1] == 0x49
                && fileBytes[2] == 0x46
                && fileBytes[3] == 0x46
                && fileBytes[8] == 0x57
                && fileBytes[9] == 0x45
                && fileBytes[10] == 0x42
                && fileBytes[11] == 0x50;
    }
}
