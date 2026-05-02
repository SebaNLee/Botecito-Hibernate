package ar.edu.itba.paw.webapp.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

@EnableWebMvc
@EnableAsync
@EnableScheduling
@Import(MailConfig.class)
@ComponentScan({
    "ar.edu.itba.paw.webapp.controller",
    "ar.edu.itba.paw.webapp.auth",
    "ar.edu.itba.paw.services",
    "ar.edu.itba.paw.persistence"
})
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
    public org.springframework.validation.beanvalidation.LocalValidatorFactoryBean localValidatorFactoryBean() {
        final org.springframework.validation.beanvalidation.LocalValidatorFactoryBean factory =
                new org.springframework.validation.beanvalidation.LocalValidatorFactoryBean();
        factory.setValidationMessageSource(messageSource());
        return factory;
    }

    @Override
    public org.springframework.validation.Validator getValidator() {
        return localValidatorFactoryBean();
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
    public Properties credentialsProperties() {
        final CredentialsSelection selection = resolveCredentialsSelection();
        final Properties credentials = loadCredentialsProperties(selection.credentialsFile());
        if (!selection.fallbackCredentialsFile().isEmpty()) {
            mergeMissingProperties(credentials, selection.fallbackCredentialsFile());
        }
        return credentials;
    }

    @Bean
    public DataSource dataSource(final Properties credentialsProperties) {
        final SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.postgresql.Driver.class);
        dataSource.setUrl(credentialsProperties.getProperty("jdbc.url"));
        dataSource.setUsername(credentialsProperties.getProperty("jdbc.username"));
        dataSource.setPassword(credentialsProperties.getProperty("jdbc.password"));
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
    public Flyway flyway(final DataSource dataSource, final Properties credentialsProperties) {
        final boolean outOfOrder =
                Boolean.parseBoolean(credentialsProperties.getProperty("flyway.outOfOrder", "false"));

        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .outOfOrder(outOfOrder)
                .load();
    }

    @Bean
    public org.springframework.web.multipart.support.StandardServletMultipartResolver multipartResolver() {
        return new org.springframework.web.multipart.support.StandardServletMultipartResolver();
    }

    @Bean(name = "mailTaskExecutor")
    public ThreadPoolTaskExecutor mailTaskExecutor() {
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("mail-");
        executor.initialize();
        return executor;
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**").addResourceLocations("/css/");
        registry.addResourceHandler("/js/**").addResourceLocations("/js/");
        registry.addResourceHandler("/img/**").addResourceLocations("/img/");
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
