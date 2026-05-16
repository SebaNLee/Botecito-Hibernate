package ar.edu.itba.paw.models.nuevo.mail;

import ar.edu.itba.paw.models.entity.UsersOrm;
import ar.edu.itba.paw.models.nuevo.PreferredLanguageModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MailRecipientModel {

    private String email;
    private String displayName;
    private PreferredLanguageModel preferredLanguage = PreferredLanguageModel.ES;

    public static MailRecipientModel fromUser(final UsersOrm user) {
        final MailRecipientModel recipient = new MailRecipientModel();
        if (user == null) {
            return recipient;
        }
        recipient.setEmail(user.getEmail());
        final String fullName = (user.getFirstName() != null ? user.getFirstName() : "")
                + " " + (user.getLastName() != null ? user.getLastName() : "");
        final String trimmed = fullName.trim();
        recipient.setDisplayName(trimmed.isBlank() ? user.getEmail() : trimmed);
        recipient.setPreferredLanguage(
                PreferredLanguageModel.fromPersistence(user.getLanguage()));
        return recipient;
    }
}
