package ar.edu.itba.paw.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.hsqldb.jdbc.JDBCDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@ComponentScan("ar.edu.itba.paw.persistence")
@Configuration
public class TestConfiguration {

    private static final String[] PG_ENUM_TYPES = {
        "item_status_enum", "booking_status_enum", "report_enum", "target_enum", "weekday_enum"
    };

    @Bean
    public DataSource dataSource() {
        final JDBCDataSource dataSource = new JDBCDataSource();
        dataSource.setURL("jdbc:hsqldb:mem:paw_test;sql.syntax_pgs=true");
        dataSource.setUser("ha");
        dataSource.setPassword("");

        // With JDBC we did a 1:1 representation of Flyway migration in HSQLDB relative syntax (ENUMs -> VARCHARs)
        // We now don't use Flyway to recreate the HSQLDB schema for testing, and instead use the @Entitys themselves
        // So now for Hibernate, this is the reasonable workaround (no really elegant tho)
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            for (final String type : PG_ENUM_TYPES) {
                stmt.execute("DROP DOMAIN IF EXISTS " + type);
                stmt.execute("CREATE DOMAIN " + type + " AS VARCHAR(20)");
            }
        } catch (final SQLException e) {
            throw new RuntimeException("HSQLDB persistence test setup failed", e);
        }

        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(final DataSource dataSource) {
        final LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setPackagesToScan("ar.edu.itba.paw.models.entity", "ar.edu.itba.paw.persistence");
        factoryBean.setDataSource(dataSource);
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        factoryBean.setJpaPropertyMap(java.util.Map.of(
                "hibernate.hbm2ddl.auto", "create-drop",
                "hibernate.dialect", "org.hibernate.dialect.HSQLDialect",
                "hibernate.show_sql", "true",
                "hibernate.format_sql", "true"));

        return factoryBean;
    }

    @Bean
    public PlatformTransactionManager transactionManager(final EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
