package ar.edu.itba.paw.persistence;

import ar.edu.itba.paw.persistence.orm.daos.MarketplaceHibernateDao;
import ar.edu.itba.paw.persistence.orm.daos.ReviewHibernateDao;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;
import javax.sql.DataSource;
import org.hsqldb.jdbc.JDBCDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
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
@ComponentScan(
        value = {"ar.edu.itba.paw.persistence"},
        excludeFilters =
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = {MarketplaceHibernateDao.class, ReviewHibernateDao.class}))
@Configuration
public class TestConfiguration {

    @Bean
    public @NonNull DataSource dataSource() {
        final SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(JDBCDriver.class);
        final String databaseName = "paw_" + UUID.randomUUID();
        dataSource.setUrl("jdbc:hsqldb:mem:" + databaseName + ";sql.syntax_pgs=true");
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

        Arrays.sort(
                migrationScripts,
                Comparator.comparingInt(TestConfiguration::migrationVersion)
                        .thenComparing(resource -> String.valueOf(resource.getFilename())));

        for (final Resource migrationScript : migrationScripts) {
            if (migrationScript == null) {
                throw new IllegalStateException("Null migration resource found");
            }
            populator.addScript(migrationScript);
        }

        return populator;
    }

    private static int migrationVersion(final Resource resource) {
        final String filename = String.valueOf(resource.getFilename());
        if (filename.length() < 2 || filename.charAt(0) != 'V') {
            return Integer.MAX_VALUE;
        }

        final int separatorIndex = filename.indexOf("__");
        if (separatorIndex <= 1) {
            return Integer.MAX_VALUE;
        }

        try {
            return Integer.parseInt(filename.substring(1, separatorIndex));
        } catch (final NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }
}
