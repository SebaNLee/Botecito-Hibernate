package ar.edu.itba.paw.persistence;

import java.io.IOException;
import java.util.Arrays;
import javax.sql.DataSource;
import org.hsqldb.jdbc.JDBCDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.lang.NonNull;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@ComponentScan({"ar.edu.itba.paw.persistence"})
@Configuration
public class TestConfiguration {

    @Bean
    public @NonNull DataSource dataSource() {
        final SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(JDBCDriver.class);
        dataSource.setUrl("jdbc:hsqldb:mem:paw;sql.syntax_pgs=true");
        dataSource.setUsername("ha");
        dataSource.setPassword("");
        return dataSource;
    }

    @Bean
    public @NonNull PlatformTransactionManager transactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }

    @Bean
    public @NonNull DataSourceInitializer dataSourceInitializer(final @NonNull DataSource dataSource) {
        final DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(databasePopulator());
        return initializer;
    }

    private @NonNull DatabasePopulator databasePopulator() {
        final ResourceDatabasePopulator populator = new ResourceDatabasePopulator();

        final Resource[] migrationScripts;
        try {
            migrationScripts =
                    new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration-hsqldb/*.sql");
        } catch (final IOException e) {
            throw new IllegalStateException("Could not load test migration scripts", e);
        }

        Arrays.sort(migrationScripts, (left, right) -> String.valueOf(left.getFilename())
                .compareTo(String.valueOf(right.getFilename())));

        for (final Resource migrationScript : migrationScripts) {
            if (migrationScript == null) {
                throw new IllegalStateException("Null migration resource found");
            }
            populator.addScript(migrationScript);
        }

        return populator;
    }
}
