package ar.edu.itba.paw.models.nuevo.mail;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentProofSubmittedMailModel {

    private MailRecipientModel owner;
    private String requesterName;
    private String itemTitle;
    private byte[] proofFileData;
    private String proofContentType;
}
