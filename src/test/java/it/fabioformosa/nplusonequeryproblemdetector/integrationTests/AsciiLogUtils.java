package it.fabioformosa.nplusonequeryproblemdetector.integrationTests;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class AsciiLogUtils {
    static <T> void displayEntitiesViaLogs(List<T> entities, String[] headers, java.util.function.Function<T, Object[]> rowMapper) {
        int[] columnWidths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            columnWidths[i] = headers[i].length();
        }

        for (T entity : entities) {
            Object[] row = rowMapper.apply(entity);
            for (int i = 0; i < row.length; i++) {
                columnWidths[i] = Math.max(columnWidths[i], String.valueOf(row[i]).length());
            }
        }

        StringBuilder formatBuilder = new StringBuilder("|");
        for (int width : columnWidths) {
            formatBuilder.append(" %-").append(width).append("s |");
        }
        String format = formatBuilder.toString();

        StringBuilder separatorBuilder = new StringBuilder("+");
        for (int width : columnWidths) {
            separatorBuilder.append("-".repeat(width + 2)).append("+");
        }
        String separator = separatorBuilder.toString();

        log.info(separator);
        log.info(String.format(format, (Object[]) headers));
        log.info(separator);
        for (T entity : entities) {
            log.info(String.format(format, rowMapper.apply(entity)));
        }
        log.info(separator);
    }
}
