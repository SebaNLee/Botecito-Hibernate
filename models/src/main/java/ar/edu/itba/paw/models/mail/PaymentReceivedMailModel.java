package ar.edu.itba.paw.models.mail;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentReceivedMailModel {

    private MailRecipientModel requester;
    private String itemTitle;
}
