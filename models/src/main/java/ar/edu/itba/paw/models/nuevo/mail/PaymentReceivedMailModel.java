package ar.edu.itba.paw.models.nuevo.mail;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentReceivedMailModel {

    private MailRecipientModel requester;
    private String itemTitle;
}
