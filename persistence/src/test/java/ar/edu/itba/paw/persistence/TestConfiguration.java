package ar.edu.itba.paw.persistence;

import java.util.List;
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

    @Bean
    public DataSource dataSource() {
        final JDBCDataSource dataSource = new JDBCDataSource();
        dataSource.setURL("jdbc:hsqldb:mem:paw_test;sql.syntax_pgs=true");
        dataSource.setUser("ha");
        dataSource.setPassword("");
        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(final DataSource dataSource) {
        final LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setPackagesToScan("ar.edu.itba.paw.models.entity");
        factoryBean.setDataSource(dataSource);
        factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        factoryBean.setJpaPropertyMap(java.util.Map.of(
                "hibernate.hbm2ddl.auto", "create-drop", // Using @Entity (no Flyway or HSQLDB needed in tests)
                "hibernate.dialect", "org.hibernate.dialect.HSQLDialect",
                "hibernate.show_sql", "true",
                "hibernate.format_sql", "true"));

        factoryBean.setPersistenceUnitPostProcessors(pui -> {
            pui.getManagedClassNames().clear();
            pui.getManagedClassNames().addAll(entityClassNames());
        });

        return factoryBean;
    }

    @Bean
    public List<String> entityClassNames() {
        return List.of();
    }

    @Bean
    public PlatformTransactionManager transactionManager(final EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
