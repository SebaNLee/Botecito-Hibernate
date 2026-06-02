package ar.edu.itba.paw.services;

import ar.edu.itba.paw.models.dto.ReportEmail;
import ar.edu.itba.paw.models.entity.Booking;
import ar.edu.itba.paw.models.entity.Users;
import ar.edu.itba.paw.models.entity.Version;

public interface MailService {

    void sendPublishConfirmationEmail(Version version);

    void sendFollowerPublishNotificationEmail(Users subscriber, Version version);

    void sendPasswordRecoveryEmail(Users user);

    void sendEmailVerificationEmail(Users user);

    void sendPreBookingMail(Booking booking);

    void sendAcceptMail(Booking booking);

    void sendRejectMail(Booking booking);

    void sendPaymentMail(Booking booking);

    void sendRefusedPaymentMail(Booking booking);

    void sendBookingConfirmedMail(Booking booking);

    void sendBookingCancelledMail(Booking booking);

    void sendBookingExpiredMail(Booking booking);

    void sendBookingFinishedMail(Booking booking);

    void sendReportDismissedEmail(ReportEmail reportEmail);

    void sendReportSuccessEmail(ReportEmail reportEmail);

    void sendPublicationRemovedEmail(ReportEmail reportEmail);
}
