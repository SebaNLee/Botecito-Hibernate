package ar.edu.itba.paw.models.nuevo;

import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"passwordHash", "passwordRecoveryToken"})
public class UserModel {

    private Integer id;
    private OffsetDateTime createdAt;
    private String givenName;
    private String lastName;
    private String email;
    private String phone;
    private String paymentAlias;
    private PreferredLanguageModel preferredLanguage = PreferredLanguageModel.ES;
    private String passwordHash;
    private String passwordRecoveryToken;
    private OffsetDateTime passwordRecoveryUsedAt;
    private boolean verified;

    public String getName() {
        final boolean noGiven = givenName == null || givenName.isEmpty();
        final boolean noLast = lastName == null || lastName.isEmpty();
        if (noGiven && noLast) {
            return "";
        }
        if (noGiven) {
            return lastName;
        }
        if (noLast) {
            return givenName;
        }
        return givenName + " " + lastName;
    }
}
