package ar.edu.itba.paw.models.nuevo.mail;

import ar.edu.itba.paw.models.nuevo.PreferredLanguageModel;
import ar.edu.itba.paw.models.nuevo.UserModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MailRecipientModel {

    private String email;
    private String displayName;
    private PreferredLanguageModel preferredLanguage = PreferredLanguageModel.ES;

    public static MailRecipientModel fromUser(final UserModel user) {
        final MailRecipientModel recipient = new MailRecipientModel();
        if (user == null) {
            return recipient;
        }
        recipient.setEmail(user.getEmail());
        recipient.setDisplayName(user.getName().isBlank() ? user.getEmail() : user.getName());
        recipient.setPreferredLanguage(user.getPreferredLanguage());
        return recipient;
    }
}
