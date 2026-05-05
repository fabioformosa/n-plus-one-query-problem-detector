package it.fabioformosa.nplusonequeryproblemdetector.scan.autoconfigure;

import it.fabioformosa.nplusonequeryproblemdetector.engine.NPlusOneQueryProblemDetector;
import it.fabioformosa.nplusonequeryproblemdetector.scan.NPlusOneStatementInspector;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.InvocationTargetException;

@AutoConfiguration
@ConditionalOnClass({StatementInspector.class, HibernatePropertiesCustomizer.class})
@ConditionalOnProperty(name = "nplusone.scan.enabled", havingValue = "true")
public class NPlusOneScanAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    NPlusOneQueryProblemDetector nPlusOneQueryProblemDetector() {
        return new NPlusOneQueryProblemDetector();
    }

    @Bean
    HibernatePropertiesCustomizer nPlusOneStatementInspectorHibernatePropertiesCustomizer() {
        return hibernateProperties -> {
            Object existingInspector = hibernateProperties.get(JdbcSettings.STATEMENT_INSPECTOR);
            hibernateProperties.put(JdbcSettings.STATEMENT_INSPECTOR, buildInspector(existingInspector));
        };
    }

    private StatementInspector buildInspector(Object existingInspector) {
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
    private StatementInspector toStatementInspector(Object inspector) {
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

    private StatementInspector instantiate(Class<? extends StatementInspector> inspectorClass) {
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
