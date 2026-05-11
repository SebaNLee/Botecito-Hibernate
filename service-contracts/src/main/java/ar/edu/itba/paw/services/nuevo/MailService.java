package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.mail.BookingResolutionMailModel;
import ar.edu.itba.paw.models.nuevo.mail.BookingReviewMailModel;
import ar.edu.itba.paw.models.nuevo.mail.EmailVerificationMailModel;
import ar.edu.itba.paw.models.nuevo.mail.MailRecipientModel;
import ar.edu.itba.paw.models.nuevo.mail.PasswordRecoveryMailModel;
import ar.edu.itba.paw.models.nuevo.mail.PaymentProofRefusedMailModel;
import ar.edu.itba.paw.models.nuevo.mail.PaymentProofSubmittedMailModel;
import ar.edu.itba.paw.models.nuevo.mail.PaymentReceivedMailModel;
import ar.edu.itba.paw.models.nuevo.mail.PublishConfirmationMailModel;
import java.util.Locale;

public interface MailService {

    void sendTestConfirmationEmail(MailRecipientModel recipient);

    void sendPublishConfirmationEmail(PublishConfirmationMailModel mail);

    void sendBookingReviewEmail(BookingReviewMailModel mail);

    void sendBookingResolutionEmail(BookingResolutionMailModel mail);

    void sendPaymentProofSubmittedEmail(PaymentProofSubmittedMailModel mail);

    void sendPaymentReceivedEmail(PaymentReceivedMailModel mail);

    void sendPaymentProofRefusedEmail(PaymentProofRefusedMailModel mail);

    void sendPasswordRecoveryEmail(PasswordRecoveryMailModel mail);

    void sendEmailVerificationEmail(EmailVerificationMailModel mail);

    Locale resolveLocale(MailRecipientModel recipient);
}
