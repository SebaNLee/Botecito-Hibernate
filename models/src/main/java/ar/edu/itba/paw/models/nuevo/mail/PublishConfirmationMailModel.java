package ar.edu.itba.paw.models.nuevo.mail;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublishConfirmationMailModel {

    private MailRecipientModel owner;
    private String itemTitle;
}
