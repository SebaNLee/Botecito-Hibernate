package ar.edu.itba.paw.webapp.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
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
    public org.springframework.context.MessageSource messageSource() {
        final org.springframework.context.support.ReloadableResourceBundleMessageSource messageSource =
                new org.springframework.context.support.ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(5);
        return messageSource;
    }

    @Bean
    public ViewResolver viewResolver() {
        final InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setViewClass(org.springframework.web.servlet.view.JstlView.class);
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".jsp");
        return viewResolver;
    }

    @Bean
    public DataSource dataSource() {
        final String profile = resolveCredentialsProfile();
        final Properties credentials = loadCredentialsProperties(profile);
        final SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.postgresql.Driver.class);
        dataSource.setUrl(credentials.getProperty("jdbc.url"));
        dataSource.setUsername(credentials.getProperty("jdbc.username"));
        dataSource.setPassword(credentials.getProperty("jdbc.password"));
        return dataSource;
    }

    private static String resolveCredentialsProfile() {
        final String env = System.getenv("PAW_CREDENTIALS_PROFILE");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        try (InputStream in = WebConfig.class.getResourceAsStream("/META-INF/paw-credentials-profile")) {
            if (in == null) {
                return "local";
            }
            final String text = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            return text.isEmpty() ? "local" : text;
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot read classpath:META-INF/paw-credentials-profile", e);
        }
    }

    private static Properties loadCredentialsProperties(final String profile) {
        final String path = "config/credentials-" + profile + ".properties";
        final ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Missing classpath resource '" + path + "' for credentials profile '" + profile + "'");
        }
        final Properties properties = new Properties();
        try (InputStream in = resource.getInputStream()) {
            properties.load(in);
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot load " + path, e);
        }
        requireJdbcKey(properties, path, "jdbc.url");
        requireJdbcKey(properties, path, "jdbc.username");
        requireJdbcKey(properties, path, "jdbc.password");
        return properties;
    }

    private static void requireJdbcKey(final Properties properties, final String path, final String key) {
        final String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing or blank '" + key + "' in " + path);
        }
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
    // (also migrated src/main/resources/schema.sql to
    // src/main/resources/db/migration/V1_init.sql)

    // @Bean
    // public DataSourceInitializer dataSourceInitializer(final DataSource
    // dataSource) {
    // final DataSourceInitializer initializer = new DataSourceInitializer();
    // initializer.setDataSource(dataSource);
    // initializer.setDatabasePopulator(databasePopulator());
    // return initializer;
    // }

    // private DatabasePopulator databasePopulator() {
    // final ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    // populator.addScript(new ClassPathResource("schema.sql"));
    // return populator;
    // }

    // ====================================
    // TODO reference, demo code from class
    // start
    // ====================================
}
