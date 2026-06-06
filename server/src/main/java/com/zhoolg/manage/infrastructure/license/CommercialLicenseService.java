package com.zhoolg.manage.infrastructure.license;

import java.util.Optional;

public interface CommercialLicenseService {
    CommercialLicenseStatus status();

    boolean requiresLicense();

    String currentMachineId();

    Optional<String> licenseSignature();
}
