package ar.edu.itba.paw.services;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    public MailServiceImpl(final JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendTestConfirmationEmail(final String recipientEmail) {
        final SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(recipientEmail);
        message.setSubject("Botecito test confirmation");
        message.setText("This is a test confirmation email sent from the Botecito development environment.");

        mailSender.send(message);
    }
}
