package com.zhoolg.manage.infrastructure.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码服务。
 *
 * 安全要点：答案只存在于服务端，按随机 captchaId 索引，短时效、一次性（取出即删，无论成败），
 * 接口只返回图片与 id，绝不返回答案；不进 Cookie/响应体。校验大小写不敏感。
 */
@Service
public class CaptchaService {
    // 去除易混字符 0/O/1/I/L
    private static final String CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int WIDTH = 130;
    private static final int HEIGHT = 44;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Value("${app.auth.captcha.length:4}")
    private int codeLength;
    @Value("${app.auth.captcha.ttl-seconds:120}")
    private long ttlSeconds;
    @Value("${app.auth.captcha.max-entries:2000}")
    private int maxEntries;

    private record Entry(String answer, Instant expiresAt) {
    }

    public record Captcha(String captchaId, String image) {
    }

    public Captcha generate() {
        evictExpired();
        String code = randomCode();
        String captchaId = newId();
        store.put(captchaId, new Entry(code.toLowerCase(), Instant.now().plusSeconds(ttlSeconds)));
        return new Captcha(captchaId, "data:image/png;base64," + render(code));
    }

    /**
     * 一次性校验：无论成败都移除该条目，防止重放与自动化反复试解同一张图。
     */
    public boolean verify(String captchaId, String input) {
        if (captchaId == null || input == null || input.isBlank()) {
            return false;
        }
        Entry entry = store.remove(captchaId);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return false;
        }
        return entry.answer().equals(input.trim().toLowerCase());
    }

    private String newId() {
        byte[] raw = new byte[24];
        random.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(codeLength);
        for (int i = 0; i < codeLength; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private void evictExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
        if (store.size() > maxEntries) {
            store.clear();
        }
    }

    private String render(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 干扰线
        for (int i = 0; i < 6; i++) {
            g.setColor(randomColor(140, 200));
            g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }
        // 噪点
        for (int i = 0; i < 60; i++) {
            g.setColor(randomColor(120, 220));
            g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
        }
        // 字符（随机颜色 + 轻微旋转）
        int charWidth = WIDTH / (code.length() + 1);
        for (int i = 0; i < code.length(); i++) {
            g.setColor(randomColor(20, 110));
            g.setFont(new Font("Arial", Font.BOLD, 30 + random.nextInt(6)));
            double angle = (random.nextDouble() - 0.5) * 0.6;
            AffineTransform original = g.getTransform();
            int x = charWidth * (i + 1) - 8;
            int y = 32 + random.nextInt(6) - 3;
            g.rotate(angle, x, y);
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.setTransform(original);
        }
        g.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception ex) {
            throw new RuntimeException("验证码渲染失败", ex);
        }
    }

    private Color randomColor(int min, int max) {
        int range = max - min;
        return new Color(min + random.nextInt(range), min + random.nextInt(range), min + random.nextInt(range));
    }
}
