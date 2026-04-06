package ar.edu.itba.paw.webapp.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
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
        final CredentialsSelection selection = resolveCredentialsSelection();
        final Properties credentials = loadCredentialsProperties(selection.credentialsFile());
        if (!selection.fallbackCredentialsFile().isEmpty()) {
            mergeMissingProperties(credentials, selection.fallbackCredentialsFile());
        }
        final SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.postgresql.Driver.class);
        dataSource.setUrl(credentials.getProperty("jdbc.url"));
        dataSource.setUsername(credentials.getProperty("jdbc.username"));
        dataSource.setPassword(credentials.getProperty("jdbc.password"));
        return dataSource;
    }

    private static CredentialsSelection resolveCredentialsSelection() {
        final Properties selection = loadProperties("config/credentials-selection.properties");
        final String selected = selection.getProperty("credentials.file");
        if (selected == null || selected.isBlank()) {
            throw new IllegalStateException(
                    "Missing or blank 'credentials.file' in config/credentials-selection.properties");
        }
        final String fallback = selection.getProperty("credentials.fallback.file", "");
        return new CredentialsSelection(selected.trim(), fallback.trim());
    }

    private static Properties loadCredentialsProperties(final String credentialsFile) {
        final String path = "config/" + credentialsFile;
        final Properties properties = loadProperties(path);
        requireJdbcKey(properties, path, "jdbc.url");
        requireJdbcKey(properties, path, "jdbc.username");
        requireJdbcKey(properties, path, "jdbc.password");
        return properties;
    }

    private static Properties loadProperties(final String path) {
        final Properties properties = new Properties();
        try (InputStream in = WebConfig.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource '" + path + "'");
            }
            properties.load(in);
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot load " + path, e);
        }
        return properties;
    }

    private static void mergeMissingProperties(final Properties target, final String fallbackFile) {
        final String path = "config/" + fallbackFile;
        final InputStream in = WebConfig.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            return;
        }

        final Properties fallback = new Properties();
        try (InputStream closeable = in) {
            fallback.load(closeable);
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot load " + path, e);
        }

        for (final String key : fallback.stringPropertyNames()) {
            if (!target.containsKey(key)) {
                target.setProperty(key, fallback.getProperty(key));
            }
        }
    }

    private static void requireJdbcKey(final Properties properties, final String path, final String key) {
        final String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing or blank '" + key + "' in " + path);
        }
    }

    private record CredentialsSelection(String credentialsFile, String fallbackCredentialsFile) {}

    @Bean(initMethod = "migrate")
    public Flyway flyway(final DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }

    @Bean
    public org.springframework.web.multipart.support.StandardServletMultipartResolver multipartResolver() {
        return new org.springframework.web.multipart.support.StandardServletMultipartResolver();
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
