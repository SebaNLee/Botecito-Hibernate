package ar.edu.itba.paw.persistence;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class JdbcDialect {

    private final boolean postgres;

    @Autowired
    public JdbcDialect(final @NonNull DataSource dataSource) {
        final JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        boolean resolved = false;
        try {
            resolved = Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
                final String productName = connection.getMetaData().getDatabaseProductName();
                return productName != null && productName.toLowerCase().contains("postgresql");
            }));
        } catch (final RuntimeException exception) {
            resolved = false;
        }
        this.postgres = resolved;
    }

    public boolean isPostgres() {
        return postgres;
    }
}
