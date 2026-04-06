package ar.edu.itba.paw.webapp.config;

import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(final Properties credentialsProperties) {
        final JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(requireProperty(credentialsProperties, "mail.host"));
        mailSender.setPort(Integer.parseInt(requireProperty(credentialsProperties, "mail.port")));
        mailSender.setUsername(requireProperty(credentialsProperties, "mail.username"));
        mailSender.setPassword(requireProperty(credentialsProperties, "mail.password"));

        final Properties properties = mailSender.getJavaMailProperties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.debug", "false");

        return mailSender;
    }

    private static String requireProperty(final Properties properties, final String key) {
        final String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing or blank '" + key + "' in credentials properties");
        }
        return value;
    }
}
