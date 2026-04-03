package ar.edu.itba.paw.webapp.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@EnableWebMvc
@ComponentScan({"ar.edu.itba.paw.webapp.controller", "ar.edu.itba.paw.services", "ar.edu.itba.paw.persistence"})
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public ViewResolver viewResolver() {
        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");
        return viewResolver;
    }

    @Bean
    public DataSource dataSource() {
        final SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.postgresql.Driver.class);
        dataSource.setUrl("jdbc:postgresql://localhost/paw"); // TODO local host, migrate to server
        dataSource.setUsername("postgres");
        dataSource.setPassword("postgres");
        return dataSource;
    }

    @Bean(initMethod = "migrate")
    public Flyway flyway(final DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load(); // TODO check this when migrating, configured to keep existing DB
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations("/css/");
        registry.addResourceHandler("/js/**").addResourceLocations("/js/");
    }

    // ====================================
    // TODO reference, demo code from class
    // start
    // ====================================

    // Note: the fragment below was replaced by Flyway
    // (also migrated src/main/resources/schema.sql to src/main/resources/db/migration/V1_init.sql)

    // @Bean
    // public DataSourceInitializer dataSourceInitializer(final DataSource dataSource) {
    //     final DataSourceInitializer initializer = new DataSourceInitializer();
    //     initializer.setDataSource(dataSource);
    //     initializer.setDatabasePopulator(databasePopulator());
    //     return initializer;
    // }

    // private DatabasePopulator databasePopulator() {
    //     final ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    //     populator.addScript(new ClassPathResource("schema.sql"));
    //     return populator;
    // }

    // ====================================
    // TODO reference, demo code from class
    // start
    // ====================================
}
