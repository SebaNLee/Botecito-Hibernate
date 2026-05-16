package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.mail.EmailVerificationMailModel;
import ar.edu.itba.paw.models.mail.MailRecipientModel;
import ar.edu.itba.paw.models.mail.PasswordRecoveryMailModel;
import ar.edu.itba.paw.models.mail.PublishConfirmationMailModel;
import java.util.Locale;

public interface MailService {

    void sendPublishConfirmationEmail(PublishConfirmationMailModel mail);

    void sendPasswordRecoveryEmail(PasswordRecoveryMailModel mail);

    void sendEmailVerificationEmail(EmailVerificationMailModel mail);

    Locale resolveLocale(MailRecipientModel recipient);
}
