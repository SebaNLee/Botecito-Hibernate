package ar.edu.itba.paw.models.nuevo.mail;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingReviewMailModel {

    private MailRecipientModel owner;
    private MailRecipientModel requester;
    private BookingMailModel booking;
    private String itemTitle;
    private String location;
    private String requestedDateLabel;
    private String requestedTimeLabel;
}
