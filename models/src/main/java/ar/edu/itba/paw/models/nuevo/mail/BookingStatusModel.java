package ar.edu.itba.paw.models.nuevo.mail;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BookingStatusModel {
    CONFIRMED("request.status.accepted"),
    REJECTED("request.status.declined"),
    PAYMENT_SUBMITTED("request.status.paymentSubmitted"),
    PAID("request.status.paid"),
    PAYMENT_REFUSED("request.status.paymentRefused"),
    UPDATED("request.status.updated");

    private final String messageCode;
}
