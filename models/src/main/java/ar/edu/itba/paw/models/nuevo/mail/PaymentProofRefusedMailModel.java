package ar.edu.itba.paw.models.nuevo.mail;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentProofRefusedMailModel {

    private MailRecipientModel requester;
    private String ownerName;
    private String itemTitle;
    private String reason;
}
