package it.fabioformosa.nplusonequeryproblemdetector.scan;

import org.hibernate.resource.jdbc.spi.StatementInspector;

public class NPlusOneStatementInspector implements StatementInspector {

    @Override
    public String inspect(String sql) {
        SqlStatementCapture.record(sql);
        return sql;
    }
}
