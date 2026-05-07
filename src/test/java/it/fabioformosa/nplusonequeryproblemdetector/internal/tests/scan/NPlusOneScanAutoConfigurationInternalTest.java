package it.fabioformosa.nplusonequeryproblemdetector.internal.tests.scan;

import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemDetector;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneStatementInspector;
import it.fabioformosa.nplusonequeryproblemdetector.scan.autoconfigure.NPlusOneScanAutoConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.assertj.core.api.Assertions;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
            .withBean(EntityManagerFactory.class, () -> Mockito.mock(EntityManagerFactory.class));

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

    @Test
    void givenExistingHibernatePropertiesCustomizerBean_whenAutoConfigurationRuns_thenExistingBeanIsNotReplaced() throws Exception {
        Class<?> customizerClass = Class.forName(availableHibernatePropertiesCustomizerClassName());
        Object existingCustomizer = createNoOpCustomizer(customizerClass);

        contextRunner
                .withBean("nPlusOneHibernatePropertiesCustomizer", customizerBeanType(customizerClass), () -> existingCustomizer)
                .run(context -> Assertions.assertThat(context.getBean(customizerClass)).isSameAs(existingCustomizer));
    }

    @Test
    void givenCustomizerProxy_whenObjectMethodsAreInvoked_thenTheyAreHandled() throws Exception {
        Class<?> customizerClass = Class.forName(availableHibernatePropertiesCustomizerClassName());

        contextRunner.run(context -> {
            Object customizer = context.getBean(customizerClass);

            Assertions.assertThat(customizer).hasToString("NPlusOneHibernatePropertiesCustomizer");
            Assertions.assertThat(customizer.hashCode()).isEqualTo(System.identityHashCode(customizer));
            Assertions.assertThat(customizer).isEqualTo(customizer).isNotEqualTo(new Object());
        });
    }

    @Test
    void givenExistingNPlusOneInspector_whenCustomizerRuns_thenInspectorIsPreserved() throws Exception {
        Class<?> customizerClass = Class.forName(availableHibernatePropertiesCustomizerClassName());

        contextRunner.run(context -> {
            Object customizer = context.getBean(customizerClass);
            Map<String, Object> hibernateProperties = new HashMap<>();
            NPlusOneStatementInspector existingInspector = new NPlusOneStatementInspector();
            hibernateProperties.put(JdbcSettings.STATEMENT_INSPECTOR, existingInspector);

            invokeCustomize(customizerClass, customizer, hibernateProperties);

            Assertions.assertThat(hibernateProperties.get(JdbcSettings.STATEMENT_INSPECTOR)).isSameAs(existingInspector);
        });
    }

    @Test
    void givenExistingStatementInspectorInstance_whenCustomizerRuns_thenInspectorsAreComposed() throws Exception {
        Class<?> customizerClass = Class.forName(availableHibernatePropertiesCustomizerClassName());

        contextRunner.run(context -> {
            Object customizer = context.getBean(customizerClass);
            Map<String, Object> hibernateProperties = new HashMap<>();
            hibernateProperties.put(JdbcSettings.STATEMENT_INSPECTOR, new PrefixingStatementInspector());

            invokeCustomize(customizerClass, customizer, hibernateProperties);

            Assertions.assertThat(hibernateProperties.get(JdbcSettings.STATEMENT_INSPECTOR))
                    .isInstanceOfSatisfying(StatementInspector.class, inspector ->
                            Assertions.assertThat(inspector.inspect("select 1")).isEqualTo("/*prefixed*/ select 1"));
        });
    }

    @Test
    void givenStatementInspectorClass_whenCustomizerRuns_thenInspectorIsInstantiated() throws Exception {
        Class<?> customizerClass = Class.forName(availableHibernatePropertiesCustomizerClassName());

        contextRunner.run(context -> {
            Object customizer = context.getBean(customizerClass);
            Map<String, Object> hibernateProperties = new HashMap<>();
            hibernateProperties.put(JdbcSettings.STATEMENT_INSPECTOR, PrefixingStatementInspector.class);

            invokeCustomize(customizerClass, customizer, hibernateProperties);

            Assertions.assertThat(hibernateProperties.get(JdbcSettings.STATEMENT_INSPECTOR))
                    .isInstanceOfSatisfying(StatementInspector.class, inspector ->
                            Assertions.assertThat(inspector.inspect("select 1")).isEqualTo("/*prefixed*/ select 1"));
        });
    }

    @Test
    void givenStatementInspectorClassName_whenCustomizerRuns_thenInspectorIsInstantiated() throws Exception {
        Class<?> customizerClass = Class.forName(availableHibernatePropertiesCustomizerClassName());

        contextRunner.run(context -> {
            Object customizer = context.getBean(customizerClass);
            Map<String, Object> hibernateProperties = new HashMap<>();
            hibernateProperties.put(JdbcSettings.STATEMENT_INSPECTOR, PrefixingStatementInspector.class.getName());

            invokeCustomize(customizerClass, customizer, hibernateProperties);

            Assertions.assertThat(hibernateProperties.get(JdbcSettings.STATEMENT_INSPECTOR))
                    .isInstanceOfSatisfying(StatementInspector.class, inspector ->
                            Assertions.assertThat(inspector.inspect("select 1")).isEqualTo("/*prefixed*/ select 1"));
        });
    }

    @Test
    void givenUnsupportedStatementInspectorValue_whenCustomizerRuns_thenFailureIsRaised() throws Exception {
        Class<?> customizerClass = Class.forName(availableHibernatePropertiesCustomizerClassName());

        contextRunner.run(context -> {
            Object customizer = context.getBean(customizerClass);
            Map<String, Object> hibernateProperties = new HashMap<>();
            hibernateProperties.put(JdbcSettings.STATEMENT_INSPECTOR, new Object());

            Assertions.assertThatThrownBy(() -> invokeCustomize(customizerClass, customizer, hibernateProperties))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Unsupported Hibernate StatementInspector value");
        });
    }

    @Test
    void givenNoHibernatePropertiesCustomizerClass_whenRegistrarRuns_thenNoBeanIsRegistered() throws Exception {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(new HidingClassLoader(originalClassLoader));
        try {
            BeanDefinitionRegistryPostProcessor registrar = createRegistrar();
            DefaultListableBeanFactory registry = new DefaultListableBeanFactory();

            registrar.postProcessBeanDefinitionRegistry(registry);

            Assertions.assertThat(registry.containsBeanDefinition("nPlusOneHibernatePropertiesCustomizer")).isFalse();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Test
    void givenHibernatePropertiesCustomizerClassCannotBeLoaded_whenLoadClassRuns_thenFailureIsRaised() throws Exception {
        Method loadClass = NPlusOneScanAutoConfiguration.class.getDeclaredMethod("loadClass", String.class, ClassLoader.class);
        loadClass.setAccessible(true);

        Assertions.assertThatThrownBy(() -> loadClass.invoke(null, BOOT_4_HIBERNATE_PROPERTIES_CUSTOMIZER, new HidingClassLoader(getClass().getClassLoader())))
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer");
    }

    @Test
    void givenUnsupportedCustomizerMethod_whenInvoked_thenFailureIsRaised() throws Exception {
        Object customizer = createHibernatePropertiesCustomizer(UnsupportedCustomizer.class);

        Assertions.assertThatThrownBy(() -> UnsupportedCustomizer.class.getMethod("unsupported").invoke(customizer))
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Unsupported HibernatePropertiesCustomizer method: public abstract void it.fabioformosa.nplusonequeryproblemdetector.internal.tests.scan.NPlusOneScanAutoConfigurationInternalTest$UnsupportedCustomizer.unsupported()");
    }

    @Test
    void givenUnsupportedObjectMethod_whenHandled_thenFailureIsRaised() throws Exception {
        Method handleObjectMethod = NPlusOneScanAutoConfiguration.class.getDeclaredMethod(
                "handleObjectMethod", Object.class, String.class, Object[].class);
        handleObjectMethod.setAccessible(true);

        Assertions.assertThatThrownBy(() -> handleObjectMethod.invoke(null, new Object(), "clone", new Object[0]))
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Unsupported Object method: clone");
    }

    @Test
    void givenUnknownStatementInspectorClassName_whenCustomizerRuns_thenFailureIsRaised() throws Exception {
        Class<?> customizerClass = Class.forName(availableHibernatePropertiesCustomizerClassName());

        contextRunner.run(context -> {
            Object customizer = context.getBean(customizerClass);
            Map<String, Object> hibernateProperties = new HashMap<>();
            hibernateProperties.put(JdbcSettings.STATEMENT_INSPECTOR, "missing.StatementInspector");

            Assertions.assertThatThrownBy(() -> invokeCustomize(customizerClass, customizer, hibernateProperties))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Unable to load Hibernate StatementInspector missing.StatementInspector");
        });
    }

    @Test
    void givenStatementInspectorWithInaccessibleConstructor_whenCustomizerRuns_thenFailureIsRaised() throws Exception {
        Class<?> customizerClass = Class.forName(availableHibernatePropertiesCustomizerClassName());

        contextRunner.run(context -> {
            Object customizer = context.getBean(customizerClass);
            Map<String, Object> hibernateProperties = new HashMap<>();
            hibernateProperties.put(JdbcSettings.STATEMENT_INSPECTOR, PrivateConstructorStatementInspector.class);

            Assertions.assertThatThrownBy(() -> invokeCustomize(customizerClass, customizer, hibernateProperties))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Unable to instantiate Hibernate StatementInspector");
        });
    }

    @Test
    void givenExistingStatementInspectorReturnsNull_whenCustomizerRuns_thenOriginalSqlIsInspected() throws Exception {
        Class<?> customizerClass = Class.forName(availableHibernatePropertiesCustomizerClassName());

        contextRunner.run(context -> {
            Object customizer = context.getBean(customizerClass);
            Map<String, Object> hibernateProperties = new HashMap<>();
            hibernateProperties.put(JdbcSettings.STATEMENT_INSPECTOR, new NullStatementInspector());

            invokeCustomize(customizerClass, customizer, hibernateProperties);

            Assertions.assertThat(hibernateProperties.get(JdbcSettings.STATEMENT_INSPECTOR))
                    .isInstanceOfSatisfying(StatementInspector.class, inspector ->
                            Assertions.assertThat(inspector.inspect("select 1")).isEqualTo("select 1"));
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

    private static Object createNoOpCustomizer(Class<?> customizerClass) {
        return Proxy.newProxyInstance(
                customizerClass.getClassLoader(),
                new Class<?>[]{customizerClass},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "ExistingHibernatePropertiesCustomizer";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new IllegalStateException("Unsupported Object method: " + method.getName());
                        };
                    }
                    return null;
                }
        );
    }

    @SuppressWarnings("unchecked")
    private static Class<Object> customizerBeanType(Class<?> customizerClass) {
        return (Class<Object>) customizerClass;
    }

    private static BeanDefinitionRegistryPostProcessor createRegistrar() throws Exception {
        Method registrar = NPlusOneScanAutoConfiguration.class.getDeclaredMethod("nPlusOneHibernatePropertiesCustomizerRegistrar");
        registrar.setAccessible(true);
        return (BeanDefinitionRegistryPostProcessor) registrar.invoke(null);
    }

    private static Object createHibernatePropertiesCustomizer(Class<?> customizerClass) throws Exception {
        Method factory = NPlusOneScanAutoConfiguration.class.getDeclaredMethod("createHibernatePropertiesCustomizer", Class.class);
        factory.setAccessible(true);
        return factory.invoke(null, customizerClass);
    }

    private static void invokeCustomize(Class<?> customizerClass, Object customizer, Map<String, Object> hibernateProperties) {
        try {
            Method customize = customizerClass.getMethod("customize", Map.class);
            customize.invoke(customizer, hibernateProperties);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (ex.getCause() instanceof Error error) {
                throw error;
            }
            throw new AssertionError("Unable to invoke HibernatePropertiesCustomizer", ex);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Unable to invoke HibernatePropertiesCustomizer", ex);
        }
    }

    public static class PrefixingStatementInspector implements StatementInspector {

        @Override
        public String inspect(String sql) {
            return "/*prefixed*/ " + sql;
        }
    }

    public static class NullStatementInspector implements StatementInspector {

        @Override
        public String inspect(String sql) {
            return null;
        }
    }

    public static class PrivateConstructorStatementInspector implements StatementInspector {

        private PrivateConstructorStatementInspector() {
        }

        @Override
        public String inspect(String sql) {
            return sql;
        }
    }

    interface UnsupportedCustomizer {
        void customize(Map<String, Object> hibernateProperties);

        void unsupported();
    }

    static class HidingClassLoader extends ClassLoader {

        HidingClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (BOOT_4_HIBERNATE_PROPERTIES_CUSTOMIZER.equals(name) || BOOT_3_HIBERNATE_PROPERTIES_CUSTOMIZER.equals(name)) {
                throw new ClassNotFoundException(name);
            }
            return super.loadClass(name, resolve);
        }
    }

}
