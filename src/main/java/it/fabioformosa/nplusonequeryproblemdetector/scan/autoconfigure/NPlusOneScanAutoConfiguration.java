package it.fabioformosa.nplusonequeryproblemdetector.scan.autoconfigure;

import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemDetector;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneStatementInspector;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Map;

@AutoConfiguration
@ConditionalOnClass(StatementInspector.class)
@ConditionalOnProperty(name = "n-plus-one-query-detector.scan.enabled", havingValue = "true")
public class NPlusOneScanAutoConfiguration {

    private static final String HIBERNATE_PROPERTIES_CUSTOMIZER_BEAN_NAME = "nPlusOneHibernatePropertiesCustomizer";
    private static final String BOOT_4_HIBERNATE_PROPERTIES_CUSTOMIZER =
            "org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer";
    private static final String BOOT_3_HIBERNATE_PROPERTIES_CUSTOMIZER =
            "org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer";

    @Bean
    @ConditionalOnBean(EntityManagerFactory.class)
    @ConditionalOnMissingBean
    NPlusOneQueryProblemDetector nPlusOneQueryProblemDetector(EntityManagerFactory entityManagerFactory) {
        return new NPlusOneQueryProblemDetector(entityManagerFactory);
    }

    @Bean
    static BeanDefinitionRegistryPostProcessor nPlusOneHibernatePropertiesCustomizerRegistrar() {
        return new BeanDefinitionRegistryPostProcessor() {
            @Override
            public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
                if (registry.containsBeanDefinition(HIBERNATE_PROPERTIES_CUSTOMIZER_BEAN_NAME)) {
                    return;
                }

                Class<?> customizerInterface = resolveHibernatePropertiesCustomizerInterface();
                if (customizerInterface == null) {
                    return;
                }

                RootBeanDefinition beanDefinition = new RootBeanDefinition(customizerInterface);
                beanDefinition.setInstanceSupplier(() -> createHibernatePropertiesCustomizer(customizerInterface));
                registry.registerBeanDefinition(HIBERNATE_PROPERTIES_CUSTOMIZER_BEAN_NAME, beanDefinition);
            }
        };
    }

    private static Class<?> resolveHibernatePropertiesCustomizerInterface() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (ClassUtils.isPresent(BOOT_4_HIBERNATE_PROPERTIES_CUSTOMIZER, classLoader)) {
            return loadClass(BOOT_4_HIBERNATE_PROPERTIES_CUSTOMIZER, classLoader);
        }
        if (ClassUtils.isPresent(BOOT_3_HIBERNATE_PROPERTIES_CUSTOMIZER, classLoader)) {
            return loadClass(BOOT_3_HIBERNATE_PROPERTIES_CUSTOMIZER, classLoader);
        }
        return null;
    }

    private static Class<?> loadClass(String className, ClassLoader classLoader) {
        try {
            return ClassUtils.forName(className, classLoader);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Unable to load HibernatePropertiesCustomizer " + className, ex);
        }
    }

    private static Object createHibernatePropertiesCustomizer(Class<?> customizerInterface) {
        return Proxy.newProxyInstance(
                customizerInterface.getClassLoader(),
                new Class<?>[]{customizerInterface},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return handleObjectMethod(proxy, method.getName(), args);
                    }
                    if ("customize".equals(method.getName()) && args != null && args.length == 1 && args[0] instanceof Map<?, ?> hibernateProperties) {
                        Object existingInspector = hibernateProperties.get(JdbcSettings.STATEMENT_INSPECTOR);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> writableHibernateProperties = (Map<String, Object>) hibernateProperties;
                        writableHibernateProperties.put(JdbcSettings.STATEMENT_INSPECTOR, buildInspector(existingInspector));
                        return null;
                    }
                    throw new IllegalStateException("Unsupported HibernatePropertiesCustomizer method: " + method);
                }
        );
    }

    private static Object handleObjectMethod(Object proxy, String methodName, Object[] args) {
        return switch (methodName) {
            case "toString" -> "NPlusOneHibernatePropertiesCustomizer";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new IllegalStateException("Unsupported Object method: " + methodName);
        };
    }

    private static StatementInspector buildInspector(Object existingInspector) {
        if (existingInspector == null) {
            return new NPlusOneStatementInspector();
        }
        StatementInspector delegate = toStatementInspector(existingInspector);
        if (delegate instanceof NPlusOneStatementInspector) {
            return delegate;
        }
        return new CompositeStatementInspector(delegate, new NPlusOneStatementInspector());
    }

    @SuppressWarnings("unchecked")
    private static StatementInspector toStatementInspector(Object inspector) {
        if (inspector instanceof StatementInspector statementInspector) {
            return statementInspector;
        }
        if (inspector instanceof Class<?> inspectorClass) {
            return instantiate((Class<? extends StatementInspector>) inspectorClass);
        }
        if (inspector instanceof String inspectorClassName) {
            try {
                Class<?> inspectorClass = Class.forName(inspectorClassName);
                return instantiate((Class<? extends StatementInspector>) inspectorClass);
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("Unable to load Hibernate StatementInspector " + inspectorClassName, ex);
            }
        }
        throw new IllegalStateException("Unsupported Hibernate StatementInspector value: " + inspector);
    }

    private static StatementInspector instantiate(Class<? extends StatementInspector> inspectorClass) {
        try {
            return inspectorClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new IllegalStateException("Unable to instantiate Hibernate StatementInspector " + inspectorClass.getName(), ex);
        }
    }

    private record CompositeStatementInspector(StatementInspector first, StatementInspector second) implements StatementInspector {
        @Override
        public String inspect(String sql) {
            String inspectedSql = first.inspect(sql);
            return second.inspect(inspectedSql != null ? inspectedSql : sql);
        }
    }

}
