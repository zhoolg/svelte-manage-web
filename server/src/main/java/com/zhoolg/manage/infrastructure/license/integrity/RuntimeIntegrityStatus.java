package com.zhoolg.manage.infrastructure.license.integrity;

public record RuntimeIntegrityStatus(
        boolean trusted,
        String code,
        String message
) {
    public static RuntimeIntegrityStatus trusted(String code, String message) {
        return new RuntimeIntegrityStatus(true, code, message);
    }

    public static RuntimeIntegrityStatus untrusted(String code, String message) {
        return new RuntimeIntegrityStatus(false, code, message);
    }
}
