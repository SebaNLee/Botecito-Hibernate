package ar.edu.itba.paw.models.mail;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerificationMailModel {

    private MailRecipientModel recipient;
    private String verificationToken;
}
