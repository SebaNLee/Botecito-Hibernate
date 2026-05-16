package ar.edu.itba.paw.webapp.form;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginForm {

    private String error;
    private String logout;
    private String registered;
    private String verificationSent;
    private String verified;
    private String verificationInvalid;
    private String unverified;
    private String legacyToken;
    private String passwordRecovered;
    private String next;
}
