package com.zhoolg.manage.infrastructure.license;

import com.zhoolg.manage.infrastructure.license.integrity.RuntimeIntegrityStatus;
import com.zhoolg.manage.infrastructure.license.integrity.RuntimeIntegrityVerifier;
import com.zhoolg.manage.service.AiModelGateway;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class OfficialPrivateModuleGuard {
    private static final String OFFICIAL_LICENSE_CLASS =
            "com.zhoolg.manage.license.OfficialCommercialLicenseService";
    private static final String OFFICIAL_AI_CLASS =
            "com.zhoolg.manage.ai.OfficialAiModelGateway";

    private final CommercialLicenseService licenseService;
    private final RuntimeIntegrityVerifier runtimeIntegrityVerifier;
    private final AiModelGateway aiModelGateway;

    public OfficialPrivateModuleGuard(
            CommercialLicenseService licenseService,
            RuntimeIntegrityVerifier runtimeIntegrityVerifier,
            AiModelGateway aiModelGateway
    ) {
        this.licenseService = licenseService;
        this.runtimeIntegrityVerifier = runtimeIntegrityVerifier;
        this.aiModelGateway = aiModelGateway;
    }

    @PostConstruct
    public void verifyOfficialPrivateModules() {
        requireClass(OFFICIAL_LICENSE_CLASS);
        requireClass(OFFICIAL_AI_CLASS);
        requireImplementation(licenseService, "com.zhoolg.manage.license.", "official license core");
        requireImplementation(aiModelGateway, "com.zhoolg.manage.ai.", "official AI core");

        RuntimeIntegrityStatus status = runtimeIntegrityVerifier.verify();
        if (!status.trusted()) {
            throw new IllegalStateException("Official private runtime rejected: " + status.code() + " - " + status.message());
        }
    }

    private void requireClass(String className) {
        try {
            Class.forName(className, false, getClass().getClassLoader());
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Required official private module class is missing: " + className, ex);
        }
    }

    private void requireImplementation(Object bean, String packagePrefix, String moduleName) {
        String beanClassName = bean.getClass().getName();
        if (!beanClassName.startsWith(packagePrefix)) {
            throw new IllegalStateException(
                    "Required " + moduleName + " bean is not active. Actual bean: " + beanClassName
            );
        }
    }
}
