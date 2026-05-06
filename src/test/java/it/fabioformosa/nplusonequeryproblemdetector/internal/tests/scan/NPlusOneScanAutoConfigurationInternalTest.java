package it.fabioformosa.nplusonequeryproblemdetector.internal.tests.scan;

import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemDetector;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneStatementInspector;
import it.fabioformosa.nplusonequeryproblemdetector.scan.autoconfigure.NPlusOneScanAutoConfiguration;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.hibernate.cfg.JdbcSettings;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class NPlusOneScanAutoConfigurationInternalTest {

    private static final String BOOT_4_HIBERNATE_PROPERTIES_CUSTOMIZER =
            "org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer";
    private static final String BOOT_3_HIBERNATE_PROPERTIES_CUSTOMIZER =
            "org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NPlusOneScanAutoConfiguration.class))
            .withPropertyValues("n-plus-one-query-detector.scan.enabled=true")
            .withBean(EntityManager.class, () -> Mockito.mock(EntityManager.class));

    @Test
    void givenScanModeIsEnabled_whenAutoConfigurationRuns_thenDetectorBeanIsAvailable() {
        contextRunner.run(context -> Assertions.assertThat(context).hasSingleBean(NPlusOneQueryProblemDetector.class));
    }

    @Test
    void givenScanModeIsEnabled_whenAutoConfigurationRuns_thenHibernatePropertiesCustomizerIsRegistered() throws ClassNotFoundException {
        Class<?> customizerClass = Class.forName(availableHibernatePropertiesCustomizerClassName());

        contextRunner.run(context -> Assertions.assertThat(context).hasSingleBean(customizerClass));
    }

    @Test
    void givenHibernatePropertiesCustomizerBean_whenInvoked_thenStatementInspectorIsInstalled() throws Exception {
        Class<?> customizerClass = Class.forName(availableHibernatePropertiesCustomizerClassName());

        contextRunner.run(context -> {
            Object customizer = context.getBean(customizerClass);
            Map<String, Object> hibernateProperties = new HashMap<>();

            invokeCustomize(customizerClass, customizer, hibernateProperties);

            Assertions.assertThat(hibernateProperties.get(JdbcSettings.STATEMENT_INSPECTOR))
                    .isInstanceOf(NPlusOneStatementInspector.class);
        });
    }

    private static String availableHibernatePropertiesCustomizerClassName() {
        if (isPresent(BOOT_4_HIBERNATE_PROPERTIES_CUSTOMIZER)) {
            return BOOT_4_HIBERNATE_PROPERTIES_CUSTOMIZER;
        }
        return BOOT_3_HIBERNATE_PROPERTIES_CUSTOMIZER;
    }

    private static boolean isPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private static void invokeCustomize(Class<?> customizerClass, Object customizer, Map<String, Object> hibernateProperties) {
        try {
            Method customize = customizerClass.getMethod("customize", Map.class);
            customize.invoke(customizer, hibernateProperties);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Unable to invoke HibernatePropertiesCustomizer", ex);
        }
    }
}
