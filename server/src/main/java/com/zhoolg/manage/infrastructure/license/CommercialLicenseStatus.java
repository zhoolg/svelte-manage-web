package com.zhoolg.manage.infrastructure.license;

import java.time.Instant;

public record CommercialLicenseStatus(
        boolean allowed,
        String code,
        String machineId,
        String message,
        String issuedTo,
        String edition,
        String domain,
        Instant expiresAt
) {
    public static CommercialLicenseStatus blocked(String code, String machineId, String message) {
        return new CommercialLicenseStatus(false, code, machineId, message, "", "", "", null);
    }

    public static CommercialLicenseStatus allowed(
            String code,
            String machineId,
            String message,
            String issuedTo,
            String edition,
            String domain,
            Instant expiresAt
    ) {
        return new CommercialLicenseStatus(true, code, machineId, message, issuedTo, edition, domain, expiresAt);
    }
}
