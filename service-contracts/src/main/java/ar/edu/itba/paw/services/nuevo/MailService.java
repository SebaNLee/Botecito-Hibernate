package ar.edu.itba.paw.services.nuevo;

import ar.edu.itba.paw.models.nuevo.mail.BookingResolutionMailModel;
import ar.edu.itba.paw.models.nuevo.mail.BookingReviewMailModel;
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

    // Deberia ser privada, las otras no deberian pedir un Locale ya resuelto
    Locale resolveLocale(MailRecipientModel recipient);

    /*
     * booking = entity de booking
     *
     * void sendPreBookingMail(booking)
     * void sendAcceptMail(booking)
     * void sendRejectMail(booking)
     * void sendPaymentMail(booking)
     * void sendRefusedPaymentMail(booking)
     * void sendBookingConfirmedMail(booking)
     * void sendBookingCancelledMail(booking)
     * void sendBookingExpiredMail(booking) // Cuando el cron job auto-cancela por
     * falta de anticipacion
     * void sendBookingFinishedMail(booking) // Dice algo de que podes dejar una
     * review
     *
     * subscribers = List<User>
     * User = entity de user
     *
     * void sendSubscriptionNotification(subscribers) // itera los subscribers y
     * manda el mismo mail a cada uno
     */
}
