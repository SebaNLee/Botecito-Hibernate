package ar.edu.itba.paw.models.nuevo.mail;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingResolutionMailModel {

    private MailRecipientModel requester;
    private BookingMailModel booking;
    private String itemTitle;
}
