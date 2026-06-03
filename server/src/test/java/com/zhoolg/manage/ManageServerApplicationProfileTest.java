package com.zhoolg.manage;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ManageServerApplicationProfileTest {

    @Test
    void usesDevProfileWhenRunningFromClassesDirectory() throws Exception {
        URL classesLocation = Path.of("target", "classes").toAbsolutePath().toUri().toURL();

        assertThat(ManageServerApplication.defaultProfileForLocation(classesLocation)).isEqualTo("dev");
        assertThat(ManageServerApplication.executableJarConfigLocation(classesLocation)).isEmpty();
    }

    @Test
    void usesProdProfileAndJarDirectoryConfigWhenRunningFromPackagedJar() throws Exception {
        Path jarPath = Path.of("deploy", "svelte-manage-web-server-0.1.0.jar").toAbsolutePath();
        URL jarLocation = jarPath.toUri().toURL();

        assertThat(ManageServerApplication.defaultProfileForLocation(jarLocation)).isEqualTo("prod");
        assertThat(ManageServerApplication.executableJarConfigLocation(jarLocation))
                .contains("optional:" + jarPath.getParent().toUri());
    }

    @Test
    void usesProdProfileWhenBootReportsNestedJarLocation() throws Exception {
        URL nestedJarLocation = new URL("jar:file:/opt/app/svelte-manage-web-server-0.1.0.jar!/BOOT-INF/classes!/");

        assertThat(ManageServerApplication.defaultProfileForLocation(nestedJarLocation)).isEqualTo("prod");
    }

    @Test
    void resolvesJarDirectoryFromSingleJarClassPath() throws Exception {
        Path jarPath = Path.of("deploy", "svelte-manage-web-server-0.1.0.jar").toAbsolutePath();
        URL classesLocation = Path.of("target", "classes").toAbsolutePath().toUri().toURL();

        assertThat(ManageServerApplication.defaultProfileForRuntime(classesLocation, jarPath.toString())).isEqualTo("prod");
        assertThat(ManageServerApplication.executableJarConfigLocation(classesLocation, jarPath.toString()))
                .contains("optional:" + jarPath.getParent().toUri());
    }
}
